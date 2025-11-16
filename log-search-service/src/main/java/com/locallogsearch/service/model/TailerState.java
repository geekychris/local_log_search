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

package com.locallogsearch.service.model;

/**
 * Represents the persistent state of a file tailer.
 */
public class TailerState {
    private String filePath;
    private long filePointer;
    private long lastModifiedTime;
    private long fileSize;
    private String fileKey;  // Unique identifier for the file (inode-like)
    private long lastCheckpointTime;
    
    public TailerState() {
    }
    
    public TailerState(String filePath, long filePointer, long lastModifiedTime, 
                       long fileSize, String fileKey) {
        this.filePath = filePath;
        this.filePointer = filePointer;
        this.lastModifiedTime = lastModifiedTime;
        this.fileSize = fileSize;
        this.fileKey = fileKey;
        this.lastCheckpointTime = System.currentTimeMillis();
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public long getFilePointer() {
        return filePointer;
    }
    
    public void setFilePointer(long filePointer) {
        this.filePointer = filePointer;
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
    
    public String getFileKey() {
        return fileKey;
    }
    
    public void setFileKey(String fileKey) {
        this.fileKey = fileKey;
    }
    
    public long getLastCheckpointTime() {
        return lastCheckpointTime;
    }
    
    public void setLastCheckpointTime(long lastCheckpointTime) {
        this.lastCheckpointTime = lastCheckpointTime;
    }
    
    @Override
    public String toString() {
        return "TailerState{" +
                "filePath='" + filePath + '\'' +
                ", filePointer=" + filePointer +
                ", lastModifiedTime=" + lastModifiedTime +
                ", fileSize=" + fileSize +
                ", fileKey='" + fileKey + '\'' +
                ", lastCheckpointTime=" + lastCheckpointTime +
                '}';
    }
}
