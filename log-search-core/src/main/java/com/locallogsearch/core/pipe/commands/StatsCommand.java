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
 * Stats command: aggregates results
 * Examples:
 *   | stats count
 *   | stats count by user
 *   | stats count avg(duration) by operation
 *   | stats sum(duration) by user
 */
public class StatsCommand implements PipeCommand {
    
    private final List<String> aggregations;
    private final List<String> groupByFields;
    
    public StatsCommand(List<String> aggregations, List<String> groupByFields) {
        this.aggregations = aggregations;
        this.groupByFields = groupByFields;
    }
    
    @Override
    public PipeResult execute(Iterator<SearchResult> input, int totalHits) {
        if (groupByFields.isEmpty()) {
            // No grouping - just compute aggregates over all results
            return executeSingleRow(input, totalHits);
        } else {
            // Group by fields and compute aggregates per group
            return executeGrouped(input, totalHits);
        }
    }
    
    private PipeResult executeSingleRow(Iterator<SearchResult> input, int totalHits) {
        List<String> columns = new ArrayList<>(aggregations);
        Map<String, Object> row = new HashMap<>();
        
        // Initialize aggregation accumulators
        Map<String, AggregationAccumulator> accumulators = new HashMap<>();
        for (String agg : aggregations) {
            accumulators.put(agg, new AggregationAccumulator(agg));
        }
        
        // Stream through results once, updating accumulators
        while (input.hasNext()) {
            SearchResult result = input.next();
            for (AggregationAccumulator acc : accumulators.values()) {
                acc.add(result);
            }
        }
        
        // Compute final values
        for (String agg : aggregations) {
            Object value = accumulators.get(agg).getValue();
            row.put(agg, value);
        }
        
        return new PipeResult.TableResult(columns, Collections.singletonList(row), totalHits);
    }
    
    private PipeResult executeGrouped(Iterator<SearchResult> input, int totalHits) {
        // Group results by field values, accumulating aggregations
        Map<String, Map<String, AggregationAccumulator>> groups = new LinkedHashMap<>();
        
        // Stream through results once, updating group accumulators
        while (input.hasNext()) {
            SearchResult result = input.next();
            String groupKey = getGroupKey(result);
            
            Map<String, AggregationAccumulator> groupAccumulators = groups.get(groupKey);
            if (groupAccumulators == null) {
                groupAccumulators = new HashMap<>();
                for (String agg : aggregations) {
                    groupAccumulators.put(agg, new AggregationAccumulator(agg));
                }
                groups.put(groupKey, groupAccumulators);
            }
            
            for (AggregationAccumulator acc : groupAccumulators.values()) {
                acc.add(result);
            }
        }
        
        // Build result table
        List<String> columns = new ArrayList<>(groupByFields);
        columns.addAll(aggregations);
        
        List<Map<String, Object>> rows = new ArrayList<>();
        
        for (Map.Entry<String, Map<String, AggregationAccumulator>> entry : groups.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            
            // Add group-by fields
            String[] keyParts = entry.getKey().split("\\\\|", -1);
            for (int i = 0; i < groupByFields.size() && i < keyParts.length; i++) {
                row.put(groupByFields.get(i), keyParts[i]);
            }
            
            // Add aggregations
            for (String agg : aggregations) {
                Object value = entry.getValue().get(agg).getValue();
                row.put(agg, value);
            }
            
            rows.add(row);
        }
        
        // Sort by first column descending (typically count)
        if (!aggregations.isEmpty()) {
            final String sortCol = aggregations.get(0);
            rows.sort((a, b) -> {
                Object valA = a.get(sortCol);
                Object valB = b.get(sortCol);
                if (valA instanceof Number && valB instanceof Number) {
                    return Double.compare(((Number) valB).doubleValue(), ((Number) valA).doubleValue());
                }
                return 0;
            });
        }
        
        return new PipeResult.TableResult(columns, rows, totalHits);
    }
    
    private String getGroupKey(SearchResult result) {
        StringBuilder key = new StringBuilder();
        for (String field : groupByFields) {
            if (key.length() > 0) key.append("|");
            String value = result.getFields().getOrDefault(field, "");
            key.append(value);
        }
        return key.toString();
    }
    
    /**
     * Accumulator for streaming aggregation calculations.
     * Processes results one at a time without storing them all in memory.
     */
    private static class AggregationAccumulator {
        private final String aggregation;
        private final String field;
        private final String function;
        
        private long count = 0;
        private double sum = 0;
        private double min = Double.MAX_VALUE;
        private double max = Double.MIN_VALUE;
        private Set<String> distinctValues;
        
        public AggregationAccumulator(String aggregation) {
            this.aggregation = aggregation;
            
            // Parse aggregation function and field
            if (aggregation.equals("count")) {
                this.function = "count";
                this.field = null;
            } else if (aggregation.startsWith("avg(") && aggregation.endsWith(")")) {
                this.function = "avg";
                this.field = aggregation.substring(4, aggregation.length() - 1);
            } else if (aggregation.startsWith("sum(") && aggregation.endsWith(")")) {
                this.function = "sum";
                this.field = aggregation.substring(4, aggregation.length() - 1);
            } else if (aggregation.startsWith("min(") && aggregation.endsWith(")")) {
                this.function = "min";
                this.field = aggregation.substring(4, aggregation.length() - 1);
            } else if (aggregation.startsWith("max(") && aggregation.endsWith(")")) {
                this.function = "max";
                this.field = aggregation.substring(4, aggregation.length() - 1);
            } else if (aggregation.startsWith("dc(") && aggregation.endsWith(")")) {
                this.function = "dc";
                this.field = aggregation.substring(3, aggregation.length() - 1);
                this.distinctValues = new HashSet<>();
            } else {
                this.function = "unknown";
                this.field = null;
            }
        }
        
        public void add(SearchResult result) {
            count++;
            
            if (field == null) return; // count only
            
            String value = result.getFields().get(field);
            if (value == null) return;
            
            switch (function) {
                case "avg":
                case "sum":
                case "min":
                case "max":
                    try {
                        String numStr = value.replaceAll("[^0-9.\\-]", "");
                        double numValue = Double.parseDouble(numStr);
                        sum += numValue;
                        min = Math.min(min, numValue);
                        max = Math.max(max, numValue);
                    } catch (NumberFormatException e) {
                        // Skip non-numeric
                    }
                    break;
                case "dc":
                    distinctValues.add(value);
                    break;
            }
        }
        
        public Object getValue() {
            switch (function) {
                case "count":
                    return (int) count;
                case "avg":
                    return count > 0 ? sum / count : 0.0;
                case "sum":
                    return sum;
                case "min":
                    return min == Double.MAX_VALUE ? 0.0 : min;
                case "max":
                    return max == Double.MIN_VALUE ? 0.0 : max;
                case "dc":
                    return distinctValues != null ? distinctValues.size() : 0;
                default:
                    return 0;
            }
        }
    }
    
    @Override
    public String getName() {
        return "stats";
    }
}
