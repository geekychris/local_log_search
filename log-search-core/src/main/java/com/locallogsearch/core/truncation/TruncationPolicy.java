package com.locallogsearch.core.truncation;

public enum TruncationPolicy {
    /**
     * Keep only documents within a certain time window (e.g., last 24 hours)
     */
    TIME_BASED,
    
    /**
     * Keep only the most recent N documents
     */
    VOLUME_BASED,
    
    /**
     * No automatic truncation
     */
    NONE
}
