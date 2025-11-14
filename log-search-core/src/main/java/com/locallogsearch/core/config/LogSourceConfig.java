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
