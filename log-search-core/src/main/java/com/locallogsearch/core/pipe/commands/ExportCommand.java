package com.locallogsearch.core.pipe.commands;

import com.locallogsearch.core.pipe.PipeCommand;
import com.locallogsearch.core.pipe.PipeResult;
import com.locallogsearch.core.search.SearchResult;

import java.util.*;

/**
 * Pipe command to mark results for export to database table
 * Syntax: | export table=mytable fields=user,level,duration sample=1000 append=true
 * 
 * This command doesn't actually perform the export (that requires database access in the service layer),
 * but it packages the results with export metadata for the service to handle.
 */
public class ExportCommand implements PipeCommand {
    private final String tableName;
    private final List<String> fields;
    private final Integer sampleSize;
    private final boolean append;
    
    public ExportCommand(String tableName, List<String> fields, Integer sampleSize, boolean append) {
        this.tableName = tableName;
        this.fields = fields;
        this.sampleSize = sampleSize;
        this.append = append;
    }
    
    @Override
    public PipeResult execute(List<SearchResult> input) {
        // Apply sampling if requested
        List<SearchResult> results = input;
        if (sampleSize != null && sampleSize > 0 && sampleSize < input.size()) {
            results = input.subList(0, sampleSize);
        }
        
        // Create export metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("tableName", tableName);
        metadata.put("fields", fields);
        metadata.put("sampleSize", sampleSize);
        metadata.put("append", append);
        metadata.put("totalResults", input.size());
        metadata.put("exportedResults", results.size());
        
        // Return as an export result
        return new PipeResult.ExportResult(results, metadata);
    }
    
    @Override
    public String getName() {
        return "export";
    }
    
    public String getTableName() {
        return tableName;
    }
    
    public List<String> getFields() {
        return fields;
    }
    
    public Integer getSampleSize() {
        return sampleSize;
    }
    
    public boolean isAppend() {
        return append;
    }
}
