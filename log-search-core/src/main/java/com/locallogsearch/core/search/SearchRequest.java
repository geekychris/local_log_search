package com.locallogsearch.core.search;

import java.util.List;
import java.util.Map;

public class SearchRequest {
    private List<String> indices;
    private String query;
    private int page;
    private int pageSize;
    private String sortField;
    private boolean sortDescending;
    private boolean includeFacets;
    private List<String> facetFields;
    private Long timestampFrom;
    private Long timestampTo;
    private Map<String, FacetBucketConfig> facetBuckets;
    
    public SearchRequest() {
        this.page = 0;
        this.pageSize = 50;
        this.sortDescending = false;
        this.includeFacets = true;
    }
    
    public List<String> getIndices() {
        return indices;
    }
    
    public void setIndices(List<String> indices) {
        this.indices = indices;
    }
    
    public String getQuery() {
        return query;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    public int getPage() {
        return page;
    }
    
    public void setPage(int page) {
        this.page = page;
    }
    
    public int getPageSize() {
        return pageSize;
    }
    
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
    
    public String getSortField() {
        return sortField;
    }
    
    public void setSortField(String sortField) {
        this.sortField = sortField;
    }
    
    public boolean isSortDescending() {
        return sortDescending;
    }
    
    public void setSortDescending(boolean sortDescending) {
        this.sortDescending = sortDescending;
    }
    
    public boolean isIncludeFacets() {
        return includeFacets;
    }
    
    public void setIncludeFacets(boolean includeFacets) {
        this.includeFacets = includeFacets;
    }
    
    public List<String> getFacetFields() {
        return facetFields;
    }
    
    public void setFacetFields(List<String> facetFields) {
        this.facetFields = facetFields;
    }
    
    public Long getTimestampFrom() {
        return timestampFrom;
    }
    
    public void setTimestampFrom(Long timestampFrom) {
        this.timestampFrom = timestampFrom;
    }
    
    public Long getTimestampTo() {
        return timestampTo;
    }
    
    public void setTimestampTo(Long timestampTo) {
        this.timestampTo = timestampTo;
    }
    
    public Map<String, FacetBucketConfig> getFacetBuckets() {
        return facetBuckets;
    }
    
    public void setFacetBuckets(Map<String, FacetBucketConfig> facetBuckets) {
        this.facetBuckets = facetBuckets;
    }
    
    /**
     * Configuration for bucketing a numeric facet field into ranges
     */
    public static class FacetBucketConfig {
        private List<Double> ranges;  // e.g., [0, 100, 500, 1000, 5000] creates buckets: 0-100, 100-500, 500-1000, 1000-5000, 5000+
        
        public List<Double> getRanges() {
            return ranges;
        }
        
        public void setRanges(List<Double> ranges) {
            this.ranges = ranges;
        }
    }
}
