package com.locallogsearch.service.controller;

import com.locallogsearch.core.config.LogSourceConfig;
import com.locallogsearch.core.index.IndexManager;
import com.locallogsearch.core.tailer.FileTailerState;
import com.locallogsearch.core.tailer.TailerManager;
import com.locallogsearch.service.model.TailerState;
import com.locallogsearch.service.repository.LogSourceRepository;
import com.locallogsearch.service.repository.TailerStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/sources")
public class LogSourceController {
    private static final Logger log = LoggerFactory.getLogger(LogSourceController.class);
    
    private final TailerManager tailerManager;
    private final IndexManager indexManager;
    private final LogSourceRepository repository;
    private final TailerStateRepository stateRepository;
    private final Map<String, LogSourceConfig> logSources;
    
    public LogSourceController(TailerManager tailerManager,
                              IndexManager indexManager,
                              LogSourceRepository repository,
                              TailerStateRepository stateRepository) {
        this.tailerManager = tailerManager;
        this.indexManager = indexManager;
        this.repository = repository;
        this.stateRepository = stateRepository;
        this.logSources = new ConcurrentHashMap<>(repository.loadAll());
    }
    
    /**
     * Initialize tailers for all saved log sources after controller creation.
     */
    @PostConstruct
    public void initializeTailers() {
        if (logSources.isEmpty()) {
            log.info("No saved log sources to restore");
            return;
        }
        
        log.info("Restoring {} log sources", logSources.size());
        Map<String, TailerState> savedStates = stateRepository.loadAll();
        
        for (LogSourceConfig config : logSources.values()) {
            TailerState savedState = savedStates.get(config.getId());
            FileTailerState initialState = null;
            
            if (savedState != null) {
                initialState = new FileTailerState(
                    savedState.getFilePath(),
                    savedState.getFilePointer(),
                    savedState.getLastModifiedTime(),
                    savedState.getFileSize(),
                    savedState.getFileKey()
                );
                log.info("Restoring tailer for {} from checkpoint at position {}", 
                    config.getFilePath(), savedState.getFilePointer());
            }
            
            tailerManager.addLogSource(config, initialState);
        }
    }
    
    @GetMapping
    public List<LogSourceConfig> getAllSources() {
        return new ArrayList<>(logSources.values());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<LogSourceConfig> getSource(@PathVariable String id) {
        LogSourceConfig config = logSources.get(id);
        if (config == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(config);
    }
    
    @PostMapping
    public ResponseEntity<LogSourceConfig> createSource(@RequestBody LogSourceConfig config) {
        if (config.getId() == null || config.getId().isEmpty()) {
            config.setId(java.util.UUID.randomUUID().toString());
        }
        
        if (logSources.containsKey(config.getId())) {
            return ResponseEntity.badRequest().build();
        }
        
        logSources.put(config.getId(), config);
        repository.saveAll(logSources);
        tailerManager.addLogSource(config);
        
        log.info("Created log source: {}", config);
        return ResponseEntity.ok(config);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<LogSourceConfig> updateSource(@PathVariable String id, @RequestBody LogSourceConfig config) {
        if (!logSources.containsKey(id)) {
            return ResponseEntity.notFound().build();
        }
        
        config.setId(id);
        logSources.put(id, config);
        repository.saveAll(logSources);
        tailerManager.updateLogSource(config);
        
        log.info("Updated log source: {}", config);
        return ResponseEntity.ok(config);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSource(@PathVariable String id) {
        LogSourceConfig config = logSources.remove(id);
        if (config == null) {
            return ResponseEntity.notFound().build();
        }
        
        repository.saveAll(logSources);
        tailerManager.removeLogSource(id);
        log.info("Deleted log source: {}", id);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{id}/enable")
    public ResponseEntity<Void> enableSource(@PathVariable String id) {
        LogSourceConfig config = logSources.get(id);
        if (config == null) {
            return ResponseEntity.notFound().build();
        }
        
        config.setEnabled(true);
        repository.saveAll(logSources);
        tailerManager.updateLogSource(config);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{id}/disable")
    public ResponseEntity<Void> disableSource(@PathVariable String id) {
        LogSourceConfig config = logSources.get(id);
        if (config == null) {
            return ResponseEntity.notFound().build();
        }
        
        config.setEnabled(false);
        repository.saveAll(logSources);
        tailerManager.updateLogSource(config);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Reindex a log source by clearing its index and re-reading the entire log file.
     * This will:
     * 1. Clear all documents from the index
     * 2. Delete the tailer checkpoint state
     * 3. Restart the tailer from the beginning of the file
     */
    @PostMapping("/{id}/reindex")
    public ResponseEntity<Map<String, String>> reindexSource(@PathVariable("id") String id) {
        LogSourceConfig config = logSources.get(id);
        if (config == null) {
            return ResponseEntity.notFound().build();
        }
        
        try {
            log.info("Starting reindex for source: {} (index: {})", id, config.getIndexName());
            
            // Clear the index
            indexManager.clearIndex(config.getIndexName());
            
            // Delete the tailer state so it starts from the beginning
            stateRepository.remove(id);
            
            // Restart the tailer from the beginning
            tailerManager.reindexLogSource(config);
            
            Map<String, String> response = new java.util.HashMap<>();
            response.put("message", "Reindexing started for source: " + id);
            response.put("indexName", config.getIndexName());
            response.put("filePath", config.getFilePath());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error reindexing source: {}", id, e);
            Map<String, String> error = new java.util.HashMap<>();
            error.put("error", "Failed to reindex: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
