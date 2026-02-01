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

package com.locallogsearch.core.desktop;

import java.time.Instant;

/**
 * Tracks the state of an indexed file for change detection.
 */
public class FileIndexState {
    private String filePath;
    private long lastModifiedTime;
    private long fileSize;
    private Instant indexedTimestamp;
    
    public FileIndexState() {
    }
    
    public FileIndexState(String filePath, long lastModifiedTime, long fileSize, Instant indexedTimestamp) {
        this.filePath = filePath;
        this.lastModifiedTime = lastModifiedTime;
        this.fileSize = fileSize;
        this.indexedTimestamp = indexedTimestamp;
    }
    
    /**
     * Check if the file has changed since it was indexed.
     */
    public boolean hasChanged(long currentModifiedTime, long currentSize) {
        return currentModifiedTime != lastModifiedTime || currentSize != fileSize;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public long getLastModifiedTime() {
        return lastModifiedTime;
    }
    
    public void setLastModifiedTime(long lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
    
    public Instant getIndexedTimestamp() {
        return indexedTimestamp;
    }
    
    public void setIndexedTimestamp(Instant indexedTimestamp) {
        this.indexedTimestamp = indexedTimestamp;
    }
    
    @Override
    public String toString() {
        return "FileIndexState{" +
                "filePath='" + filePath + '\'' +
                ", lastModifiedTime=" + lastModifiedTime +
                ", fileSize=" + fileSize +
                ", indexedTimestamp=" + indexedTimestamp +
                '}';
    }
}
