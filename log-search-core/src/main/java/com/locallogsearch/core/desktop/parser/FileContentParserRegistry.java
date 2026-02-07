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

package com.locallogsearch.core.desktop.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Registry for managing file content parsers.
 * Supports plugin loading via Java ServiceLoader.
 */
public class FileContentParserRegistry {
    private static final Logger log = LoggerFactory.getLogger(FileContentParserRegistry.class);
    
    private final List<FileContentParser> parsers;
    
    public FileContentParserRegistry() {
        this.parsers = new ArrayList<>();
        registerBuiltInParsers();
        loadPluginParsers();
    }
    
    /**
     * Register built-in parsers that ship with the application.
     */
    private void registerBuiltInParsers() {
        register(new PlainTextParser());
        register(new PdfParser());
        register(new OfficeDocumentParser());
        register(new StructuredDataParser());
        register(new MarkdownParser());
        register(new BinaryFileParser());
        
        log.info("Registered {} built-in file parsers", parsers.size());
    }
    
    /**
     * Load plugin parsers via Java ServiceLoader.
     */
    private void loadPluginParsers() {
        ServiceLoader<FileContentParser> loader = ServiceLoader.load(FileContentParser.class);
        int pluginCount = 0;
        
        for (FileContentParser parser : loader) {
            register(parser);
            pluginCount++;
            log.info("Loaded plugin parser: {}", parser.getParserName());
        }
        
        if (pluginCount > 0) {
            log.info("Loaded {} plugin parsers", pluginCount);
        }
    }
    
    /**
     * Register a parser. Parsers are sorted by priority (highest first).
     */
    public void register(FileContentParser parser) {
        parsers.add(parser);
        // Sort by priority descending
        parsers.sort(Comparator.comparingInt(FileContentParser::getPriority).reversed());
    }
    
    /**
     * Find the appropriate parser for a given file.
     * Returns the first parser that supports the file.
     * 
     * @param filePath the path to the file
     * @return the parser, or null if no parser supports the file
     */
    public FileContentParser findParser(Path filePath) {
        for (FileContentParser parser : parsers) {
            if (parser.supports(filePath)) {
                return parser;
            }
        }
        return null;
    }
    
    /**
     * Get all registered parsers.
     */
    public List<FileContentParser> getAllParsers() {
        return new ArrayList<>(parsers);
    }
}
