package com.locallogsearch.core.pipe.commands;

import com.locallogsearch.core.pipe.PipeCommand;
import com.locallogsearch.core.pipe.PipeResult;
import com.locallogsearch.core.search.SearchResult;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Timechart command: creates time-series charts
 * Examples:
 *   | timechart span=1h count
 *   | timechart span=5m count by user
 */
public class TimeChartCommand implements PipeCommand {
    
    private final String span; // e.g., "1h", "5m", "1d"
    private final List<String> aggregations;
    private final String splitByField;
    
    public TimeChartCommand(String span, List<String> aggregations, String splitByField) {
        this.span = span != null ? span : "1h";
        this.aggregations = aggregations;
        this.splitByField = splitByField;
    }
    
    @Override
    public PipeResult execute(List<SearchResult> input) {
        long spanMillis = parseSpan(span);
        
        // Determine time range
        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;
        
        for (SearchResult result : input) {
            if (result.getTimestamp() != null) {
                long time = result.getTimestamp().toEpochMilli();
                minTime = Math.min(minTime, time);
                maxTime = Math.max(maxTime, time);
            }
        }
        
        if (minTime == Long.MAX_VALUE) {
            // No timestamps found
            return new PipeResult.TimeChartResult(new ArrayList<>(), new LinkedHashMap<>());
        }
        
        // Create time buckets - align to span boundaries
        long minBucket = (minTime / spanMillis) * spanMillis;
        long maxBucket = (maxTime / spanMillis) * spanMillis;
        
        List<Long> bucketTimes = new ArrayList<>();
        for (long t = minBucket; t <= maxBucket; t += spanMillis) {
            bucketTimes.add(t);
        }
        
        // Group results by time bucket and split field
        Map<String, Map<Long, List<SearchResult>>> seriesData = new LinkedHashMap<>();
        
        for (SearchResult result : input) {
            if (result.getTimestamp() == null) continue;
            
            long time = result.getTimestamp().toEpochMilli();
            long bucket = (time / spanMillis) * spanMillis;
            
            String seriesKey = splitByField != null && result.getFields().containsKey(splitByField) 
                ? result.getFields().get(splitByField) 
                : "count";
            
            seriesData.computeIfAbsent(seriesKey, k -> new LinkedHashMap<>())
                      .computeIfAbsent(bucket, k -> new ArrayList<>())
                      .add(result);
        }
        
        // Build result
        List<String> timestamps = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        for (long bucketTime : bucketTimes) {
            timestamps.add(sdf.format(new Date(bucketTime)));
        }
        
        Map<String, List<Number>> series = new LinkedHashMap<>();
        for (Map.Entry<String, Map<Long, List<SearchResult>>> entry : seriesData.entrySet()) {
            List<Number> counts = new ArrayList<>();
            for (long bucketTime : bucketTimes) {
                List<SearchResult> bucketResults = entry.getValue().getOrDefault(bucketTime, new ArrayList<>());
                
                // Compute aggregation for this bucket
                int count = bucketResults.size();
                counts.add(count);
            }
            series.put(entry.getKey(), counts);
        }
        
        return new PipeResult.TimeChartResult(timestamps, series);
    }
    
    private long parseSpan(String span) {
        // Parse span like "1h", "5m", "1d"
        if (span.endsWith("s")) {
            return Long.parseLong(span.substring(0, span.length() - 1)) * 1000;
        } else if (span.endsWith("m")) {
            return Long.parseLong(span.substring(0, span.length() - 1)) * 60 * 1000;
        } else if (span.endsWith("h")) {
            return Long.parseLong(span.substring(0, span.length() - 1)) * 60 * 60 * 1000;
        } else if (span.endsWith("d")) {
            return Long.parseLong(span.substring(0, span.length() - 1)) * 24 * 60 * 60 * 1000;
        }
        // Default to 1 hour
        return 60 * 60 * 1000;
    }
    
    @Override
    public String getName() {
        return "timechart";
    }
}
