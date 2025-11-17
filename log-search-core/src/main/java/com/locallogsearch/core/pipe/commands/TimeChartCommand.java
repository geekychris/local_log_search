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
    public PipeResult execute(Iterator<SearchResult> input, int totalHits) {
        long spanMillis = parseSpan(span);
        
        // Determine time range and group results by time bucket in one pass
        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;
        
        // Group results by time bucket and split field - count only to save memory
        Map<String, Map<Long, Integer>> seriesData = new LinkedHashMap<>();
        
        while (input.hasNext()) {
            SearchResult result = input.next();
            if (result.getTimestamp() == null) continue;
            
            long time = result.getTimestamp().toEpochMilli();
            minTime = Math.min(minTime, time);
            maxTime = Math.max(maxTime, time);
            
            long bucket = (time / spanMillis) * spanMillis;
            
            String seriesKey = splitByField != null && result.getFields().containsKey(splitByField) 
                ? result.getFields().get(splitByField) 
                : "count";
            
            seriesData.computeIfAbsent(seriesKey, k -> new LinkedHashMap<>())
                      .merge(bucket, 1, Integer::sum);
        }
        
        if (minTime == Long.MAX_VALUE) {
            // No timestamps found
            return new PipeResult.TimeChartResult(new ArrayList<>(), new LinkedHashMap<>(), totalHits);
        }
        
        // Create time buckets - align to span boundaries
        long minBucket = (minTime / spanMillis) * spanMillis;
        long maxBucket = (maxTime / spanMillis) * spanMillis;
        
        List<Long> bucketTimes = new ArrayList<>();
        for (long t = minBucket; t <= maxBucket; t += spanMillis) {
            bucketTimes.add(t);
        }
        
        // Build result
        List<String> timestamps = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        for (long bucketTime : bucketTimes) {
            timestamps.add(sdf.format(new Date(bucketTime)));
        }
        
        Map<String, List<Number>> series = new LinkedHashMap<>();
        for (Map.Entry<String, Map<Long, Integer>> entry : seriesData.entrySet()) {
            List<Number> counts = new ArrayList<>();
            for (long bucketTime : bucketTimes) {
                int count = entry.getValue().getOrDefault(bucketTime, 0);
                counts.add(count);
            }
            series.put(entry.getKey(), counts);
        }
        
        return new PipeResult.TimeChartResult(timestamps, series, totalHits);
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
