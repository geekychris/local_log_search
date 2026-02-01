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

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the parsed content and metadata extracted from a file.
 */
public class ParsedFileContent {
    private String textContent;
    private Map<String, String> metadata;
    private Map<String, String> extractedFields;
    
    public ParsedFileContent() {
        this.metadata = new HashMap<>();
        this.extractedFields = new HashMap<>();
    }
    
    public ParsedFileContent(String textContent) {
        this();
        this.textContent = textContent;
    }
    
    public String getTextContent() {
        return textContent;
    }
    
    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }
    
    public Map<String, String> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
    
    public void addMetadata(String key, String value) {
        this.metadata.put(key, value);
    }
    
    public Map<String, String> getExtractedFields() {
        return extractedFields;
    }
    
    public void setExtractedFields(Map<String, String> extractedFields) {
        this.extractedFields = extractedFields;
    }
    
    public void addExtractedField(String key, String value) {
        this.extractedFields.put(key, value);
    }
    
    @Override
    public String toString() {
        return "ParsedFileContent{" +
                "textContentLength=" + (textContent != null ? textContent.length() : 0) +
                ", metadata=" + metadata +
                ", extractedFields=" + extractedFields +
                '}';
    }
}
