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
    public PipeResult execute(Iterator<SearchResult> input, int totalHits) {
        // Collect results with optional sampling
        List<SearchResult> results = new ArrayList<>();
        int limit = sampleSize != null && sampleSize > 0 ? sampleSize : Integer.MAX_VALUE;
        
        while (input.hasNext() && results.size() < limit) {
            results.add(input.next());
        }
        
        // Create export metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("tableName", tableName);
        metadata.put("fields", fields);
        metadata.put("sampleSize", sampleSize);
        metadata.put("append", append);
        metadata.put("totalResults", totalHits);
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
