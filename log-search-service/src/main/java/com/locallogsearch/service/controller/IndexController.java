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
import com.locallogsearch.core.truncation.TruncationConfig;
import com.locallogsearch.core.truncation.TruncationPolicy;
import com.locallogsearch.service.truncation.TruncationScheduler;
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
import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/indices")
public class IndexController {
    private static final Logger log = LoggerFactory.getLogger(IndexController.class);
    
    private final IndexConfig indexConfig;
    private final TruncationScheduler truncationScheduler;
    
    public IndexController(IndexConfig indexConfig, TruncationScheduler truncationScheduler) {
        this.indexConfig = indexConfig;
        this.truncationScheduler = truncationScheduler;
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
    
    // ===== Truncation Endpoints =====
    
    /**
     * Get truncation configuration for an index.
     */
    @GetMapping("/{indexName}/truncation")
    public ResponseEntity<Map<String, Object>> getTruncationConfig(@PathVariable String indexName) {
        TruncationConfig config = truncationScheduler.getTruncationConfig(indexName);
        
        if (config == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("indexName", indexName);
            response.put("policy", "NONE");
            response.put("autoTruncateEnabled", false);
            return ResponseEntity.ok(response);
        }
        
        return ResponseEntity.ok(convertConfigToMap(config));
    }
    
    /**
     * Set or update truncation configuration for an index.
     */
    @PostMapping("/{indexName}/truncation")
    public ResponseEntity<Map<String, Object>> setTruncationConfig(
            @PathVariable String indexName,
            @RequestBody Map<String, Object> request) {
        try {
            TruncationConfig config = new TruncationConfig();
            config.setIndexName(indexName);
            
            String policyStr = (String) request.get("policy");
            TruncationPolicy policy = TruncationPolicy.valueOf(policyStr);
            config.setPolicy(policy);
            
            Boolean autoTruncate = (Boolean) request.get("autoTruncateEnabled");
            config.setAutoTruncateEnabled(autoTruncate != null && autoTruncate);
            
            if (policy == TruncationPolicy.TIME_BASED) {
                Object retentionValue = request.get("retentionValue");
                String retentionUnit = (String) request.get("retentionUnit");
                
                if (retentionValue == null || retentionUnit == null) {
                    return ResponseEntity.badRequest().body(
                        Map.of("error", "TIME_BASED policy requires retentionValue and retentionUnit")
                    );
                }
                
                Duration retention = parseDuration(retentionValue, retentionUnit);
                config.setRetentionPeriod(retention);
                
                // Set truncation interval
                Object intervalValue = request.get("intervalValue");
                String intervalUnit = (String) request.get("intervalUnit");
                if (intervalValue != null && intervalUnit != null) {
                    config.setTruncationInterval(parseDuration(intervalValue, intervalUnit));
                } else {
                    // Default: same as retention period (or daily, whichever is less)
                    config.setTruncationInterval(
                        retention.compareTo(Duration.ofDays(1)) > 0 ? Duration.ofDays(1) : retention
                    );
                }
                
            } else if (policy == TruncationPolicy.VOLUME_BASED) {
                Object maxDocsObj = request.get("maxDocuments");
                if (maxDocsObj == null) {
                    return ResponseEntity.badRequest().body(
                        Map.of("error", "VOLUME_BASED policy requires maxDocuments")
                    );
                }
                
                long maxDocuments = ((Number) maxDocsObj).longValue();
                config.setMaxDocuments(maxDocuments);
                
                // Set truncation interval (default: hourly for volume-based)
                Object intervalValue = request.get("intervalValue");
                String intervalUnit = (String) request.get("intervalUnit");
                if (intervalValue != null && intervalUnit != null) {
                    config.setTruncationInterval(parseDuration(intervalValue, intervalUnit));
                } else {
                    config.setTruncationInterval(Duration.ofHours(1));
                }
            }
            
            truncationScheduler.setTruncationConfig(config);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Truncation configuration updated");
            response.put("config", convertConfigToMap(config));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error setting truncation config for index: {}", indexName, e);
            return ResponseEntity.badRequest().body(
                Map.of("error", "Invalid truncation configuration: " + e.getMessage())
            );
        }
    }
    
    /**
     * Delete truncation configuration for an index.
     */
    @DeleteMapping("/{indexName}/truncation")
    public ResponseEntity<Map<String, String>> deleteTruncationConfig(@PathVariable String indexName) {
        truncationScheduler.removeTruncationConfig(indexName);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Truncation configuration removed for: " + indexName);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Manually trigger truncation for an index.
     */
    @PostMapping("/{indexName}/truncate")
    public ResponseEntity<Map<String, Object>> truncateIndex(@PathVariable String indexName) {
        try {
            int deletedCount = truncationScheduler.executeTruncation(indexName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Truncation completed");
            response.put("indexName", indexName);
            response.put("deletedDocuments", deletedCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error truncating index: {}", indexName, e);
            return ResponseEntity.internalServerError().body(
                Map.of("error", "Failed to truncate index: " + e.getMessage())
            );
        }
    }
    
    /**
     * Get all truncation configurations.
     */
    @GetMapping("/truncation")
    public ResponseEntity<List<Map<String, Object>>> getAllTruncationConfigs() {
        Map<String, TruncationConfig> configs = truncationScheduler.getAllConfigs();
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (TruncationConfig config : configs.values()) {
            result.add(convertConfigToMap(config));
        }
        
        return ResponseEntity.ok(result);
    }
    
    private Map<String, Object> convertConfigToMap(TruncationConfig config) {
        Map<String, Object> map = new HashMap<>();
        map.put("indexName", config.getIndexName());
        map.put("policy", config.getPolicy().toString());
        map.put("autoTruncateEnabled", config.isAutoTruncateEnabled());
        
        if (config.getRetentionPeriod() != null) {
            map.put("retentionPeriodSeconds", config.getRetentionPeriod().getSeconds());
            map.put("retentionPeriodFormatted", formatDuration(config.getRetentionPeriod()));
        }
        
        if (config.getMaxDocuments() != null) {
            map.put("maxDocuments", config.getMaxDocuments());
        }
        
        if (config.getTruncationInterval() != null) {
            map.put("truncationIntervalSeconds", config.getTruncationInterval().getSeconds());
            map.put("truncationIntervalFormatted", formatDuration(config.getTruncationInterval()));
        }
        
        return map;
    }
    
    private Duration parseDuration(Object value, String unit) {
        long numValue = ((Number) value).longValue();
        
        switch (unit.toLowerCase()) {
            case "minutes":
                return Duration.ofMinutes(numValue);
            case "hours":
                return Duration.ofHours(numValue);
            case "days":
                return Duration.ofDays(numValue);
            case "seconds":
            default:
                return Duration.ofSeconds(numValue);
        }
    }
    
    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        
        if (seconds < 60) {
            return seconds + " second" + (seconds != 1 ? "s" : "");
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            return minutes + " minute" + (minutes != 1 ? "s" : "");
        } else if (seconds < 86400) {
            long hours = seconds / 3600;
            return hours + " hour" + (hours != 1 ? "s" : "");
        } else {
            long days = seconds / 86400;
            return days + " day" + (days != 1 ? "s" : "");
        }
    }
}
