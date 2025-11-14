package com.locallogsearch.core.config;

public class IndexConfig {
    private String baseDirectory;
    private int commitIntervalSeconds;
    private int maxBufferedDocs;
    
    public IndexConfig() {
        this.baseDirectory = System.getProperty("user.home") + "/.local_log_search/indices";
        this.commitIntervalSeconds = 15;
        this.maxBufferedDocs = 1000;
    }
    
    public String getBaseDirectory() {
        return baseDirectory;
    }
    
    public void setBaseDirectory(String baseDirectory) {
        this.baseDirectory = baseDirectory;
    }
    
    public int getCommitIntervalSeconds() {
        return commitIntervalSeconds;
    }
    
    public void setCommitIntervalSeconds(int commitIntervalSeconds) {
        this.commitIntervalSeconds = commitIntervalSeconds;
    }
    
    public int getMaxBufferedDocs() {
        return maxBufferedDocs;
    }
    
    public void setMaxBufferedDocs(int maxBufferedDocs) {
        this.maxBufferedDocs = maxBufferedDocs;
    }
}
