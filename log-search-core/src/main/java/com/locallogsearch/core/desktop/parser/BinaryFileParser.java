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

import org.apache.tika.Tika;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Fallback parser for binary files.
 * Indexes only metadata, no content.
 */
public class BinaryFileParser implements FileContentParser {
    private final Tika tika = new Tika();
    
    @Override
    public boolean supports(Path filePath) {
        // This is the fallback parser, supports everything
        return true;
    }
    
    @Override
    public ParsedFileContent parse(Path filePath, long maxContentBytes) throws IOException {
        // For binary files, only index filename and metadata, no content
        ParsedFileContent result = new ParsedFileContent("");
        
        // Detect mime type
        String mimeType = tika.detect(filePath);
        if (mimeType != null) {
            result.addMetadata("mime_type", mimeType);
        }
        
        // Add note that content was not indexed
        result.addMetadata("content_indexed", "false");
        result.addMetadata("reason", "binary_file");
        
        return result;
    }
    
    @Override
    public String getParserName() {
        return "BinaryFileParser";
    }
    
    @Override
    public int getPriority() {
        return -100; // Lowest priority - fallback only
    }
}
