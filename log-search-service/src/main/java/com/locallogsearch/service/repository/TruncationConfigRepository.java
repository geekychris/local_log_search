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
import com.locallogsearch.core.truncation.TruncationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Repository
public class TruncationConfigRepository {
    private static final Logger log = LoggerFactory.getLogger(TruncationConfigRepository.class);
    private static final String STATE_FILE = "state/truncation-configs.json";
    
    private final ObjectMapper objectMapper;
    
    public TruncationConfigRepository() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    /**
     * Save all truncation configurations to disk.
     */
    public void saveAll(Map<String, TruncationConfig> configs) {
        try {
            Path path = Paths.get(STATE_FILE);
            Files.createDirectories(path.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), configs);
            log.debug("Saved {} truncation configurations", configs.size());
        } catch (IOException e) {
            log.error("Failed to save truncation configurations", e);
        }
    }
    
    /**
     * Load all truncation configurations from disk.
     */
    public Map<String, TruncationConfig> loadAll() {
        try {
            File file = new File(STATE_FILE);
            if (!file.exists()) {
                log.info("No saved truncation configurations found");
                return new HashMap<>();
            }
            
            Map<String, TruncationConfig> configs = objectMapper.readValue(
                file, 
                objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, TruncationConfig.class)
            );
            log.info("Loaded {} truncation configurations", configs.size());
            return configs;
        } catch (IOException e) {
            log.error("Failed to load truncation configurations", e);
            return new HashMap<>();
        }
    }
    
    /**
     * Remove a specific truncation configuration.
     */
    public void remove(String indexName) {
        Map<String, TruncationConfig> configs = loadAll();
        configs.remove(indexName);
        saveAll(configs);
        log.info("Removed truncation configuration for index: {}", indexName);
    }
}
