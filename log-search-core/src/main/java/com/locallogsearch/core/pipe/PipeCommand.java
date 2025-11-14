package com.locallogsearch.core.pipe;

import com.locallogsearch.core.search.SearchResult;
import java.util.List;

/**
 * Interface for pipe commands that process search results
 */
public interface PipeCommand {
    /**
     * Process the input results and return transformed output
     */
    PipeResult execute(List<SearchResult> input);
    
    /**
     * Get the command name (e.g., "stats", "chart", "timechart")
     */
    String getName();
}
