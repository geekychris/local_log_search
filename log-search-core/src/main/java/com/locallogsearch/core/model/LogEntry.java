package com.locallogsearch.core.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class LogEntry {
    private String rawText;
    private Instant timestamp;
    private String source;
    private String indexName;
    private Map<String, String> fields;
    
    public LogEntry() {
        this.fields = new HashMap<>();
    }
    
    public LogEntry(String rawText, String source, String indexName) {
        this();
        this.rawText = rawText;
        this.source = source;
        this.indexName = indexName;
        this.timestamp = Instant.now();
    }
    
    public String getRawText() {
        return rawText;
    }
    
    public void setRawText(String rawText) {
        this.rawText = rawText;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public String getIndexName() {
        return indexName;
    }
    
    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }
    
    public Map<String, String> getFields() {
        return fields;
    }
    
    public void setFields(Map<String, String> fields) {
        this.fields = fields;
    }
    
    public void addField(String key, String value) {
        this.fields.put(key, value);
    }
    
    @Override
    public String toString() {
        return "LogEntry{" +
                "timestamp=" + timestamp +
                ", source='" + source + '\'' +
                ", indexName='" + indexName + '\'' +
                ", fields=" + fields +
                ", rawText='" + rawText + '\'' +
                '}';
    }
}
