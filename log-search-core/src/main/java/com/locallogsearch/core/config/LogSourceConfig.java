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

import java.util.HashMap;
import java.util.Map;

public class LogSourceConfig {
    private String id;
    private String filePath;
    private String indexName;
    private String parserType; // "keyvalue", "regex", "grok", "custom"
    private Map<String, String> parserConfig;
    private boolean enabled;
    
    public LogSourceConfig() {
        this.parserConfig = new HashMap<>();
        this.enabled = true;
        this.parserType = "keyvalue";
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public String getIndexName() {
        return indexName;
    }
    
    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }
    
    public String getParserType() {
        return parserType;
    }
    
    public void setParserType(String parserType) {
        this.parserType = parserType;
    }
    
    public Map<String, String> getParserConfig() {
        return parserConfig;
    }
    
    public void setParserConfig(Map<String, String> parserConfig) {
        this.parserConfig = parserConfig;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    @Override
    public String toString() {
        return "LogSourceConfig{" +
                "id='" + id + '\'' +
                ", filePath='" + filePath + '\'' +
                ", indexName='" + indexName + '\'' +
                ", parserType='" + parserType + '\'' +
                ", enabled=" + enabled +
                '}';
    }
}
