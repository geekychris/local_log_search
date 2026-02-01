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

import java.io.IOException;
import java.nio.file.Path;

/**
 * Plugin interface for parsing different file types.
 * Implementations can be loaded via Java ServiceLoader for extensibility.
 */
public interface FileContentParser {
    /**
     * Check if this parser supports the given file.
     * 
     * @param filePath the path to the file
     * @return true if this parser can handle the file
     */
    boolean supports(Path filePath);
    
    /**
     * Parse the file and extract content and metadata.
     * 
     * @param filePath the path to the file to parse
     * @param maxContentBytes maximum bytes of content to extract (for large files)
     * @return the parsed content and metadata
     * @throws IOException if parsing fails
     */
    ParsedFileContent parse(Path filePath, long maxContentBytes) throws IOException;
    
    /**
     * Get the name of this parser for logging/debugging.
     * 
     * @return parser name
     */
    String getParserName();
    
    /**
     * Get the priority of this parser (higher priority parsers are tried first).
     * Default is 0. Use higher values for more specific parsers.
     * 
     * @return priority value
     */
    default int getPriority() {
        return 0;
    }
}
