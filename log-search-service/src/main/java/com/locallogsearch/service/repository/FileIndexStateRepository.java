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

package com.locallogsearch.service.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.locallogsearch.core.desktop.FileIndexState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Repository for persisting file index states to disk.
 * Stores state per desktop source for smart re-indexing.
 */
public class FileIndexStateRepository {
    private static final Logger log = LoggerFactory.getLogger(FileIndexStateRepository.class);
    private static final String FILE_STATES_PREFIX = "file-states-";
    private static final String FILE_STATES_SUFFIX = ".json";
    
    private final Path stateDirectory;
    private final ObjectMapper objectMapper;
    
    public FileIndexStateRepository(Path stateDirectory) {
        this.stateDirectory = stateDirectory;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        
        try {
            Files.createDirectories(stateDirectory);
        } catch (IOException e) {
            log.error("Failed to create state directory: {}", stateDirectory, e);
        }
    }
    
    /**
     * Save file states for a specific desktop source.
     */
    public void save(String sourceId, Map<String, FileIndexState> states) {
        Path file = stateDirectory.resolve(FILE_STATES_PREFIX + sourceId + FILE_STATES_SUFFIX);
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), states);
            log.debug("Saved {} file states for source {} to {}", states.size(), sourceId, file);
        } catch (IOException e) {
            log.error("Failed to save file states for source {} to {}", sourceId, file, e);
        }
    }
    
    /**
     * Load file states for a specific desktop source.
     */
    public Map<String, FileIndexState> load(String sourceId) {
        Path file = stateDirectory.resolve(FILE_STATES_PREFIX + sourceId + FILE_STATES_SUFFIX);
        
        if (!Files.exists(file)) {
            log.debug("No existing file states found for source {} at {}", sourceId, file);
            return new HashMap<>();
        }
        
        try {
            Map<String, FileIndexState> states = objectMapper.readValue(
                file.toFile(),
                objectMapper.getTypeFactory().constructMapType(
                    HashMap.class, String.class, FileIndexState.class
                )
            );
            log.info("Loaded {} file states for source {} from {}", states.size(), sourceId, file);
            return states;
        } catch (IOException e) {
            log.error("Failed to load file states for source {} from {}", sourceId, file, e);
            return new HashMap<>();
        }
    }
    
    /**
     * Delete file states for a specific desktop source.
     */
    public void delete(String sourceId) {
        Path file = stateDirectory.resolve(FILE_STATES_PREFIX + sourceId + FILE_STATES_SUFFIX);
        try {
            Files.deleteIfExists(file);
            log.info("Deleted file states for source {}", sourceId);
        } catch (IOException e) {
            log.error("Failed to delete file states for source {}", sourceId, e);
        }
    }
}
