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
        TIMECHART  // Time-series chart data
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
        
        public TableResult(List<String> columns, List<Map<String, Object>> rows) {
            super(ResultType.TABLE);
            this.columns = columns;
            this.rows = rows;
        }
        
        public List<String> getColumns() {
            return columns;
        }
        
        public List<Map<String, Object>> getRows() {
            return rows;
        }
    }
    
    /**
     * Result containing chart data
     */
    public static class ChartResult extends PipeResult {
        private final String chartType; // bar, pie, line
        private final List<String> labels;
        private final Map<String, List<Number>> series;
        
        public ChartResult(String chartType, List<String> labels, Map<String, List<Number>> series) {
            super(ResultType.CHART);
            this.chartType = chartType;
            this.labels = labels;
            this.series = series;
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
    }
    
    /**
     * Result containing time-series data
     */
    public static class TimeChartResult extends PipeResult {
        private final List<String> timestamps;
        private final Map<String, List<Number>> series;
        
        public TimeChartResult(List<String> timestamps, Map<String, List<Number>> series) {
            super(ResultType.TIMECHART);
            this.timestamps = timestamps;
            this.series = series;
        }
        
        public List<String> getTimestamps() {
            return timestamps;
        }
        
        public Map<String, List<Number>> getSeries() {
            return series;
        }
    }
}
