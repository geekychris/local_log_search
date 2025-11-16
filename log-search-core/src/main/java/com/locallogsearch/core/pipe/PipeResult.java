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

package com.locallogsearch.core.pipe;

import com.locallogsearch.core.search.SearchResult;
import java.util.List;
import java.util.Map;

/**
 * Base class for pipe command results
 */
public abstract class PipeResult {
    
    public enum ResultType {
        LOGS,      // Raw log entries
        TABLE,     // Tabular data
        CHART,     // Chart data
        TIMECHART, // Time-series chart data
        EXPORT     // Export to database
    }
    
    private final ResultType type;
    
    public PipeResult(ResultType type) {
        this.type = type;
    }
    
    public ResultType getType() {
        return type;
    }
    
    /**
     * Result containing raw log entries
     */
    public static class LogsResult extends PipeResult {
        private final List<SearchResult> results;
        
        public LogsResult(List<SearchResult> results) {
            super(ResultType.LOGS);
            this.results = results;
        }
        
        public List<SearchResult> getResults() {
            return results;
        }
    }
    
    /**
     * Result containing tabular data
     */
    public static class TableResult extends PipeResult {
        private final List<String> columns;
        private final List<Map<String, Object>> rows;
        private final int sourceHits;
        
        public TableResult(List<String> columns, List<Map<String, Object>> rows) {
            this(columns, rows, 0);
        }
        
        public TableResult(List<String> columns, List<Map<String, Object>> rows, int sourceHits) {
            super(ResultType.TABLE);
            this.columns = columns;
            this.rows = rows;
            this.sourceHits = sourceHits;
        }
        
        public List<String> getColumns() {
            return columns;
        }
        
        public List<Map<String, Object>> getRows() {
            return rows;
        }
        
        public int getSourceHits() {
            return sourceHits;
        }
    }
    
    /**
     * Result containing chart data
     */
    public static class ChartResult extends PipeResult {
        private final String chartType; // bar, pie, line
        private final List<String> labels;
        private final Map<String, List<Number>> series;
        private final int sourceHits;
        
        public ChartResult(String chartType, List<String> labels, Map<String, List<Number>> series) {
            this(chartType, labels, series, 0);
        }
        
        public ChartResult(String chartType, List<String> labels, Map<String, List<Number>> series, int sourceHits) {
            super(ResultType.CHART);
            this.chartType = chartType;
            this.labels = labels;
            this.series = series;
            this.sourceHits = sourceHits;
        }
        
        public String getChartType() {
            return chartType;
        }
        
        public List<String> getLabels() {
            return labels;
        }
        
        public Map<String, List<Number>> getSeries() {
            return series;
        }
        
        public int getSourceHits() {
            return sourceHits;
        }
    }
    
    /**
     * Result containing time-series data
     */
    public static class TimeChartResult extends PipeResult {
        private final List<String> timestamps;
        private final Map<String, List<Number>> series;
        private final int sourceHits;
        
        public TimeChartResult(List<String> timestamps, Map<String, List<Number>> series) {
            this(timestamps, series, 0);
        }
        
        public TimeChartResult(List<String> timestamps, Map<String, List<Number>> series, int sourceHits) {
            super(ResultType.TIMECHART);
            this.timestamps = timestamps;
            this.series = series;
            this.sourceHits = sourceHits;
        }
        
        public List<String> getTimestamps() {
            return timestamps;
        }
        
        public Map<String, List<Number>> getSeries() {
            return series;
        }
        
        public int getSourceHits() {
            return sourceHits;
        }
    }
    
    /**
     * Result containing export metadata and data to be exported
     */
    public static class ExportResult extends PipeResult {
        private final List<SearchResult> results;
        private final Map<String, Object> metadata;
        
        public ExportResult(List<SearchResult> results, Map<String, Object> metadata) {
            super(ResultType.EXPORT);
            this.results = results;
            this.metadata = metadata;
        }
        
        public List<SearchResult> getResults() {
            return results;
        }
        
        public Map<String, Object> getMetadata() {
            return metadata;
        }
    }
}
