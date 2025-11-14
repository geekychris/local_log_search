package com.locallogsearch.core.search;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class SearchResult {
    private String rawText;
    private Instant timestamp;
    private String source;
    private String indexName;
    private Map<String, String> fields;
    private float score;
    
    public SearchResult() {
        this.fields = new HashMap<>();
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
    
    public float getScore() {
        return score;
    }
    
    public void setScore(float score) {
        this.score = score;
    }
}
