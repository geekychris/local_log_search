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

package com.locallogsearch.service.controller;

import com.locallogsearch.core.pipe.PipeResult;
import com.locallogsearch.core.search.*;
import com.locallogsearch.core.search.SearchRequest;
import org.apache.lucene.queryparser.classic.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {
    private static final Logger log = LoggerFactory.getLogger(SearchController.class);
    
    private final SearchService searchService;
    
    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }
    
    @PostMapping
    public ResponseEntity<?> search(@RequestBody ApiSearchRequest apiRequest) {
        try {
            if (apiRequest.getQuery() == null || apiRequest.getQuery().isEmpty()) {
                return ResponseEntity.badRequest().body("Query cannot be empty");
            }
            
            if (apiRequest.getIndices() == null || apiRequest.getIndices().isEmpty()) {
                return ResponseEntity.badRequest().body("At least one index must be specified");
            }
            
            // Convert API request to core SearchRequest
            SearchRequest request = new SearchRequest();
            request.setIndices(apiRequest.getIndices());
            request.setQuery(apiRequest.getQuery());
            request.setPage(apiRequest.getPage() >= 0 ? apiRequest.getPage() : 0);
            request.setPageSize(apiRequest.getPageSize() > 0 ? apiRequest.getPageSize() : 50);
            request.setSortField(apiRequest.getSortField());
            request.setSortDescending(apiRequest.isSortDescending());
            request.setIncludeFacets(apiRequest.isIncludeFacets());
            request.setTimestampFrom(apiRequest.getTimestampFrom());
            request.setTimestampTo(apiRequest.getTimestampTo());
            request.setFacetBuckets(apiRequest.getFacetBuckets());
            
            log.info("Search request - timestampFrom: {}, timestampTo: {}", apiRequest.getTimestampFrom(), apiRequest.getTimestampTo());
            
            SearchResponse response = searchService.search(request);
            
            return ResponseEntity.ok(new ApiSearchResponse(response));
            
        } catch (ParseException e) {
            log.error("Query parse error", e);
            return ResponseEntity.badRequest().body("Invalid query: " + e.getMessage());
        } catch (IOException e) {
            log.error("Search error", e);
            return ResponseEntity.internalServerError().body("Search error: " + e.getMessage());
        }
    }
    
    public static class ApiSearchRequest {
        private List<String> indices;
        private String query;
        private int page = 0;
        private int pageSize = 50;
        private String sortField;
        private boolean sortDescending = false;
        private boolean includeFacets = true;
        private int maxResults = 50; // For backward compatibility
        private Long timestampFrom;
        private Long timestampTo;
        private java.util.Map<String, SearchRequest.FacetBucketConfig> facetBuckets;
        
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
        
        public int getMaxResults() {
            return maxResults;
        }
        
        public void setMaxResults(int maxResults) {
            this.maxResults = maxResults;
            this.pageSize = maxResults; // Sync with pageSize
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
        
        public java.util.Map<String, SearchRequest.FacetBucketConfig> getFacetBuckets() {
            return facetBuckets;
        }
        
        public void setFacetBuckets(java.util.Map<String, SearchRequest.FacetBucketConfig> facetBuckets) {
            this.facetBuckets = facetBuckets;
        }
    }
    
    public static class ApiSearchResponse {
        private List<SearchResult> results;
        private int totalHits;  // Original query hits
        private Integer filteredHits;  // Hits after filtering (null if no filter used)
        private int page;
        private int pageSize;
        private int totalPages;
        private java.util.Map<String, java.util.Map<String, Integer>> facets;
        private String resultType;
        private Object pipeResult;
        
        public ApiSearchResponse(SearchResponse response) {
            this.resultType = response.getResultType().name();
            
            // Check if this is a pipe result
            if (response.getPipeResult() != null) {
                this.pipeResult = response.getPipeResult();
                // For LOGS pipe results, keep the results populated
                if (response.getResultType() == PipeResult.ResultType.LOGS) {
                    this.results = response.getResults();
                    this.totalHits = response.getTotalHits();
                    this.filteredHits = response.getFilteredHits();
                    this.page = response.getPage();
                    this.pageSize = response.getPageSize();
                    this.totalPages = response.getTotalPages();
                    this.facets = response.getFacets();
                } else {
                    // For TABLE/CHART results, initialize regular fields as empty
                    this.results = new java.util.ArrayList<>();
                    this.totalHits = 0;
                    this.page = 0;
                    this.pageSize = 0;
                    this.totalPages = 0;
                    this.facets = new java.util.HashMap<>();
                }
            } else {
                // Regular log results
                this.results = response.getResults();
                this.totalHits = response.getTotalHits();
                this.filteredHits = null;  // No filtering for regular queries
                this.page = response.getPage();
                this.pageSize = response.getPageSize();
                this.totalPages = response.getTotalPages();
                this.facets = response.getFacets();
                this.pipeResult = null;
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
            return totalPages;
        }
        
        public void setTotalPages(int totalPages) {
            this.totalPages = totalPages;
        }
        
        public java.util.Map<String, java.util.Map<String, Integer>> getFacets() {
            return facets;
        }
        
        public void setFacets(java.util.Map<String, java.util.Map<String, Integer>> facets) {
            this.facets = facets;
        }
        
        public String getResultType() {
            return resultType;
        }
        
        public void setResultType(String resultType) {
            this.resultType = resultType;
        }
        
        public Object getPipeResult() {
            return pipeResult;
        }
        
        public void setPipeResult(Object pipeResult) {
            this.pipeResult = pipeResult;
        }
        
        public Integer getFilteredHits() {
            return filteredHits;
        }
        
        public void setFilteredHits(Integer filteredHits) {
            this.filteredHits = filteredHits;
        }
    }
}
