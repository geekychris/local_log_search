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
    public PipeResult execute(List<SearchResult> input) {
        if (groupByFields.isEmpty()) {
            // No grouping - just compute aggregates over all results
            return executeSingleRow(input);
        } else {
            // Group by fields and compute aggregates per group
            return executeGrouped(input);
        }
    }
    
    private PipeResult executeSingleRow(List<SearchResult> input) {
        List<String> columns = new ArrayList<>(aggregations);
        Map<String, Object> row = new HashMap<>();
        
        for (String agg : aggregations) {
            Object value = computeAggregation(agg, input);
            row.put(agg, value);
        }
        
        return new PipeResult.TableResult(columns, Collections.singletonList(row), input.size());
    }
    
    private PipeResult executeGrouped(List<SearchResult> input) {
        // Group results by field values
        Map<String, List<SearchResult>> groups = new LinkedHashMap<>();
        
        for (SearchResult result : input) {
            String groupKey = getGroupKey(result);
            groups.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(result);
        }
        
        // Build result table
        List<String> columns = new ArrayList<>(groupByFields);
        columns.addAll(aggregations);
        
        List<Map<String, Object>> rows = new ArrayList<>();
        
        for (Map.Entry<String, List<SearchResult>> entry : groups.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            
            // Add group-by fields
            String[] keyParts = entry.getKey().split("\\|", -1);
            for (int i = 0; i < groupByFields.size() && i < keyParts.length; i++) {
                row.put(groupByFields.get(i), keyParts[i]);
            }
            
            // Add aggregations
            for (String agg : aggregations) {
                Object value = computeAggregation(agg, entry.getValue());
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
        
        return new PipeResult.TableResult(columns, rows, input.size());
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
    
    private Object computeAggregation(String agg, List<SearchResult> results) {
        // Parse aggregation function
        if (agg.equals("count")) {
            return results.size();
        } else if (agg.startsWith("avg(") && agg.endsWith(")")) {
            String field = agg.substring(4, agg.length() - 1);
            return computeAvg(field, results);
        } else if (agg.startsWith("sum(") && agg.endsWith(")")) {
            String field = agg.substring(4, agg.length() - 1);
            return computeSum(field, results);
        } else if (agg.startsWith("min(") && agg.endsWith(")")) {
            String field = agg.substring(4, agg.length() - 1);
            return computeMin(field, results);
        } else if (agg.startsWith("max(") && agg.endsWith(")")) {
            String field = agg.substring(4, agg.length() - 1);
            return computeMax(field, results);
        } else if (agg.startsWith("dc(") && agg.endsWith(")")) {
            String field = agg.substring(3, agg.length() - 1);
            return computeDistinctCount(field, results);
        }
        
        return 0;
    }
    
    private double computeAvg(String field, List<SearchResult> results) {
        double sum = 0;
        int count = 0;
        for (SearchResult result : results) {
            String value = result.getFields().get(field);
            if (value != null) {
                try {
                    // Try to parse as number, removing units like "ms"
                    String numStr = value.replaceAll("[^0-9.]", "");
                    sum += Double.parseDouble(numStr);
                    count++;
                } catch (NumberFormatException e) {
                    // Skip non-numeric values
                }
            }
        }
        return count > 0 ? sum / count : 0;
    }
    
    private double computeSum(String field, List<SearchResult> results) {
        double sum = 0;
        for (SearchResult result : results) {
            String value = result.getFields().get(field);
            if (value != null) {
                try {
                    String numStr = value.replaceAll("[^0-9.]", "");
                    sum += Double.parseDouble(numStr);
                } catch (NumberFormatException e) {
                    // Skip
                }
            }
        }
        return sum;
    }
    
    private double computeMin(String field, List<SearchResult> results) {
        double min = Double.MAX_VALUE;
        for (SearchResult result : results) {
            String value = result.getFields().get(field);
            if (value != null) {
                try {
                    String numStr = value.replaceAll("[^0-9.]", "");
                    min = Math.min(min, Double.parseDouble(numStr));
                } catch (NumberFormatException e) {
                    // Skip
                }
            }
        }
        return min == Double.MAX_VALUE ? 0 : min;
    }
    
    private double computeMax(String field, List<SearchResult> results) {
        double max = Double.MIN_VALUE;
        for (SearchResult result : results) {
            String value = result.getFields().get(field);
            if (value != null) {
                try {
                    String numStr = value.replaceAll("[^0-9.]", "");
                    max = Math.max(max, Double.parseDouble(numStr));
                } catch (NumberFormatException e) {
                    // Skip
                }
            }
        }
        return max == Double.MIN_VALUE ? 0 : max;
    }
    
    private int computeDistinctCount(String field, List<SearchResult> results) {
        Set<String> distinctValues = new HashSet<>();
        for (SearchResult result : results) {
            String value = result.getFields().get(field);
            if (value != null) {
                distinctValues.add(value);
            }
        }
        return distinctValues.size();
    }
    
    @Override
    public String getName() {
        return "stats";
    }
}
