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

package com.locallogsearch.core.config;

import java.util.ArrayList;
import java.util.List;

public class DesktopSourceConfig {
    private String id;
    private String rootPath;
    private String indexName;
    private boolean enabled;
    private List<String> includePatterns;
    private List<String> excludePatterns;
    private long maxFileSizeBytes;
    private long maxIndexedContentBytes;
    
    public DesktopSourceConfig() {
        this.enabled = true;
        this.includePatterns = new ArrayList<>();
        this.excludePatterns = new ArrayList<>();
        // Default: 10MB max file size
        this.maxFileSizeBytes = 10 * 1024 * 1024;
        // Default: 1MB max indexed content per file
        this.maxIndexedContentBytes = 1024 * 1024;
        
        // Default exclude patterns
        this.excludePatterns.add("**/node_modules/**");
        this.excludePatterns.add("**/.git/**");
        this.excludePatterns.add("**/.svn/**");
        this.excludePatterns.add("**/.hg/**");
        this.excludePatterns.add("**/target/**");
        this.excludePatterns.add("**/build/**");
        this.excludePatterns.add("**/.idea/**");
        this.excludePatterns.add("**/.vscode/**");
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getRootPath() {
        return rootPath;
    }
    
    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }
    
    public String getIndexName() {
        return indexName;
    }
    
    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public List<String> getIncludePatterns() {
        return includePatterns;
    }
    
    public void setIncludePatterns(List<String> includePatterns) {
        this.includePatterns = includePatterns;
    }
    
    public List<String> getExcludePatterns() {
        return excludePatterns;
    }
    
    public void setExcludePatterns(List<String> excludePatterns) {
        this.excludePatterns = excludePatterns;
    }
    
    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }
    
    public void setMaxFileSizeBytes(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }
    
    public long getMaxIndexedContentBytes() {
        return maxIndexedContentBytes;
    }
    
    public void setMaxIndexedContentBytes(long maxIndexedContentBytes) {
        this.maxIndexedContentBytes = maxIndexedContentBytes;
    }
    
    @Override
    public String toString() {
        return "DesktopSourceConfig{" +
                "id='" + id + '\'' +
                ", rootPath='" + rootPath + '\'' +
                ", indexName='" + indexName + '\'' +
                ", enabled=" + enabled +
                ", includePatterns=" + includePatterns +
                ", excludePatterns=" + excludePatterns +
                ", maxFileSizeBytes=" + maxFileSizeBytes +
                ", maxIndexedContentBytes=" + maxIndexedContentBytes +
                '}';
    }
}
