package com.locallogsearch.core.truncation;

import java.time.Duration;

public class TruncationConfig {
    private String indexName;
    private TruncationPolicy policy;
    private Duration retentionPeriod; // For TIME_BASED policy
    private Long maxDocuments; // For VOLUME_BASED policy
    private boolean autoTruncateEnabled;
    private Duration truncationInterval; // How often to run auto-truncation
    
    public TruncationConfig() {
        this.policy = TruncationPolicy.NONE;
        this.autoTruncateEnabled = false;
    }
    
    public TruncationConfig(String indexName, TruncationPolicy policy) {
        this.indexName = indexName;
        this.policy = policy;
        this.autoTruncateEnabled = false;
    }
    
    // Static factory methods for common configurations
    public static TruncationConfig timeBased(String indexName, Duration retentionPeriod, boolean autoTruncate) {
        TruncationConfig config = new TruncationConfig(indexName, TruncationPolicy.TIME_BASED);
        config.setRetentionPeriod(retentionPeriod);
        config.setAutoTruncateEnabled(autoTruncate);
        // Default: run truncation at the same interval as retention period (or daily, whichever is less)
        config.setTruncationInterval(retentionPeriod.compareTo(Duration.ofDays(1)) > 0 
            ? Duration.ofDays(1) 
            : retentionPeriod);
        return config;
    }
    
    public static TruncationConfig volumeBased(String indexName, long maxDocuments, boolean autoTruncate) {
        TruncationConfig config = new TruncationConfig(indexName, TruncationPolicy.VOLUME_BASED);
        config.setMaxDocuments(maxDocuments);
        config.setAutoTruncateEnabled(autoTruncate);
        // Default: check every hour for volume-based truncation
        config.setTruncationInterval(Duration.ofHours(1));
        return config;
    }
    
    public String getIndexName() {
        return indexName;
    }
    
    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }
    
    public TruncationPolicy getPolicy() {
        return policy;
    }
    
    public void setPolicy(TruncationPolicy policy) {
        this.policy = policy;
    }
    
    public Duration getRetentionPeriod() {
        return retentionPeriod;
    }
    
    public void setRetentionPeriod(Duration retentionPeriod) {
        this.retentionPeriod = retentionPeriod;
    }
    
    public Long getMaxDocuments() {
        return maxDocuments;
    }
    
    public void setMaxDocuments(Long maxDocuments) {
        this.maxDocuments = maxDocuments;
    }
    
    public boolean isAutoTruncateEnabled() {
        return autoTruncateEnabled;
    }
    
    public void setAutoTruncateEnabled(boolean autoTruncateEnabled) {
        this.autoTruncateEnabled = autoTruncateEnabled;
    }
    
    public Duration getTruncationInterval() {
        return truncationInterval;
    }
    
    public void setTruncationInterval(Duration truncationInterval) {
        this.truncationInterval = truncationInterval;
    }
    
    @Override
    public String toString() {
        return "TruncationConfig{" +
                "indexName='" + indexName + '\'' +
                ", policy=" + policy +
                ", retentionPeriod=" + retentionPeriod +
                ", maxDocuments=" + maxDocuments +
                ", autoTruncateEnabled=" + autoTruncateEnabled +
                ", truncationInterval=" + truncationInterval +
                '}';
    }
}
