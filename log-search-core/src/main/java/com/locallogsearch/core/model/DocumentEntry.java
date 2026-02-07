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

package com.locallogsearch.core.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a desktop file/document to be indexed.
 * Similar to LogEntry but for static file content.
 */
public class DocumentEntry {
    private String filePath;
    private String fileName;
    private String fileExtension;
    private long fileSize;
    private Instant createdDate;
    private Instant modifiedDate;
    private String content;
    private String indexName;
    private Map<String, String> metadata;
    private Map<String, String> extractedFields;
    
    public DocumentEntry() {
        this.metadata = new HashMap<>();
        this.extractedFields = new HashMap<>();
    }
    
    public DocumentEntry(String filePath, String indexName) {
        this();
        this.filePath = filePath;
        this.indexName = indexName;
        
        // Extract file name and extension
        int lastSeparator = filePath.lastIndexOf('/');
        if (lastSeparator == -1) {
            lastSeparator = filePath.lastIndexOf('\\');
        }
        this.fileName = lastSeparator >= 0 ? filePath.substring(lastSeparator + 1) : filePath;
        
        int lastDot = fileName.lastIndexOf('.');
        this.fileExtension = lastDot > 0 ? fileName.substring(lastDot + 1) : "";
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getFileExtension() {
        return fileExtension;
    }
    
    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
    
    public Instant getCreatedDate() {
        return createdDate;
    }
    
    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }
    
    public Instant getModifiedDate() {
        return modifiedDate;
    }
    
    public void setModifiedDate(Instant modifiedDate) {
        this.modifiedDate = modifiedDate;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getIndexName() {
        return indexName;
    }
    
    public void setIndexName(String indexName) {
        this.indexName = indexName;
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
        return "DocumentEntry{" +
                "filePath='" + filePath + '\'' +
                ", fileName='" + fileName + '\'' +
                ", fileExtension='" + fileExtension + '\'' +
                ", fileSize=" + fileSize +
                ", modifiedDate=" + modifiedDate +
                ", indexName='" + indexName + '\'' +
                ", metadata=" + metadata +
                ", extractedFields=" + extractedFields +
                '}';
    }
}
