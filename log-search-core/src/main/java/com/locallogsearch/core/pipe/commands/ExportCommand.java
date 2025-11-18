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
    
    @Override
    public String getAIDocumentation() {
        return """
## Export Command
Saves log search results to a database table for later SQL analysis. Use this when the user wants to save, persist, or run SQL queries on search results.

Syntax: `| export table=<tablename> [fields=<field1>,<field2>...] [sample=<count>] [append=true|false]`

Basic export (all fields):
- `| export table=error_logs` - Export all error results to table named 'error_logs'
- Creates new table (overwrites if exists unless append=true)

Select specific fields:
- `| export table=slow_requests fields=timestamp,endpoint,duration,user` - Only save these columns
- Reduces storage when you only need subset of data

Limit result size:
- `| export table=error_sample sample=1000` - Only export first 1000 results
- `| export table=recent_errors sample=500 fields=timestamp,message` - Sample with field selection

Append to existing table:
- `| export table=daily_stats append=true` - Add to existing data (don't replace)
- Useful for incremental log collection

When to use export:
- User says "save these results" / "put this in a table" → export
- User wants to "analyze with SQL later" → export  
- User wants to "combine multiple searches" → export with append=true
- Building reports or datasets → export

After exporting, users can query tables at the SQL Query UI.

Common patterns:
- Sample errors: `level:ERROR | export table=errors sample=10000`
- Save performance data: `status:slow | export table=perf_issues fields=timestamp,operation,duration,user`
- Daily snapshots: `timestamp:[NOW-1d TO NOW] | stats count by service | export table=daily_service_stats append=true`""";
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
