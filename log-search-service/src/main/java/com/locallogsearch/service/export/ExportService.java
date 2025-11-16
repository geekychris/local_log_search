package com.locallogsearch.service.export;

import com.locallogsearch.core.search.SearchResult;
import com.locallogsearch.service.entity.ExportedRow;
import com.locallogsearch.service.entity.ExportedTable;
import com.locallogsearch.service.repository.ExportedRowRepository;
import com.locallogsearch.service.repository.ExportedTableRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for exporting search results to database tables
 */
@Service
public class ExportService {
    private static final Logger log = LoggerFactory.getLogger(ExportService.class);
    
    private final ExportedTableRepository tableRepository;
    private final ExportedRowRepository rowRepository;
    
    public ExportService(ExportedTableRepository tableRepository, ExportedRowRepository rowRepository) {
        this.tableRepository = tableRepository;
        this.rowRepository = rowRepository;
    }
    
    /**
     * Export search results to a database table
     * 
     * @param tableName Name of the table to create/append to
     * @param results Search results to export
     * @param columns Columns to export (null = all fields)
     * @param sampleSize Maximum number of rows to export (null = all)
     * @param sourceQuery Original query that produced these results
     * @param append If true, append to existing table; if false, replace
     * @return Export statistics
     */
    @Transactional
    public ExportResult exportResults(String tableName, List<SearchResult> results, 
                                     List<String> columns, Integer sampleSize, 
                                     String sourceQuery, boolean append) {
        log.info("Exporting {} results to table '{}' (columns: {}, sample: {}, append: {})", 
                 results.size(), tableName, columns, sampleSize, append);
        
        // Validate table name
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new IllegalArgumentException("Table name cannot be empty");
        }
        tableName = tableName.trim().toLowerCase();
        
        // Determine which results to export
        List<SearchResult> toExport = results;
        if (sampleSize != null && sampleSize > 0 && sampleSize < results.size()) {
            toExport = results.subList(0, sampleSize);
        }
        
        // Determine columns
        Set<String> columnSet = determineColumns(toExport, columns);
        List<String> columnList = new ArrayList<>(columnSet);
        Collections.sort(columnList); // Consistent ordering
        
        // Handle existing table
        ExportedTable table;
        if (!append && tableRepository.existsByTableName(tableName)) {
            log.info("Deleting existing table '{}' (append=false)", tableName);
            deleteTable(tableName);
        }
        
        Optional<ExportedTable> existingTable = tableRepository.findByTableName(tableName);
        if (existingTable.isPresent()) {
            table = existingTable.get();
            // Merge columns if appending
            Set<String> mergedColumns = new LinkedHashSet<>(table.getColumns());
            mergedColumns.addAll(columnList);
            table.setColumns(new ArrayList<>(mergedColumns));
        } else {
            table = new ExportedTable(tableName, columnList);
            table.setSourceQuery(sourceQuery);
        }
        
        // Export rows
        int rowsExported = 0;
        for (SearchResult result : toExport) {
            Map<String, String> rowData = extractRowData(result, columnSet);
            ExportedRow row = new ExportedRow(tableName, rowData, result.getTimestamp());
            rowRepository.save(row);
            rowsExported++;
        }
        
        // Update table metadata
        table.setRowCount(rowRepository.countByTableName(tableName));
        tableRepository.save(table);
        
        log.info("Exported {} rows to table '{}' (total rows now: {})", 
                 rowsExported, tableName, table.getRowCount());
        
        return new ExportResult(tableName, rowsExported, table.getRowCount(), columnList);
    }
    
    /**
     * List all exported tables
     */
    public List<ExportedTable> listTables() {
        return tableRepository.findAll();
    }
    
    /**
     * Get metadata for a specific table
     */
    public Optional<ExportedTable> getTable(String tableName) {
        return tableRepository.findByTableName(tableName);
    }
    
    /**
     * Query rows from an exported table
     */
    public Page<ExportedRow> queryTable(String tableName, Pageable pageable) {
        return rowRepository.findByTableName(tableName, pageable);
    }
    
    /**
     * Get all rows from an exported table (use with caution for large tables)
     */
    public List<ExportedRow> getAllRows(String tableName) {
        return rowRepository.findByTableName(tableName);
    }
    
    /**
     * Delete an exported table and all its rows
     */
    @Transactional
    public void deleteTable(String tableName) {
        log.info("Deleting table '{}' and all its rows", tableName);
        rowRepository.deleteByTableName(tableName);
        tableRepository.deleteByTableName(tableName);
    }
    
    /**
     * Determine which columns to export
     */
    private Set<String> determineColumns(List<SearchResult> results, List<String> requestedColumns) {
        if (requestedColumns != null && !requestedColumns.isEmpty()) {
            // Use requested columns
            return new LinkedHashSet<>(requestedColumns);
        }
        
        // Auto-detect all fields from results
        Set<String> allFields = new LinkedHashSet<>();
        allFields.add("timestamp");
        allFields.add("raw_text");
        
        for (SearchResult result : results) {
            if (result.getFields() != null) {
                allFields.addAll(result.getFields().keySet());
            }
        }
        
        return allFields;
    }
    
    /**
     * Extract row data from a search result
     */
    private Map<String, String> extractRowData(SearchResult result, Set<String> columns) {
        Map<String, String> rowData = new HashMap<>();
        
        for (String column : columns) {
            String value = null;
            
            if ("timestamp".equals(column) && result.getTimestamp() != null) {
                value = result.getTimestamp().toString();
            } else if ("raw_text".equals(column)) {
                value = result.getRawText();
            } else if (result.getFields() != null) {
                value = result.getFields().get(column);
            }
            
            if (value != null) {
                rowData.put(column, value);
            }
        }
        
        return rowData;
    }
    
    /**
     * Result of an export operation
     */
    public static class ExportResult {
        private final String tableName;
        private final int rowsExported;
        private final long totalRows;
        private final List<String> columns;
        
        public ExportResult(String tableName, int rowsExported, long totalRows, List<String> columns) {
            this.tableName = tableName;
            this.rowsExported = rowsExported;
            this.totalRows = totalRows;
            this.columns = columns;
        }
        
        public String getTableName() {
            return tableName;
        }
        
        public int getRowsExported() {
            return rowsExported;
        }
        
        public long getTotalRows() {
            return totalRows;
        }
        
        public List<String> getColumns() {
            return columns;
        }
    }
}
