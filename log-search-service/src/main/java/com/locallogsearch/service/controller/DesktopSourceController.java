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

import com.locallogsearch.core.config.DesktopSourceConfig;
import com.locallogsearch.core.desktop.DesktopIndexerManager;
import com.locallogsearch.core.desktop.FileIndexState;
import com.locallogsearch.core.desktop.IndexingResult;
import com.locallogsearch.core.index.IndexManager;
import com.locallogsearch.service.repository.DesktopSourceRepository;
import com.locallogsearch.service.repository.FileIndexStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/api/desktop-sources")
public class DesktopSourceController {
    private static final Logger log = LoggerFactory.getLogger(DesktopSourceController.class);
    
    private final DesktopIndexerManager indexerManager;
    private final IndexManager indexManager;
    private final DesktopSourceRepository repository;
    private final FileIndexStateRepository stateRepository;
    private final Map<String, DesktopSourceConfig> desktopSources;
    
    public DesktopSourceController(DesktopIndexerManager indexerManager,
                                  IndexManager indexManager,
                                  DesktopSourceRepository repository,
                                  FileIndexStateRepository stateRepository) {
        this.indexerManager = indexerManager;
        this.indexManager = indexManager;
        this.repository = repository;
        this.stateRepository = stateRepository;
        this.desktopSources = new ConcurrentHashMap<>(repository.loadAll());
    }
    
    /**
     * Initialize indexers for all saved desktop sources after controller creation.
     */
    @PostConstruct
    public void initializeIndexers() {
        if (desktopSources.isEmpty()) {
            log.info("No saved desktop sources to restore");
            return;
        }
        
        log.info("Restoring {} desktop sources", desktopSources.size());
        
        for (DesktopSourceConfig config : desktopSources.values()) {
            Map<String, FileIndexState> savedStates = stateRepository.load(config.getId());
            indexerManager.addDesktopSource(config, savedStates);
        }
    }
    
    @GetMapping
    public List<DesktopSourceConfig> getAllSources() {
        return new ArrayList<>(desktopSources.values());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<DesktopSourceConfig> getSource(@PathVariable String id) {
        DesktopSourceConfig config = desktopSources.get(id);
        if (config == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(config);
    }
    
    @PostMapping
    public ResponseEntity<DesktopSourceConfig> createSource(@RequestBody DesktopSourceConfig config) {
        if (config.getId() == null || config.getId().isEmpty()) {
            config.setId(UUID.randomUUID().toString());
        }
        
        if (desktopSources.containsKey(config.getId())) {
            return ResponseEntity.badRequest().build();
        }
        
        desktopSources.put(config.getId(), config);
        repository.saveAll(desktopSources);
        indexerManager.addDesktopSource(config, null);
        
        log.info("Created desktop source: {}", config);
        return ResponseEntity.ok(config);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<DesktopSourceConfig> updateSource(@PathVariable String id, @RequestBody DesktopSourceConfig config) {
        if (!desktopSources.containsKey(id)) {
            return ResponseEntity.notFound().build();
        }
        
        config.setId(id);
        desktopSources.put(id, config);
        repository.saveAll(desktopSources);
        
        // Remove and re-add to indexer manager
        indexerManager.removeDesktopSource(id);
        Map<String, FileIndexState> savedStates = stateRepository.load(id);
        indexerManager.addDesktopSource(config, savedStates);
        
        log.info("Updated desktop source: {}", config);
        return ResponseEntity.ok(config);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSource(@PathVariable String id) {
        DesktopSourceConfig config = desktopSources.remove(id);
        if (config == null) {
            return ResponseEntity.notFound().build();
        }
        
        repository.saveAll(desktopSources);
        indexerManager.removeDesktopSource(id);
        stateRepository.delete(id);
        
        // Optionally clear the index
        try {
            indexManager.clearIndex(config.getIndexName());
        } catch (Exception e) {
            log.error("Failed to clear index for deleted source: {}", id, e);
        }
        
        log.info("Deleted desktop source: {}", id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Trigger full indexing for a desktop source.
     */
    @PostMapping("/{id}/index")
    public ResponseEntity<Map<String, Object>> indexSource(@PathVariable String id) {
        if (!desktopSources.containsKey(id)) {
            return ResponseEntity.notFound().build();
        }
        
        try {
            Future<IndexingResult> future = indexerManager.triggerIndexing(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "started");
            response.put("message", "Indexing started for source: " + id);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to start indexing for: {}", id, e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Trigger smart re-indexing for a desktop source.
     */
    @PostMapping("/{id}/reindex")
    public ResponseEntity<Map<String, Object>> reindexSource(@PathVariable String id) {
        if (!desktopSources.containsKey(id)) {
            return ResponseEntity.notFound().build();
        }
        
        try {
            Future<IndexingResult> future = indexerManager.triggerReindexing(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "started");
            response.put("message", "Re-indexing started for source: " + id);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to start re-indexing for: {}", id, e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Get indexing status for a desktop source.
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> getIndexingStatus(@PathVariable String id) {
        if (!desktopSources.containsKey(id)) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("sourceId", id);
        response.put("isIndexing", indexerManager.isIndexing(id));
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * View/download a file by its absolute path.
     * Security: Only allows files within configured desktop source root paths.
     */
    @GetMapping("/view-file")
    public ResponseEntity<Resource> viewFile(@RequestParam String path) {
        try {
            Path filePath = Paths.get(path).normalize().toAbsolutePath();
            File file = filePath.toFile();
            
            // Security check: verify file is within a configured desktop source root
            boolean isAllowed = false;
            for (DesktopSourceConfig config : desktopSources.values()) {
                Path rootPath = Paths.get(config.getRootPath()).normalize().toAbsolutePath();
                if (filePath.startsWith(rootPath)) {
                    isAllowed = true;
                    break;
                }
            }
            
            if (!isAllowed) {
                log.warn("Attempted to access file outside configured desktop sources: {}", path);
                return ResponseEntity.status(403).build();
            }
            
            if (!file.exists() || !file.isFile()) {
                return ResponseEntity.notFound().build();
            }
            
            // Determine content type
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                // Fallback based on file extension
                String fileName = file.getName().toLowerCase();
                if (fileName.endsWith(".pdf")) {
                    contentType = "application/pdf";
                } else if (fileName.endsWith(".png")) {
                    contentType = "image/png";
                } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                    contentType = "image/jpeg";
                } else if (fileName.endsWith(".gif")) {
                    contentType = "image/gif";
                } else if (fileName.endsWith(".txt")) {
                    contentType = "text/plain";
                } else if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
                    contentType = "text/html";
                } else if (fileName.endsWith(".json")) {
                    contentType = "application/json";
                } else if (fileName.endsWith(".xml")) {
                    contentType = "application/xml";
                } else {
                    contentType = "application/octet-stream";
                }
            }
            
            Resource resource = new FileSystemResource(file);
            
            // Set headers for inline display (for browsers that support it)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentLength(file.length());
            
            // For PDFs, images, and text files, use inline; for Office docs, use attachment
            if (contentType.startsWith("image/") || 
                contentType.equals("application/pdf") || 
                contentType.startsWith("text/")) {
                // Use inline disposition for viewable files
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"");
            } else {
                // Force download for non-viewable files
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"");
            }
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("Error serving file: {}", path, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
