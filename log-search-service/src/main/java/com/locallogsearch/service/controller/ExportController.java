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
import com.locallogsearch.core.search.SearchRequest;
import com.locallogsearch.core.search.SearchResponse;
import com.locallogsearch.core.search.SearchResult;
import com.locallogsearch.core.search.SearchService;
import com.locallogsearch.service.entity.ExportedRow;
import com.locallogsearch.service.entity.ExportedTable;
import com.locallogsearch.service.export.DirectTableExportService;
import com.locallogsearch.service.export.ExportService;
import org.apache.lucene.queryparser.classic.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * REST API for exporting search results to database tables
 */
@RestController
@RequestMapping("/api/export")
public class ExportController {
    private static final Logger log = LoggerFactory.getLogger(ExportController.class);
    
    private final ExportService exportService;
    private final DirectTableExportService directExportService;
    private final SearchService searchService;
    
    public ExportController(ExportService exportService, 
                           DirectTableExportService directExportService,
                           SearchService searchService) {
        this.exportService = exportService;
        this.directExportService = directExportService;
        this.searchService = searchService;
    }
    
    /**
     * Export search results directly to a table
     */
    @PostMapping("/results")
    public ResponseEntity<?> exportSearchResults(@RequestBody ExportRequest request) {
        try {
            log.info("Exporting search results: query='{}', table='{}'", request.getQuery(), request.getTableName());
            
            // Execute search to get results
            SearchRequest searchRequest = new SearchRequest();
            searchRequest.setIndices(request.getIndices());
            searchRequest.setQuery(request.getQuery());
            searchRequest.setPageSize(Integer.MAX_VALUE); // Get all results
            searchRequest.setIncludeFacets(false);
            searchRequest.setTimestampFrom(request.getTimestampFrom());
            searchRequest.setTimestampTo(request.getTimestampTo());
            
            SearchResponse searchResponse = searchService.search(searchRequest);
            
            // Handle export result from pipe command - USE DIRECT EXPORT
            if (searchResponse.getResultType() == PipeResult.ResultType.EXPORT && 
                searchResponse.getPipeResult() instanceof PipeResult.ExportResult) {
                
                PipeResult.ExportResult exportResult = (PipeResult.ExportResult) searchResponse.getPipeResult();
                Map<String, Object> metadata = exportResult.getMetadata();
                
                // Use metadata from pipe command
                String tableName = (String) metadata.get("tableName");
                List<String> fields = (List<String>) metadata.get("fields");
                Integer sampleSize = (Integer) metadata.get("sampleSize");
                Boolean append = (Boolean) metadata.get("append");
                
                // Use direct table export (creates real SQL tables)
                DirectTableExportService.DirectExportResult result = directExportService.exportToTable(
                    tableName,
                    exportResult.getResults(),
                    fields,
                    sampleSize,
                    request.getQuery(),
                    append != null ? append : true
                );
                
                return ResponseEntity.ok(result);
            }
            
            // Regular search results export - USE DIRECT EXPORT
            DirectTableExportService.DirectExportResult result = directExportService.exportToTable(
                request.getTableName(),
                searchResponse.getResults(),
                request.getFields(),
                request.getSampleSize(),
                request.getQuery(),
                request.isAppend()
            );
            
            return ResponseEntity.ok(result);
            
        } catch (ParseException e) {
            log.error("Query parse error", e);
            return ResponseEntity.badRequest().body("Invalid query: " + e.getMessage());
        } catch (IOException e) {
            log.error("Search error", e);
            return ResponseEntity.internalServerError().body("Search error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Export error", e);
            return ResponseEntity.internalServerError().body("Export error: " + e.getMessage());
        }
    }
    
    /**
     * Export stats table results to database
     */
    @PostMapping("/stats")
    public ResponseEntity<?> exportStatsTable(@RequestBody StatsExportRequest request) {
        try {
            log.info("Exporting stats table: table='{}', columns={}", 
                    request.getTableName(), request.getColumns());
            
            DirectTableExportService.DirectExportResult result = directExportService.exportStatsTable(
                request.getTableName(),
                request.getColumns(),
                request.getRows(),
                request.getFields(),
                request.getSampleSize(),
                request.getSourceQuery(),
                request.isAppend()
            );
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Stats table export error", e);
            return ResponseEntity.internalServerError().body("Export error: " + e.getMessage());
        }
    }
    
    /**
     * Export search results directly to a real SQL table
     * Creates actual tables with proper columns that can be queried with SQL
     */
    @PostMapping("/direct")
    public ResponseEntity<?> exportDirect(@RequestBody ExportRequest request) {
        try {
            log.info("Direct export: query='{}', table='{}'", request.getQuery(), request.getTableName());
            
            // Execute search to get results
            SearchRequest searchRequest = new SearchRequest();
            searchRequest.setIndices(request.getIndices());
            searchRequest.setQuery(request.getQuery());
            searchRequest.setPageSize(Integer.MAX_VALUE); // Get all results
            searchRequest.setIncludeFacets(false);
            searchRequest.setTimestampFrom(request.getTimestampFrom());
            searchRequest.setTimestampTo(request.getTimestampTo());
            
            SearchResponse searchResponse = searchService.search(searchRequest);
            
            // Use direct table export
            DirectTableExportService.DirectExportResult result = directExportService.exportToTable(
                request.getTableName(),
                searchResponse.getResults(),
                request.getFields(),
                request.getSampleSize(),
                request.getQuery(),
                request.isAppend()
            );
            
            return ResponseEntity.ok(result);
            
        } catch (ParseException e) {
            log.error("Query parse error", e);
            return ResponseEntity.badRequest().body("Invalid query: " + e.getMessage());
        } catch (IOException e) {
            log.error("Search error", e);
            return ResponseEntity.internalServerError().body("Search error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Export error", e);
            return ResponseEntity.internalServerError().body("Export error: " + e.getMessage());
        }
    }
    
    /**
     * List all exported tables
     */
    @GetMapping("/tables")
    public ResponseEntity<List<ExportedTable>> listTables() {
        try {
            List<ExportedTable> tables = exportService.listTables();
            return ResponseEntity.ok(tables);
        } catch (Exception e) {
            log.error("Error listing tables", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get metadata for a specific table
     */
    @GetMapping("/tables/{tableName}")
    public ResponseEntity<?> getTable(@PathVariable String tableName) {
        try {
            return exportService.getTable(tableName)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error getting table metadata", e);
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * Query rows from an exported table
     */
    @GetMapping("/tables/{tableName}/rows")
    public ResponseEntity<?> queryTable(
            @PathVariable String tableName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ExportedRow> rows = exportService.queryTable(tableName, pageable);
            return ResponseEntity.ok(rows);
        } catch (Exception e) {
            log.error("Error querying table", e);
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * Delete an exported table
     */
    @DeleteMapping("/tables/{tableName}")
    public ResponseEntity<?> deleteTable(@PathVariable String tableName) {
        try {
            exportService.deleteTable(tableName);
            return ResponseEntity.ok().body("Table deleted: " + tableName);
        } catch (Exception e) {
            log.error("Error deleting table", e);
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * Request to export search results
     */
    public static class ExportRequest {
        private List<String> indices;
        private String query;
        private String tableName;
        private List<String> fields;
        private Integer sampleSize;
        private boolean append = true;
        private Long timestampFrom;
        private Long timestampTo;
        
        // Getters and Setters
        
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
        
        public String getTableName() {
            return tableName;
        }
        
        public void setTableName(String tableName) {
            this.tableName = tableName;
        }
        
        public List<String> getFields() {
            return fields;
        }
        
        public void setFields(List<String> fields) {
            this.fields = fields;
        }
        
        public Integer getSampleSize() {
            return sampleSize;
        }
        
        public void setSampleSize(Integer sampleSize) {
            this.sampleSize = sampleSize;
        }
        
        public boolean isAppend() {
            return append;
        }
        
        public void setAppend(boolean append) {
            this.append = append;
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
    }
    
    /**
     * Request to export stats table results
     */
    public static class StatsExportRequest {
        private String tableName;
        private List<String> columns;
        private List<Map<String, Object>> rows;
        private List<String> fields;
        private Integer sampleSize;
        private String sourceQuery;
        private boolean append = false;
        
        // Getters and Setters
        
        public String getTableName() {
            return tableName;
        }
        
        public void setTableName(String tableName) {
            this.tableName = tableName;
        }
        
        public List<String> getColumns() {
            return columns;
        }
        
        public void setColumns(List<String> columns) {
            this.columns = columns;
        }
        
        public List<Map<String, Object>> getRows() {
            return rows;
        }
        
        public void setRows(List<Map<String, Object>> rows) {
            this.rows = rows;
        }
        
        public List<String> getFields() {
            return fields;
        }
        
        public void setFields(List<String> fields) {
            this.fields = fields;
        }
        
        public Integer getSampleSize() {
            return sampleSize;
        }
        
        public void setSampleSize(Integer sampleSize) {
            this.sampleSize = sampleSize;
        }
        
        public String getSourceQuery() {
            return sourceQuery;
        }
        
        public void setSourceQuery(String sourceQuery) {
            this.sourceQuery = sourceQuery;
        }
        
        public boolean isAppend() {
            return append;
        }
        
        public void setAppend(boolean append) {
            this.append = append;
        }
    }
}
