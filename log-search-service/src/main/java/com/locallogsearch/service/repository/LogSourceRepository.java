package com.locallogsearch.service.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.locallogsearch.core.config.LogSourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for persisting log source configurations to disk.
 */
public class LogSourceRepository {
    private static final Logger log = LoggerFactory.getLogger(LogSourceRepository.class);
    private static final String LOG_SOURCES_FILE = "log-sources.json";
    
    private final Path stateDirectory;
    private final ObjectMapper objectMapper;
    
    public LogSourceRepository(Path stateDirectory) {
        this.stateDirectory = stateDirectory;
        this.objectMapper = new ObjectMapper();
        
        try {
            Files.createDirectories(stateDirectory);
        } catch (IOException e) {
            log.error("Failed to create state directory: {}", stateDirectory, e);
        }
    }
    
    /**
     * Save all log source configurations to disk.
     */
    public void saveAll(Map<String, LogSourceConfig> sources) {
        Path file = stateDirectory.resolve(LOG_SOURCES_FILE);
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), sources);
            log.info("Saved {} log source configurations to {}", sources.size(), file);
        } catch (IOException e) {
            log.error("Failed to save log sources to {}", file, e);
        }
    }
    
    /**
     * Load all log source configurations from disk.
     */
    public Map<String, LogSourceConfig> loadAll() {
        Path file = stateDirectory.resolve(LOG_SOURCES_FILE);
        
        if (!Files.exists(file)) {
            log.info("No existing log sources file found at {}", file);
            return new HashMap<>();
        }
        
        try {
            Map<String, LogSourceConfig> sources = objectMapper.readValue(
                file.toFile(), 
                objectMapper.getTypeFactory().constructMapType(
                    HashMap.class, String.class, LogSourceConfig.class
                )
            );
            log.info("Loaded {} log source configurations from {}", sources.size(), file);
            return sources;
        } catch (IOException e) {
            log.error("Failed to load log sources from {}", file, e);
            return new HashMap<>();
        }
    }
}
