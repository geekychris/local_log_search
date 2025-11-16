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

package com.locallogsearch.core.tailer;

/**
 * Represents the runtime state of a file tailer (used for checkpointing).
 */
public class FileTailerState {
    private final String filePath;
    private final long filePointer;
    private final long lastModifiedTime;
    private final long fileSize;
    private final String fileKey;
    
    public FileTailerState(String filePath, long filePointer, long lastModifiedTime,
                           long fileSize, String fileKey) {
        this.filePath = filePath;
        this.filePointer = filePointer;
        this.lastModifiedTime = lastModifiedTime;
        this.fileSize = fileSize;
        this.fileKey = fileKey;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public long getFilePointer() {
        return filePointer;
    }
    
    public long getLastModifiedTime() {
        return lastModifiedTime;
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    public String getFileKey() {
        return fileKey;
    }
}
