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
import com.locallogsearch.core.config.DesktopSourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Repository for persisting desktop source configurations to disk.
 */
public class DesktopSourceRepository {
    private static final Logger log = LoggerFactory.getLogger(DesktopSourceRepository.class);
    private static final String DESKTOP_SOURCES_FILE = "desktop-sources.json";
    
    private final Path stateDirectory;
    private final ObjectMapper objectMapper;
    
    public DesktopSourceRepository(Path stateDirectory) {
        this.stateDirectory = stateDirectory;
        this.objectMapper = new ObjectMapper();
        
        try {
            Files.createDirectories(stateDirectory);
        } catch (IOException e) {
            log.error("Failed to create state directory: {}", stateDirectory, e);
        }
    }
    
    /**
     * Save all desktop source configurations to disk.
     */
    public void saveAll(Map<String, DesktopSourceConfig> sources) {
        Path file = stateDirectory.resolve(DESKTOP_SOURCES_FILE);
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), sources);
            log.info("Saved {} desktop source configurations to {}", sources.size(), file);
        } catch (IOException e) {
            log.error("Failed to save desktop sources to {}", file, e);
        }
    }
    
    /**
     * Load all desktop source configurations from disk.
     */
    public Map<String, DesktopSourceConfig> loadAll() {
        Path file = stateDirectory.resolve(DESKTOP_SOURCES_FILE);
        
        if (!Files.exists(file)) {
            log.info("No existing desktop sources file found at {}", file);
            return new HashMap<>();
        }
        
        try {
            Map<String, DesktopSourceConfig> sources = objectMapper.readValue(
                file.toFile(), 
                objectMapper.getTypeFactory().constructMapType(
                    HashMap.class, String.class, DesktopSourceConfig.class
                )
            );
            log.info("Loaded {} desktop source configurations from {}", sources.size(), file);
            return sources;
        } catch (IOException e) {
            log.error("Failed to load desktop sources from {}", file, e);
            return new HashMap<>();
        }
    }
}
