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

package com.locallogsearch.service.controller;

import com.locallogsearch.core.config.IndexConfig;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/api/lucene-diagnostics")
public class LuceneDiagnosticsController {
    private static final Logger log = LoggerFactory.getLogger(LuceneDiagnosticsController.class);
    
    private final IndexConfig indexConfig;
    
    public LuceneDiagnosticsController(IndexConfig indexConfig) {
        this.indexConfig = indexConfig;
    }
    
    /**
     * Get detailed information about an index including fields and structure
     */
    @GetMapping("/{indexName}")
    public ResponseEntity<Map<String, Object>> getIndexInfo(@PathVariable String indexName) {
        try {
            Path indexPath = Paths.get(indexConfig.getBaseDirectory(), indexName);
            
            if (!Files.exists(indexPath)) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> info = new HashMap<>();
            info.put("indexName", indexName);
            info.put("indexPath", indexPath.toString());
            
            try (Directory directory = FSDirectory.open(indexPath);
                 IndexReader reader = DirectoryReader.open(directory)) {
                
                // Basic stats
                info.put("numDocs", reader.numDocs());
                info.put("maxDoc", reader.maxDoc());
                info.put("numDeletedDocs", reader.numDeletedDocs());
                info.put("hasDeletions", reader.hasDeletions());
                
                // Get all field information
                List<Map<String, Object>> fields = new ArrayList<>();
                Set<String> fieldNames = new HashSet<>();
                
                for (LeafReaderContext leafContext : reader.leaves()) {
                    LeafReader leafReader = leafContext.reader();
                    FieldInfos fieldInfos = leafReader.getFieldInfos();
                    
                    for (FieldInfo fieldInfo : fieldInfos) {
                        if (!fieldNames.contains(fieldInfo.name)) {
                            fieldNames.add(fieldInfo.name);
                            
                            Map<String, Object> fieldData = new HashMap<>();
                            fieldData.put("name", fieldInfo.name);
                            fieldData.put("number", fieldInfo.number);
                            fieldData.put("hasVectors", fieldInfo.hasVectors());
                            fieldData.put("hasNorms", fieldInfo.hasNorms());
                            fieldData.put("hasPayloads", fieldInfo.hasPayloads());
                            fieldData.put("indexOptions", fieldInfo.getIndexOptions().toString());
                            fieldData.put("docValuesType", fieldInfo.getDocValuesType().toString());
                            
                            // Get term count for this field if it's indexed
                            if (fieldInfo.getIndexOptions() != IndexOptions.NONE) {
                                Terms terms = MultiTerms.getTerms(reader, fieldInfo.name);
                                if (terms != null) {
                                    fieldData.put("uniqueTerms", terms.size());
                                    fieldData.put("sumDocFreq", terms.getSumDocFreq());
                                    fieldData.put("sumTotalTermFreq", terms.getSumTotalTermFreq());
                                }
                            }
                            
                            fields.add(fieldData);
                        }
                    }
                }
                
                info.put("fields", fields);
                
                // Segment information
                List<Map<String, Object>> segments = new ArrayList<>();
                for (LeafReaderContext leafContext : reader.leaves()) {
                    LeafReader leafReader = leafContext.reader();
                    
                    Map<String, Object> segmentInfo = new HashMap<>();
                    segmentInfo.put("docBase", leafContext.docBase);
                    segmentInfo.put("numDocs", leafReader.numDocs());
                    segmentInfo.put("maxDoc", leafReader.maxDoc());
                    segmentInfo.put("numDeletedDocs", leafReader.numDeletedDocs());
                    
                    if (leafReader instanceof SegmentReader) {
                        SegmentReader segReader = (SegmentReader) leafReader;
                        SegmentCommitInfo segCommitInfo = segReader.getSegmentInfo();
                        segmentInfo.put("segmentName", segCommitInfo.info.name);
                        segmentInfo.put("sizeInBytes", segCommitInfo.sizeInBytes());
                    }
                    
                    segments.add(segmentInfo);
                }
                
                info.put("segments", segments);
                info.put("numSegments", segments.size());
                
            } catch (IOException e) {
                log.error("Error reading index: {}", indexName, e);
                return ResponseEntity.internalServerError().body(
                    Map.of("error", "Failed to read index: " + e.getMessage())
                );
            }
            
            return ResponseEntity.ok(info);
            
        } catch (Exception e) {
            log.error("Error getting index info: {}", indexName, e);
            return ResponseEntity.internalServerError().body(
                Map.of("error", "Failed to get index info: " + e.getMessage())
            );
        }
    }
    
    /**
     * Execute a Lucene query and return results
     */
    @PostMapping("/{indexName}/query")
    public ResponseEntity<Map<String, Object>> executeQuery(
            @PathVariable String indexName,
            @RequestBody Map<String, Object> request) {
        
        try {
            String queryString = (String) request.get("query");
            String field = (String) request.getOrDefault("field", "message");
            int maxResults = ((Number) request.getOrDefault("maxResults", 100)).intValue();
            
            if (queryString == null || queryString.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "Query string is required")
                );
            }
            
            Path indexPath = Paths.get(indexConfig.getBaseDirectory(), indexName);
            
            if (!Files.exists(indexPath)) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> response = new HashMap<>();
            
            try (Directory directory = FSDirectory.open(indexPath);
                 IndexReader reader = DirectoryReader.open(directory)) {
                
                IndexSearcher searcher = new IndexSearcher(reader);
                QueryParser parser = new QueryParser(field, new StandardAnalyzer());
                Query query = parser.parse(queryString);
                
                TopDocs topDocs = searcher.search(query, maxResults);
                
                response.put("query", query.toString());
                response.put("totalHits", topDocs.totalHits.value);
                response.put("relation", topDocs.totalHits.relation.toString());
                
                List<Map<String, Object>> results = new ArrayList<>();
                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    Document doc = searcher.doc(scoreDoc.doc);
                    
                    Map<String, Object> docData = new HashMap<>();
                    docData.put("docId", scoreDoc.doc);
                    docData.put("score", scoreDoc.score);
                    
                    Map<String, List<String>> fields = new HashMap<>();
                    for (IndexableField indexableField : doc.getFields()) {
                        String fieldName = indexableField.name();
                        String fieldValue = indexableField.stringValue();
                        
                        fields.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(fieldValue);
                    }
                    docData.put("fields", fields);
                    
                    results.add(docData);
                }
                
                response.put("results", results);
                
            } catch (ParseException e) {
                log.error("Query parse error: {}", queryString, e);
                return ResponseEntity.badRequest().body(
                    Map.of("error", "Invalid query: " + e.getMessage())
                );
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error executing query on index: {}", indexName, e);
            return ResponseEntity.internalServerError().body(
                Map.of("error", "Query execution failed: " + e.getMessage())
            );
        }
    }
    
    /**
     * Force merge segments in an index
     */
    @PostMapping("/{indexName}/merge")
    public ResponseEntity<Map<String, Object>> forceIndexMerge(
            @PathVariable String indexName,
            @RequestBody(required = false) Map<String, Object> request) {
        
        try {
            int maxSegments = 1;
            if (request != null && request.containsKey("maxSegments")) {
                maxSegments = ((Number) request.get("maxSegments")).intValue();
            }
            
            Path indexPath = Paths.get(indexConfig.getBaseDirectory(), indexName);
            
            if (!Files.exists(indexPath)) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> response = new HashMap<>();
            
            // Get segment count before merge
            int segmentsBefore = 0;
            try (Directory directory = FSDirectory.open(indexPath);
                 IndexReader reader = DirectoryReader.open(directory)) {
                segmentsBefore = reader.leaves().size();
            }
            
            // Perform merge
            try (Directory directory = FSDirectory.open(indexPath)) {
                IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
                config.setOpenMode(IndexWriterConfig.OpenMode.APPEND);
                
                try (IndexWriter writer = new IndexWriter(directory, config)) {
                    log.info("Starting force merge on index: {} to {} segments", indexName, maxSegments);
                    writer.forceMerge(maxSegments);
                    writer.commit();
                    log.info("Force merge completed on index: {}", indexName);
                }
            }
            
            // Get segment count after merge
            int segmentsAfter = 0;
            try (Directory directory = FSDirectory.open(indexPath);
                 IndexReader reader = DirectoryReader.open(directory)) {
                segmentsAfter = reader.leaves().size();
            }
            
            response.put("message", "Index merge completed");
            response.put("indexName", indexName);
            response.put("segmentsBefore", segmentsBefore);
            response.put("segmentsAfter", segmentsAfter);
            response.put("maxSegments", maxSegments);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error merging index: {}", indexName, e);
            return ResponseEntity.internalServerError().body(
                Map.of("error", "Index merge failed: " + e.getMessage())
            );
        }
    }
    
    /**
     * Get term statistics for a specific field
     */
    @GetMapping("/{indexName}/field/{fieldName}/terms")
    public ResponseEntity<Map<String, Object>> getFieldTerms(
            @PathVariable String indexName,
            @PathVariable String fieldName,
            @RequestParam(defaultValue = "100") int limit) {
        
        try {
            Path indexPath = Paths.get(indexConfig.getBaseDirectory(), indexName);
            
            if (!Files.exists(indexPath)) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> response = new HashMap<>();
            
            try (Directory directory = FSDirectory.open(indexPath);
                 IndexReader reader = DirectoryReader.open(directory)) {
                
                Terms terms = MultiTerms.getTerms(reader, fieldName);
                
                if (terms == null) {
                    return ResponseEntity.ok(Map.of(
                        "fieldName", fieldName,
                        "message", "Field not found or has no terms"
                    ));
                }
                
                response.put("fieldName", fieldName);
                response.put("uniqueTerms", terms.size());
                response.put("sumDocFreq", terms.getSumDocFreq());
                response.put("sumTotalTermFreq", terms.getSumTotalTermFreq());
                
                // Get top terms by frequency
                List<Map<String, Object>> topTerms = new ArrayList<>();
                TermsEnum termsEnum = terms.iterator();
                
                int count = 0;
                while (termsEnum.next() != null && count < limit) {
                    Map<String, Object> termData = new HashMap<>();
                    termData.put("term", termsEnum.term().utf8ToString());
                    termData.put("docFreq", termsEnum.docFreq());
                    termData.put("totalTermFreq", termsEnum.totalTermFreq());
                    
                    topTerms.add(termData);
                    count++;
                }
                
                response.put("topTerms", topTerms);
                response.put("limit", limit);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting field terms for index: {} field: {}", indexName, fieldName, e);
            return ResponseEntity.internalServerError().body(
                Map.of("error", "Failed to get field terms: " + e.getMessage())
            );
        }
    }
}
