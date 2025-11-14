package com.locallogsearch.core.parser;

import com.locallogsearch.core.model.LogEntry;

public interface LogParser {
    /**
     * Parse a raw log line into a LogEntry with extracted fields
     */
    void parse(LogEntry entry);
    
    /**
     * Initialize the parser with configuration
     */
    void configure(java.util.Map<String, String> config);
}
