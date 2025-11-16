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
import com.locallogsearch.service.model.TailerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Repository for persisting tailer state (file position, inode, etc.) to disk.
 */
public class TailerStateRepository {
    private static final Logger log = LoggerFactory.getLogger(TailerStateRepository.class);
    private static final String TAILER_STATE_FILE = "tailer-state.json";
    
    private final Path stateDirectory;
    private final ObjectMapper objectMapper;
    
    public TailerStateRepository(Path stateDirectory) {
        this.stateDirectory = stateDirectory;
        this.objectMapper = new ObjectMapper();
        
        try {
            Files.createDirectories(stateDirectory);
        } catch (IOException e) {
            log.error("Failed to create state directory: {}", stateDirectory, e);
        }
    }
    
    /**
     * Save all tailer states to disk.
     */
    public void saveAll(Map<String, TailerState> states) {
        Path file = stateDirectory.resolve(TAILER_STATE_FILE);
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), states);
            log.debug("Saved {} tailer states to {}", states.size(), file);
        } catch (IOException e) {
            log.error("Failed to save tailer states to {}", file, e);
        }
    }
    
    /**
     * Save a single tailer state to disk.
     */
    public void save(String sourceId, TailerState state) {
        Map<String, TailerState> states = loadAll();
        states.put(sourceId, state);
        saveAll(states);
    }
    
    /**
     * Load all tailer states from disk.
     */
    public Map<String, TailerState> loadAll() {
        Path file = stateDirectory.resolve(TAILER_STATE_FILE);
        
        if (!Files.exists(file)) {
            log.debug("No existing tailer state file found at {}", file);
            return new HashMap<>();
        }
        
        try {
            Map<String, TailerState> states = objectMapper.readValue(
                file.toFile(),
                objectMapper.getTypeFactory().constructMapType(
                    HashMap.class, String.class, TailerState.class
                )
            );
            log.info("Loaded {} tailer states from {}", states.size(), file);
            return states;
        } catch (IOException e) {
            log.error("Failed to load tailer states from {}", file, e);
            return new HashMap<>();
        }
    }
    
    /**
     * Load a single tailer state from disk.
     */
    public TailerState load(String sourceId) {
        return loadAll().get(sourceId);
    }
    
    /**
     * Remove a tailer state from disk.
     */
    public void remove(String sourceId) {
        Map<String, TailerState> states = loadAll();
        states.remove(sourceId);
        saveAll(states);
    }
}
