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
 * Chart command: converts stats results to chart format
 * Examples:
 *   | chart count by user
 *   | chart sum(duration) by operation
 */
public class ChartCommand implements PipeCommand {
    
    private final String chartType; // bar, pie, line
    private final List<String> aggregations;
    private final List<String> groupByFields;
    
    public ChartCommand(String chartType, List<String> aggregations, List<String> groupByFields) {
        this.chartType = chartType != null ? chartType : "bar";
        this.aggregations = aggregations;
        this.groupByFields = groupByFields;
    }
    
    @Override
    public PipeResult execute(Iterator<SearchResult> input, int totalHits) {
        // First compute stats
        StatsCommand statsCmd = new StatsCommand(aggregations, groupByFields);
        PipeResult statsResult = statsCmd.execute(input, totalHits);
        
        // Convert table result to chart result
        if (statsResult instanceof PipeResult.TableResult) {
            PipeResult.TableResult tableResult = (PipeResult.TableResult) statsResult;
            return convertToChart(tableResult);
        }
        
        return statsResult;
    }
    
    /**
     * Execute chart command on pre-computed table result.
     * This allows chaining: stats | filter | chart
     */
    public PipeResult executeOnTable(PipeResult.TableResult tableResult) {
        return convertToChart(tableResult);
    }
    
    private PipeResult convertToChart(PipeResult.TableResult tableResult) {
        List<String> labels = new ArrayList<>();
        Map<String, List<Number>> series = new LinkedHashMap<>();
        
        // Extract labels from first groupBy field
        String labelField = !groupByFields.isEmpty() ? groupByFields.get(0) : tableResult.getColumns().get(0);
        
        // Extract series data from aggregations
        for (String agg : aggregations) {
            series.put(agg, new ArrayList<>());
        }
        
        for (Map<String, Object> row : tableResult.getRows()) {
            // Get label
            Object labelValue = row.get(labelField);
            labels.add(labelValue != null ? labelValue.toString() : "");
            
            // Get values for each aggregation
            for (String agg : aggregations) {
                Object value = row.get(agg);
                Number numValue = value instanceof Number ? (Number) value : 0;
                series.get(agg).add(numValue);
            }
        }
        
        return new PipeResult.ChartResult(chartType, labels, series, tableResult.getSourceHits());
    }
    
    @Override
    public String getName() {
        return "chart";
    }
    
    @Override
    public String getAIDocumentation() {
        return """
## Chart Command
Creates visual charts from log data. Use this instead of stats when the user asks for graphs, charts, or visualizations.

Syntax: `| chart [type=<charttype>] <function> by <field>`

Bar charts (default):
- `| chart count by user` - Bar chart showing event count per user
- `| chart type=bar avg(duration) by operation` - Bar chart of average duration per operation
- `| chart sum(bytes) by endpoint` - Total bytes per endpoint as bars

Pie charts (for distribution/proportions):
- `| chart type=pie count by status` - Show proportion of each status code
- `| chart type=pie count by level` - Distribution of ERROR/WARN/INFO logs
- `| chart type=pie sum(amount) by category` - Financial breakdown by category

Line charts (for trends):
- `| chart type=line count by hour` - Trend line of events over hours
- `| chart type=line avg(duration) by service` - Performance comparison across services

When to use chart vs stats:
- User asks "show me" / "visualize" / "graph" → use chart
- User asks "how many" / "count" / "list" → use stats
- Chart is stats + visualization in one command

Common patterns:
- `error | chart type=pie count by component` - Which components have most errors (pie)
- `status:[400 TO 499] | chart count by status` - Client error distribution (bar)
- `slow | chart type=bar avg(duration) by user` - Average slowness per user (bar)""";
    }
}
