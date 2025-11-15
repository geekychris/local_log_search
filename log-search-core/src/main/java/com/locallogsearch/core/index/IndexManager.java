package com.locallogsearch.core.index;

import com.locallogsearch.core.config.IndexConfig;
import com.locallogsearch.core.model.LogEntry;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class IndexManager implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(IndexManager.class);
    
    private final IndexConfig config;
    private final Map<String, IndexWriter> indexWriters;
    private final ScheduledExecutorService commitScheduler;
    private final StandardAnalyzer analyzer;
    private final FacetsConfig facetsConfig;
    
    public IndexManager(IndexConfig config) {
        this.config = config;
        this.indexWriters = new ConcurrentHashMap<>();
        this.analyzer = new StandardAnalyzer();
        this.facetsConfig = new FacetsConfig();
        // Use default field name for facets (don't override)
        this.commitScheduler = Executors.newSingleThreadScheduledExecutor();
        
        // Schedule periodic commits
        commitScheduler.scheduleAtFixedRate(
            this::commitAll,
            config.getCommitIntervalSeconds(),
            config.getCommitIntervalSeconds(),
            TimeUnit.SECONDS
        );
    }
    
    public void indexLogEntry(LogEntry entry) throws IOException {
        String indexName = entry.getIndexName();
        IndexWriter writer = getOrCreateWriter(indexName);
        
        Document doc = new Document();
        
        // Store raw text
        doc.add(new TextField("raw_text", entry.getRawText(), Field.Store.YES));
        
        // Store timestamp
        if (entry.getTimestamp() != null) {
            doc.add(new LongPoint("timestamp", entry.getTimestamp().toEpochMilli()));
            doc.add(new StoredField("timestamp", entry.getTimestamp().toEpochMilli()));
            doc.add(new NumericDocValuesField("timestamp", entry.getTimestamp().toEpochMilli()));
        }
        
        // Store source
        doc.add(new StringField("source", entry.getSource(), Field.Store.YES));
        
        // Store all extracted fields
        for (Map.Entry<String, String> field : entry.getFields().entrySet()) {
            String fieldName = field.getKey();
            String fieldValue = field.getValue();
            
            // Store as searchable text and exact match
            doc.add(new TextField(fieldName, fieldValue, Field.Store.YES));
            doc.add(new StringField(fieldName + "_exact", fieldValue, Field.Store.NO));
            
            // Add facet field for efficient aggregation
            // Skip very long values to avoid bloating the index
            if (fieldValue.length() <= 100) {
                doc.add(new SortedSetDocValuesFacetField(fieldName, fieldValue));
                // Configure facet dimension to be multi-valued
                facetsConfig.setMultiValued(fieldName, true);
            }
            
            // Try to parse as number and store numeric field for range queries
            try {
                double numericValue = Double.parseDouble(fieldValue.trim());
                // Store as DoublePoint for efficient range queries
                doc.add(new DoublePoint(fieldName + "_num", numericValue));
                doc.add(new StoredField(fieldName + "_num", numericValue));
                doc.add(new DoubleDocValuesField(fieldName + "_num", numericValue));
            } catch (NumberFormatException e) {
                // Not a number, skip numeric indexing
            }
        }
        
        // Process document with FacetsConfig to properly index facet fields
        Document processedDoc = facetsConfig.build(doc);
        writer.addDocument(processedDoc);
    }
    
    private IndexWriter getOrCreateWriter(String indexName) throws IOException {
        return indexWriters.computeIfAbsent(indexName, name -> {
            try {
                Path indexPath = Paths.get(config.getBaseDirectory(), name);
                Files.createDirectories(indexPath);
                
                Directory directory = FSDirectory.open(indexPath);
                IndexWriterConfig writerConfig = new IndexWriterConfig(analyzer);
                writerConfig.setCommitOnClose(true);
                
                log.info("Created index writer for: {}", name);
                return new IndexWriter(directory, writerConfig);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create index writer for: " + name, e);
            }
        });
    }
    
    public void commit(String indexName) throws IOException {
        IndexWriter writer = indexWriters.get(indexName);
        if (writer != null) {
            writer.commit();
            log.debug("Committed index: {}", indexName);
        }
    }
    
    public void commitAll() {
        for (Map.Entry<String, IndexWriter> entry : indexWriters.entrySet()) {
            try {
                entry.getValue().commit();
                log.debug("Committed index: {}", entry.getKey());
            } catch (IOException e) {
                log.error("Failed to commit index: {}", entry.getKey(), e);
            }
        }
    }
    
    /**
     * Clear all documents from the specified index.
     * This closes the existing writer and creates a new one with an empty index.
     * 
     * @param indexName the name of the index to clear
     * @throws IOException if an I/O error occurs
     */
    public void clearIndex(String indexName) throws IOException {
        log.info("Clearing index: {}", indexName);
        
        // Close existing writer if it exists
        IndexWriter existingWriter = indexWriters.remove(indexName);
        if (existingWriter != null) {
            existingWriter.close();
        }
        
        // Delete all documents by creating a new writer and calling deleteAll
        Path indexPath = Paths.get(config.getBaseDirectory(), indexName);
        if (Files.exists(indexPath)) {
            Directory directory = FSDirectory.open(indexPath);
            IndexWriterConfig writerConfig = new IndexWriterConfig(analyzer);
            try (IndexWriter writer = new IndexWriter(directory, writerConfig)) {
                writer.deleteAll();
                writer.commit();
            }
        }
        
        log.info("Index cleared: {}", indexName);
    }
    
    @Override
    public void close() {
        log.info("Closing IndexManager");
        commitScheduler.shutdown();
        
        try {
            if (!commitScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                commitScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            commitScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        for (Map.Entry<String, IndexWriter> entry : indexWriters.entrySet()) {
            try {
                entry.getValue().close();
                log.info("Closed index: {}", entry.getKey());
            } catch (IOException e) {
                log.error("Failed to close index: {}", entry.getKey(), e);
            }
        }
    }
    
    public Path getIndexPath(String indexName) {
        return Paths.get(config.getBaseDirectory(), indexName);
    }
}
