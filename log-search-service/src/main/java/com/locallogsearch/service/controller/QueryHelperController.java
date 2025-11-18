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

import com.locallogsearch.core.search.SearchResult;
import com.locallogsearch.service.ai.QueryHelperService;
import com.locallogsearch.service.ai.SqlHelperService;
import com.locallogsearch.service.ai.SummarizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class QueryHelperController {
    
    private final QueryHelperService queryHelperService;
    private final SqlHelperService sqlHelperService;
    private final SummarizationService summarizationService;
    
    public QueryHelperController(QueryHelperService queryHelperService, 
                                 SqlHelperService sqlHelperService,
                                 SummarizationService summarizationService) {
        this.queryHelperService = queryHelperService;
        this.sqlHelperService = sqlHelperService;
        this.summarizationService = summarizationService;
    }
    
    @PostMapping("/help")
    public ResponseEntity<QueryHelpResponse> helpWithQuery(@RequestBody QueryHelpRequest request) {
        try {
            String suggestion = queryHelperService.helpWithQuery(
                request.getUserRequest(), 
                request.getCurrentQuery()
            );
            
            return ResponseEntity.ok(new QueryHelpResponse(suggestion));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(new QueryHelpResponse("Error: " + e.getMessage()));
        }
    }
    
    @PostMapping("/sql-help")
    public ResponseEntity<QueryHelpResponse> helpWithSql(@RequestBody SqlHelpRequest request) {
        try {
            String suggestion = sqlHelperService.helpWithSql(
                request.getUserRequest(),
                request.getCurrentQuery(),
                request.getTableSchemas()
            );
            
            return ResponseEntity.ok(new QueryHelpResponse(suggestion));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(new QueryHelpResponse("Error: " + e.getMessage()));
        }
    }
    
    @PostMapping("/summarize")
    public ResponseEntity<SummarizationResponse> summarizeLogs(@RequestBody SummarizationRequest request) {
        try {
            SummarizationService.SummaryType summaryType = 
                SummarizationService.SummaryType.valueOf(request.getSummaryType());
            
            String summary = summarizationService.summarizeLogs(
                request.getResults(),
                summaryType,
                request.getQuery()
            );
            
            return ResponseEntity.ok(new SummarizationResponse(
                summary,
                request.getResults().size(),
                summarizationService.getMaxLogsForSummary()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(new SummarizationResponse(
                    "Invalid summary type: " + request.getSummaryType(), 0, 0
                ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(new SummarizationResponse(
                    "Error: " + e.getMessage(), 0, 0
                ));
        }
    }
    
    public static class QueryHelpRequest {
        private String userRequest;
        private String currentQuery;
        
        public String getUserRequest() {
            return userRequest;
        }
        
        public void setUserRequest(String userRequest) {
            this.userRequest = userRequest;
        }
        
        public String getCurrentQuery() {
            return currentQuery;
        }
        
        public void setCurrentQuery(String currentQuery) {
            this.currentQuery = currentQuery;
        }
    }
    
    public static class QueryHelpResponse {
        private String suggestion;
        
        public QueryHelpResponse(String suggestion) {
            this.suggestion = suggestion;
        }
        
        public String getSuggestion() {
            return suggestion;
        }
        
        public void setSuggestion(String suggestion) {
            this.suggestion = suggestion;
        }
    }
    
    public static class SqlHelpRequest {
        private String userRequest;
        private String currentQuery;
        private List<Map<String, Object>> tableSchemas;
        
        public String getUserRequest() {
            return userRequest;
        }
        
        public void setUserRequest(String userRequest) {
            this.userRequest = userRequest;
        }
        
        public String getCurrentQuery() {
            return currentQuery;
        }
        
        public void setCurrentQuery(String currentQuery) {
            this.currentQuery = currentQuery;
        }
        
        public List<Map<String, Object>> getTableSchemas() {
            return tableSchemas;
        }
        
        public void setTableSchemas(List<Map<String, Object>> tableSchemas) {
            this.tableSchemas = tableSchemas;
        }
    }
    
    public static class SummarizationRequest {
        private List<SearchResult> results;
        private String summaryType;
        private String query;
        
        public List<SearchResult> getResults() {
            return results;
        }
        
        public void setResults(List<SearchResult> results) {
            this.results = results;
        }
        
        public String getSummaryType() {
            return summaryType;
        }
        
        public void setSummaryType(String summaryType) {
            this.summaryType = summaryType;
        }
        
        public String getQuery() {
            return query;
        }
        
        public void setQuery(String query) {
            this.query = query;
        }
    }
    
    public static class SummarizationResponse {
        private String summary;
        private int totalResults;
        private int maxAnalyzed;
        
        public SummarizationResponse(String summary, int totalResults, int maxAnalyzed) {
            this.summary = summary;
            this.totalResults = totalResults;
            this.maxAnalyzed = maxAnalyzed;
        }
        
        public String getSummary() {
            return summary;
        }
        
        public void setSummary(String summary) {
            this.summary = summary;
        }
        
        public int getTotalResults() {
            return totalResults;
        }
        
        public void setTotalResults(int totalResults) {
            this.totalResults = totalResults;
        }
        
        public int getMaxAnalyzed() {
            return maxAnalyzed;
        }
        
        public void setMaxAnalyzed(int maxAnalyzed) {
            this.maxAnalyzed = maxAnalyzed;
        }
    }
}
