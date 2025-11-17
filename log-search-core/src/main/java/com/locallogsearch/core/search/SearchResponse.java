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

package com.locallogsearch.core.search;

import com.locallogsearch.core.pipe.PipeResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchResponse {
    private List<SearchResult> results;
    private int totalHits;  // Original query hits before filtering
    private int filteredHits; // Hits after filter pipe commands (if filter was used)
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
        this.facets = new HashMap<>();
        
        // If the result is logs, populate the results list
        if (pipeResult instanceof PipeResult.LogsResult) {
            PipeResult.LogsResult logsResult = (PipeResult.LogsResult) pipeResult;
            this.results = logsResult.getResults();
            this.filteredHits = this.results.size();
            this.totalHits = this.results.size(); // Will be overridden by searchWithPipes if needed
            this.page = 0;
            this.pageSize = this.results.size();
        } else {
            this.results = new ArrayList<>();
            this.totalHits = 0;
            this.filteredHits = 0;
            this.page = 0;
            this.pageSize = 0;
        }
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
    
    public int getFilteredHits() {
        return filteredHits;
    }
    
    public void setFilteredHits(int filteredHits) {
        this.filteredHits = filteredHits;
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
