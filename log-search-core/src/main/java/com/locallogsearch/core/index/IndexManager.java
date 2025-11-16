/*
 * MIT License
 *
 * Copyright (c) 2025 Chris Collins
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.locallogsearch.core.index;

import com.locallogsearch.core.config.IndexConfig;
import com.locallogsearch.core.model.LogEntry;
import com.locallogsearch.core.truncation.TruncationConfig;
import com.locallogsearch.core.truncation.TruncationPolicy;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
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
    
    /**
     * Truncate index based on a time-based retention policy.
     * Deletes all documents older than the cutoff timestamp.
     * 
     * @param indexName the name of the index to truncate
     * @param cutoffTimestamp documents older than this timestamp will be deleted
     * @return the number of documents deleted
     * @throws IOException if an I/O error occurs
     */
    public int truncateByTime(String indexName, Instant cutoffTimestamp) throws IOException {
        log.info("Truncating index {} - deleting documents older than {}", indexName, cutoffTimestamp);
        
        Path indexPath = Paths.get(config.getBaseDirectory(), indexName);
        if (!Files.exists(indexPath)) {
            log.warn("Index does not exist: {}", indexName);
            return 0;
        }
        
        int deletedCount = 0;
        Directory directory = FSDirectory.open(indexPath);
        
        try (IndexReader reader = DirectoryReader.open(directory)) {
            // Count documents that will be deleted
            IndexSearcher searcher = new IndexSearcher(reader);
            Query query = LongPoint.newRangeQuery("timestamp", 0, cutoffTimestamp.toEpochMilli());
            TotalHitCountCollector collector = new TotalHitCountCollector();
            searcher.search(query, collector);
            deletedCount = collector.getTotalHits();
        }
        
        // Close existing writer if open
        IndexWriter existingWriter = indexWriters.remove(indexName);
        if (existingWriter != null) {
            existingWriter.close();
        }
        
        // Delete documents
        IndexWriterConfig writerConfig = new IndexWriterConfig(analyzer);
        try (IndexWriter writer = new IndexWriter(directory, writerConfig)) {
            Query deleteQuery = LongPoint.newRangeQuery("timestamp", 0, cutoffTimestamp.toEpochMilli());
            writer.deleteDocuments(deleteQuery);
            writer.commit();
        }
        
        log.info("Truncated index {}: deleted {} documents", indexName, deletedCount);
        return deletedCount;
    }
    
    /**
     * Truncate index based on a volume-based retention policy.
     * Keeps only the most recent N documents, deleting the oldest.
     * 
     * @param indexName the name of the index to truncate
     * @param maxDocuments the maximum number of documents to keep
     * @return the number of documents deleted
     * @throws IOException if an I/O error occurs
     */
    public int truncateByVolume(String indexName, long maxDocuments) throws IOException {
        log.info("Truncating index {} - keeping only {} most recent documents", indexName, maxDocuments);
        
        Path indexPath = Paths.get(config.getBaseDirectory(), indexName);
        if (!Files.exists(indexPath)) {
            log.warn("Index does not exist: {}", indexName);
            return 0;
        }
        
        Directory directory = FSDirectory.open(indexPath);
        
        try (IndexReader reader = DirectoryReader.open(directory)) {
            int totalDocs = reader.numDocs();
            
            if (totalDocs <= maxDocuments) {
                log.info("Index {} has {} documents, no truncation needed (max: {})", 
                    indexName, totalDocs, maxDocuments);
                return 0;
            }
            
            int docsToDelete = (int) (totalDocs - maxDocuments);
            
            // Find the timestamp of the Nth newest document (where N = maxDocuments)
            IndexSearcher searcher = new IndexSearcher(reader);
            Query allDocsQuery = new MatchAllDocsQuery();
            
            // Sort by timestamp descending to get newest first
            Sort sort = new Sort(new SortField("timestamp", SortField.Type.LONG, true));
            TopDocs topDocs = searcher.search(allDocsQuery, (int) maxDocuments, sort);
            
            if (topDocs.scoreDocs.length < maxDocuments) {
                log.warn("Could not find enough documents with timestamps in index {}", indexName);
                return 0;
            }
            
            // Get the timestamp of the oldest document we want to keep
            Document oldestKeptDoc = searcher.doc(topDocs.scoreDocs[(int) maxDocuments - 1].doc);
            Long cutoffTimestamp = oldestKeptDoc.getField("timestamp") != null 
                ? oldestKeptDoc.getField("timestamp").numericValue().longValue()
                : null;
            
            if (cutoffTimestamp == null) {
                log.warn("Could not determine cutoff timestamp for index {}", indexName);
                return 0;
            }
            
            // Close existing writer if open
            IndexWriter existingWriter = indexWriters.remove(indexName);
            if (existingWriter != null) {
                existingWriter.close();
            }
            
            // Delete all documents older than the cutoff
            IndexWriterConfig writerConfig = new IndexWriterConfig(analyzer);
            try (IndexWriter writer = new IndexWriter(directory, writerConfig)) {
                Query deleteQuery = LongPoint.newRangeQuery("timestamp", 0, cutoffTimestamp - 1);
                writer.deleteDocuments(deleteQuery);
                writer.commit();
            }
            
            log.info("Truncated index {}: deleted approximately {} documents", indexName, docsToDelete);
            return docsToDelete;
        }
    }
    
    /**
     * Truncate an index based on a truncation configuration.
     * 
     * @param truncationConfig the configuration specifying how to truncate
     * @return the number of documents deleted
     * @throws IOException if an I/O error occurs
     */
    public int truncateIndex(TruncationConfig truncationConfig) throws IOException {
        if (truncationConfig.getPolicy() == TruncationPolicy.NONE) {
            log.debug("No truncation policy set for index {}", truncationConfig.getIndexName());
            return 0;
        }
        
        switch (truncationConfig.getPolicy()) {
            case TIME_BASED:
                if (truncationConfig.getRetentionPeriod() == null) {
                    log.warn("TIME_BASED policy requires retentionPeriod");
                    return 0;
                }
                Instant cutoff = Instant.now().minus(truncationConfig.getRetentionPeriod());
                return truncateByTime(truncationConfig.getIndexName(), cutoff);
                
            case VOLUME_BASED:
                if (truncationConfig.getMaxDocuments() == null) {
                    log.warn("VOLUME_BASED policy requires maxDocuments");
                    return 0;
                }
                return truncateByVolume(truncationConfig.getIndexName(), truncationConfig.getMaxDocuments());
                
            default:
                return 0;
        }
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
