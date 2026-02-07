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
import java.util.ArrayList;
import java.util.List;

/**
 * Result of an indexing operation.
 */
public class IndexingResult {
    private int filesScanned;
    private int filesIndexed;
    private int filesSkipped;
    private int filesErrored;
    private long bytesIndexed;
    private Instant startTime;
    private Instant endTime;
    private List<String> errors;
    
    public IndexingResult() {
        this.errors = new ArrayList<>();
        this.startTime = Instant.now();
    }
    
    public void finish() {
        this.endTime = Instant.now();
    }
    
    public void incrementFilesScanned() {
        filesScanned++;
    }
    
    public void incrementFilesIndexed() {
        filesIndexed++;
    }
    
    public void incrementFilesSkipped() {
        filesSkipped++;
    }
    
    public void incrementFilesErrored() {
        filesErrored++;
    }
    
    public void addBytesIndexed(long bytes) {
        bytesIndexed += bytes;
    }
    
    public void addError(String error) {
        errors.add(error);
    }
    
    public int getFilesScanned() {
        return filesScanned;
    }
    
    public int getFilesIndexed() {
        return filesIndexed;
    }
    
    public int getFilesSkipped() {
        return filesSkipped;
    }
    
    public int getFilesErrored() {
        return filesErrored;
    }
    
    public long getBytesIndexed() {
        return bytesIndexed;
    }
    
    public Instant getStartTime() {
        return startTime;
    }
    
    public Instant getEndTime() {
        return endTime;
    }
    
    public List<String> getErrors() {
        return errors;
    }
    
    public long getDurationMillis() {
        if (endTime == null) {
            return Instant.now().toEpochMilli() - startTime.toEpochMilli();
        }
        return endTime.toEpochMilli() - startTime.toEpochMilli();
    }
    
    @Override
    public String toString() {
        return "IndexingResult{" +
                "filesScanned=" + filesScanned +
                ", filesIndexed=" + filesIndexed +
                ", filesSkipped=" + filesSkipped +
                ", filesErrored=" + filesErrored +
                ", bytesIndexed=" + bytesIndexed +
                ", durationMs=" + getDurationMillis() +
                ", errors=" + errors.size() +
                '}';
    }
}
