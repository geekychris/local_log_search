package com.locallogsearch.core.search;

import com.locallogsearch.core.pipe.PipeResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchResponse {
    private List<SearchResult> results;
    private int totalHits;
    private int page;
    private int pageSize;
    private Map<String, Map<String, Integer>> facets;
    private Integer facetSampleSize; // Number of docs used for facet calculation (may be less than totalHits)
    
    // Pipe result fields
    private PipeResult.ResultType resultType;
    private PipeResult pipeResult;
    
    public SearchResponse(List<SearchResult> results, int totalHits) {
        this.results = results;
        this.totalHits = totalHits;
        this.page = 0;
        this.pageSize = results.size();
        this.facets = new HashMap<>();
    }
    
    public SearchResponse(List<SearchResult> results, int totalHits, int page, int pageSize, Map<String, Map<String, Integer>> facets) {
        this.results = results;
        this.totalHits = totalHits;
        this.page = page;
        this.pageSize = pageSize;
        this.facets = facets != null ? facets : new HashMap<>();
        this.resultType = PipeResult.ResultType.LOGS;
        this.pipeResult = null;
    }
    
    // Constructor for pipe results
    public SearchResponse(PipeResult pipeResult) {
        this.pipeResult = pipeResult;
        this.resultType = pipeResult.getType();
        this.results = new ArrayList<>();
        this.totalHits = 0;
        this.page = 0;
        this.pageSize = 0;
        this.facets = new HashMap<>();
    }
    
    public List<SearchResult> getResults() {
        return results;
    }
    
    public void setResults(List<SearchResult> results) {
        this.results = results;
    }
    
    public int getTotalHits() {
        return totalHits;
    }
    
    public void setTotalHits(int totalHits) {
        this.totalHits = totalHits;
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
    
    public int getTotalPages() {
        return (int) Math.ceil((double) totalHits / pageSize);
    }
    
    public Map<String, Map<String, Integer>> getFacets() {
        return facets;
    }
    
    public void setFacets(Map<String, Map<String, Integer>> facets) {
        this.facets = facets;
    }
    
    public Integer getFacetSampleSize() {
        return facetSampleSize;
    }
    
    public void setFacetSampleSize(Integer facetSampleSize) {
        this.facetSampleSize = facetSampleSize;
    }
    
    public PipeResult.ResultType getResultType() {
        return resultType != null ? resultType : PipeResult.ResultType.LOGS;
    }
    
    public void setResultType(PipeResult.ResultType resultType) {
        this.resultType = resultType;
    }
    
    public PipeResult getPipeResult() {
        return pipeResult;
    }
    
    public void setPipeResult(PipeResult pipeResult) {
        this.pipeResult = pipeResult;
    }
}
