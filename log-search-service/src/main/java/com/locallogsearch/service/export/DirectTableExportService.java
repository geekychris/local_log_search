package com.locallogsearch.service.export;

import com.locallogsearch.core.search.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.*;

/**
 * Service for exporting search results directly to user-named SQL tables.
 * Unlike ExportService which uses an EAV pattern, this creates real SQL tables
 * with actual columns that can be queried directly.
 * 
 * Database-agnostic: works with any JDBC-compliant database.
 */
@Service
public class DirectTableExportService {
    private static final Logger log = LoggerFactory.getLogger(DirectTableExportService.class);
    
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    
    public DirectTableExportService(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }
    
    /**
     * Export search results to a real SQL table with actual columns
     * 
     * @param tableName Name of the SQL table to create
     * @param results Search results to export
     * @param columns Columns to export (null = all fields)
     * @param sampleSize Maximum number of rows to export (null = all)
     * @param sourceQuery Original query that produced these results
     * @param append If true, append to existing table; if false, replace
     * @return Export statistics
     */
    @Transactional
    public DirectExportResult exportToTable(String tableName, List<SearchResult> results,
                                           List<String> columns, Integer sampleSize,
                                           String sourceQuery, boolean append) {
        log.info("Direct export: {} results to table '{}' (columns: {}, sample: {}, append: {})",
                results.size(), tableName, columns, sampleSize, append);
        
        // Validate table name (SQL injection prevention)
        validateTableName(tableName);
        tableName = sanitizeTableName(tableName);
        
        // Determine which results to export
        List<SearchResult> toExport = results;
        if (sampleSize != null && sampleSize > 0 && sampleSize < results.size()) {
            toExport = results.subList(0, sampleSize);
        }
        
        if (toExport.isEmpty()) {
            return new DirectExportResult(tableName, 0, 0, Collections.emptyList());
        }
        
        // Analyze data to determine columns and types
        TableSchema schema = analyzeSchema(toExport, columns);
        
        try (Connection conn = dataSource.getConnection()) {
            boolean tableExists = checkTableExists(conn, tableName);
            
            if (tableExists) {
                if (append) {
                    log.info("Table '{}' exists, appending data", tableName);
                } else {
                    log.info("Table '{}' exists, dropping and recreating (append=false)", tableName);
                    dropTable(conn, tableName);
                    createTable(conn, tableName, schema);
                }
            } else {
                log.info("Creating new table '{}'", tableName);
                createTable(conn, tableName, schema);
            }
            
            // Insert data
            int rowsInserted = insertData(conn, tableName, schema, toExport);
            
            // Get final row count
            long totalRows = getTableRowCount(tableName);
            
            log.info("Direct export complete: {} rows inserted to '{}' (total: {})",
                    rowsInserted, tableName, totalRows);
            
            return new DirectExportResult(tableName, rowsInserted, totalRows, schema.columns);
            
        } catch (SQLException e) {
            log.error("Error exporting to table '{}'", tableName, e);
            throw new RuntimeException("Failed to export to table: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate table name to prevent SQL injection
     */
    private void validateTableName(String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new IllegalArgumentException("Table name cannot be empty");
        }
        
        // Only allow alphanumeric, underscore, and dash
        if (!tableName.matches("^[a-zA-Z0-9_-]+$")) {
            throw new IllegalArgumentException(
                "Table name can only contain letters, numbers, underscores, and dashes: " + tableName);
        }
        
        if (tableName.length() > 64) {
            throw new IllegalArgumentException("Table name too long (max 64 characters): " + tableName);
        }
    }
    
    /**
     * Sanitize table name for SQL (database-agnostic)
     */
    private String sanitizeTableName(String tableName) {
        // Replace dashes with underscores for better SQL compatibility
        return tableName.trim().toUpperCase().replace('-', '_');
    }
    
    /**
     * Check if a table exists
     */
    private boolean checkTableExists(Connection conn, String tableName) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        
        // Try multiple case variations
        String[] variations = {tableName, tableName.toUpperCase(), tableName.toLowerCase()};
        
        for (String variant : variations) {
            try (ResultSet rs = metaData.getTables(null, null, variant, new String[]{"TABLE"})) {
                if (rs.next()) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Analyze search results to determine table schema
     */
    private TableSchema analyzeSchema(List<SearchResult> results, List<String> requestedColumns) {
        Set<String> columnSet = new LinkedHashSet<>();
        Map<String, ColumnType> columnTypes = new LinkedHashMap<>();
        
        // Always include timestamp and raw_text
        columnSet.add("timestamp");
        columnTypes.put("timestamp", ColumnType.TIMESTAMP);
        
        if (requestedColumns == null || requestedColumns.isEmpty()) {
            columnSet.add("raw_text");
            columnTypes.put("raw_text", ColumnType.LONG_TEXT);
        }
        
        // Determine columns
        if (requestedColumns != null && !requestedColumns.isEmpty()) {
            columnSet.addAll(requestedColumns);
        } else {
            // Auto-detect from results
            for (SearchResult result : results) {
                if (result.getFields() != null) {
                    columnSet.addAll(result.getFields().keySet());
                }
            }
        }
        
        // Analyze data types for each column
        for (String column : columnSet) {
            if (!columnTypes.containsKey(column)) {
                ColumnType type = detectColumnType(column, results);
                columnTypes.put(column, type);
            }
        }
        
        List<String> columns = new ArrayList<>(columnSet);
        return new TableSchema(columns, columnTypes);
    }
    
    /**
     * Detect the best SQL type for a column by examining the data
     * Uses conservative approach - requires ALL sampled values to be numeric
     */
    private ColumnType detectColumnType(String columnName, List<SearchResult> results) {
        // Check ALL values (not just sample) to be safe
        int maxLength = 0;
        boolean allNumeric = true;
        boolean hasDecimal = false;
        int numericCount = 0;
        int nonNullCount = 0;
        
        for (SearchResult result : results) {
            String value = null;
            
            if (result.getFields() != null) {
                value = result.getFields().get(columnName);
            }
            
            if (value != null && !value.isEmpty()) {
                nonNullCount++;
                maxLength = Math.max(maxLength, value.length());
                
                // Check if numeric
                try {
                    if (value.contains(".")) {
                        Double.parseDouble(value);
                        hasDecimal = true;
                        numericCount++;
                    } else {
                        Long.parseLong(value);
                        numericCount++;
                    }
                } catch (NumberFormatException e) {
                    allNumeric = false;
                    // Once we find a non-numeric value, we know it's not numeric
                    break;
                }
            }
        }
        
        // Determine type - only use numeric if ALL non-null values are numeric
        if (allNumeric && numericCount > 0 && numericCount == nonNullCount) {
            return hasDecimal ? ColumnType.DOUBLE : ColumnType.BIGINT;
        } else if (maxLength <= 255) {
            return ColumnType.VARCHAR_255;
        } else if (maxLength <= 1000) {
            return ColumnType.VARCHAR_1000;
        } else {
            return ColumnType.LONG_TEXT;
        }
    }
    
    /**
     * Create the SQL table
     */
    private void createTable(Connection conn, String tableName, TableSchema schema) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ").append(tableName).append(" (\n");
        sql.append("  id BIGINT AUTO_INCREMENT PRIMARY KEY,\n");
        
        for (int i = 0; i < schema.columns.size(); i++) {
            String column = schema.columns.get(i);
            ColumnType type = schema.columnTypes.get(column);
            
            // Quote column names to handle reserved keywords
            sql.append("  \"").append(sanitizeColumnName(column)).append("\"")
               .append(" ").append(type.getSqlType());
            
            if (i < schema.columns.size() - 1) {
                sql.append(",\n");
            }
        }
        
        sql.append("\n)");
        
        log.debug("Creating table with SQL: {}", sql);
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql.toString());
        }
    }
    
    /**
     * Drop a table
     */
    private void dropTable(Connection conn, String tableName) throws SQLException {
        String sql = "DROP TABLE IF EXISTS " + tableName;
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }
    
    /**
     * Insert data into the table
     */
    private int insertData(Connection conn, String tableName, TableSchema schema, 
                          List<SearchResult> results) throws SQLException {
        
        // Build INSERT statement with quoted column names
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(tableName).append(" (");
        
        for (int i = 0; i < schema.columns.size(); i++) {
            sql.append("\"").append(sanitizeColumnName(schema.columns.get(i))).append("\"");
            if (i < schema.columns.size() - 1) {
                sql.append(", ");
            }
        }
        
        sql.append(") VALUES (");
        for (int i = 0; i < schema.columns.size(); i++) {
            sql.append("?");
            if (i < schema.columns.size() - 1) {
                sql.append(", ");
            }
        }
        sql.append(")");
        
        int rowsInserted = 0;
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (SearchResult result : results) {
                // Set parameter values
                for (int i = 0; i < schema.columns.size(); i++) {
                    String column = schema.columns.get(i);
                    String value = extractValue(result, column);
                    
                    if (value == null) {
                        pstmt.setNull(i + 1, Types.VARCHAR);
                    } else {
                        ColumnType type = schema.columnTypes.get(column);
                        setParameterValue(pstmt, i + 1, value, type);
                    }
                }
                
                pstmt.addBatch();
                rowsInserted++;
                
                // Execute batch every 1000 rows
                if (rowsInserted % 1000 == 0) {
                    pstmt.executeBatch();
                }
            }
            
            // Execute remaining batch
            if (rowsInserted % 1000 != 0) {
                pstmt.executeBatch();
            }
        }
        
        return rowsInserted;
    }
    
    /**
     * Set parameter value with appropriate type
     */
    private void setParameterValue(PreparedStatement pstmt, int index, String value, 
                                   ColumnType type) throws SQLException {
        try {
            switch (type) {
                case BIGINT:
                    pstmt.setLong(index, Long.parseLong(value));
                    break;
                case DOUBLE:
                    pstmt.setDouble(index, Double.parseDouble(value));
                    break;
                case TIMESTAMP:
                    // Try to parse as Instant, fall back to string
                    try {
                        Timestamp ts = Timestamp.from(Instant.parse(value));
                        pstmt.setTimestamp(index, ts);
                    } catch (Exception e) {
                        pstmt.setString(index, value);
                    }
                    break;
                default:
                    pstmt.setString(index, value);
            }
        } catch (Exception e) {
            // If conversion fails, store as string
            pstmt.setString(index, value);
        }
    }
    
    /**
     * Extract value from search result
     */
    private String extractValue(SearchResult result, String column) {
        if ("timestamp".equals(column)) {
            return result.getTimestamp() != null ? result.getTimestamp().toString() : null;
        } else if ("raw_text".equals(column)) {
            return result.getRawText();
        } else if (result.getFields() != null) {
            return result.getFields().get(column);
        }
        return null;
    }
    
    /**
     * Sanitize column name
     */
    private String sanitizeColumnName(String column) {
        return column.toUpperCase().replace('-', '_').replace('.', '_');
    }
    
    /**
     * Get row count for a table
     */
    private long getTableRowCount(String tableName) {
        try {
            Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName,
                Long.class
            );
            return count != null ? count : 0;
        } catch (Exception e) {
            log.warn("Could not get row count for table '{}'", tableName, e);
            return 0;
        }
    }
    
    /**
     * Table schema information
     */
    private static class TableSchema {
        final List<String> columns;
        final Map<String, ColumnType> columnTypes;
        
        TableSchema(List<String> columns, Map<String, ColumnType> columnTypes) {
            this.columns = columns;
            this.columnTypes = columnTypes;
        }
    }
    
    /**
     * SQL column types
     */
    private enum ColumnType {
        VARCHAR_255("VARCHAR(255)"),
        VARCHAR_1000("VARCHAR(1000)"),
        LONG_TEXT("VARCHAR(4000)"),
        BIGINT("BIGINT"),
        DOUBLE("DOUBLE"),
        TIMESTAMP("TIMESTAMP");
        
        private final String sqlType;
        
        ColumnType(String sqlType) {
            this.sqlType = sqlType;
        }
        
        String getSqlType() {
            return sqlType;
        }
    }
    
    /**
     * Result of a direct export operation
     */
    public static class DirectExportResult {
        private final String tableName;
        private final int rowsExported;
        private final long totalRows;
        private final List<String> columns;
        
        public DirectExportResult(String tableName, int rowsExported, long totalRows, List<String> columns) {
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
