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

import com.locallogsearch.service.ai.QueryHelperService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class QueryHelperController {
    
    private final QueryHelperService queryHelperService;
    
    public QueryHelperController(QueryHelperService queryHelperService) {
        this.queryHelperService = queryHelperService;
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
}
