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
    public PipeResult execute(List<SearchResult> input) {
        // First compute stats
        StatsCommand statsCmd = new StatsCommand(aggregations, groupByFields);
        PipeResult statsResult = statsCmd.execute(input);
        
        // Convert table result to chart result
        if (statsResult instanceof PipeResult.TableResult) {
            PipeResult.TableResult tableResult = (PipeResult.TableResult) statsResult;
            return convertToChart(tableResult);
        }
        
        return statsResult;
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
        
        return new PipeResult.ChartResult(chartType, labels, series);
    }
    
    @Override
    public String getName() {
        return "chart";
    }
}
