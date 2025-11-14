package com.locallogsearch.service.controller;

import com.locallogsearch.core.config.LogSourceConfig;
import com.locallogsearch.core.tailer.TailerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/sources")
public class LogSourceController {
    private static final Logger log = LoggerFactory.getLogger(LogSourceController.class);
    
    private final TailerManager tailerManager;
    private final Map<String, LogSourceConfig> logSources;
    
    public LogSourceController(TailerManager tailerManager) {
        this.tailerManager = tailerManager;
        this.logSources = new ConcurrentHashMap<>();
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
        tailerManager.updateLogSource(config);
        return ResponseEntity.ok().build();
    }
}
