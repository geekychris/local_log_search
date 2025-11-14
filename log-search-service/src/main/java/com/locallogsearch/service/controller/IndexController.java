package com.locallogsearch.service.controller;

import com.locallogsearch.core.config.IndexConfig;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/indices")
public class IndexController {
    private static final Logger log = LoggerFactory.getLogger(IndexController.class);
    
    private final IndexConfig indexConfig;
    
    public IndexController(IndexConfig indexConfig) {
        this.indexConfig = indexConfig;
    }
    
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listIndices() {
        try {
            Path baseDir = Paths.get(indexConfig.getBaseDirectory());
            
            if (!Files.exists(baseDir)) {
                return ResponseEntity.ok(new ArrayList<>());
            }
            
            List<Map<String, Object>> indices = new ArrayList<>();
            
            try (Stream<Path> paths = Files.list(baseDir)) {
                paths.filter(Files::isDirectory).forEach(indexPath -> {
                    try {
                        Map<String, Object> indexInfo = new HashMap<>();
                        indexInfo.put("name", indexPath.getFileName().toString());
                        indexInfo.put("path", indexPath.toString());
                        
                        // Get index stats if it exists
                        if (Files.exists(indexPath) && isValidIndex(indexPath)) {
                            Directory directory = FSDirectory.open(indexPath);
                            try (IndexReader reader = DirectoryReader.open(directory)) {
                                indexInfo.put("documentCount", reader.numDocs());
                                indexInfo.put("maxDoc", reader.maxDoc());
                                indexInfo.put("deletedDocs", reader.numDeletedDocs());
                            } catch (IOException e) {
                                log.warn("Could not read index: {}", indexPath, e);
                                indexInfo.put("documentCount", 0);
                                indexInfo.put("error", "Could not read index");
                            }
                            
                            // Get directory size
                            long size = getDirectorySize(indexPath);
                            indexInfo.put("sizeBytes", size);
                            indexInfo.put("sizeFormatted", formatSize(size));
                        } else {
                            indexInfo.put("documentCount", 0);
                            indexInfo.put("sizeBytes", 0);
                            indexInfo.put("sizeFormatted", "0 B");
                        }
                        
                        indices.add(indexInfo);
                    } catch (Exception e) {
                        log.error("Error processing index: {}", indexPath, e);
                    }
                });
            }
            
            return ResponseEntity.ok(indices);
        } catch (IOException e) {
            log.error("Error listing indices", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @DeleteMapping("/{indexName}")
    public ResponseEntity<Map<String, String>> deleteIndex(@PathVariable String indexName) {
        try {
            Path indexPath = Paths.get(indexConfig.getBaseDirectory(), indexName);
            
            if (!Files.exists(indexPath)) {
                return ResponseEntity.notFound().build();
            }
            
            // Delete all files in the index directory
            deleteDirectory(indexPath);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Index deleted: " + indexName);
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Error deleting index: {}", indexName, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/{indexName}/clear")
    public ResponseEntity<Map<String, String>> clearIndex(@PathVariable String indexName) {
        try {
            Path indexPath = Paths.get(indexConfig.getBaseDirectory(), indexName);
            
            if (!Files.exists(indexPath)) {
                return ResponseEntity.notFound().build();
            }
            
            // Delete and recreate the index (effectively clearing it)
            if (isValidIndex(indexPath)) {
                Directory directory = FSDirectory.open(indexPath);
                IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
                try (IndexWriter writer = new IndexWriter(directory, config)) {
                    writer.deleteAll();
                    writer.commit();
                }
            }
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Index cleared: " + indexName);
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Error clearing index: {}", indexName, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    private boolean isValidIndex(Path indexPath) {
        try {
            return Files.exists(indexPath.resolve("segments_1")) || 
                   Files.exists(indexPath.resolve("segments_2")) ||
                   Files.list(indexPath).anyMatch(p -> p.getFileName().toString().startsWith("segments_"));
        } catch (IOException e) {
            return false;
        }
    }
    
    private long getDirectorySize(Path path) {
        try (Stream<Path> walk = Files.walk(path)) {
            return walk
                .filter(Files::isRegularFile)
                .mapToLong(p -> {
                    try {
                        return Files.size(p);
                    } catch (IOException e) {
                        return 0L;
                    }
                })
                .sum();
        } catch (IOException e) {
            return 0L;
        }
    }
    
    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    private void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            try (Stream<Path> walk = Files.walk(directory)) {
                walk.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.error("Could not delete: {}", path, e);
                        }
                    });
            }
        }
    }
}
