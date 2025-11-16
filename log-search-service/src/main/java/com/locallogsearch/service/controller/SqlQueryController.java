package com.locallogsearch.service.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@RestController
@RequestMapping("/api/sql")
public class SqlQueryController {

    private static final Logger logger = LoggerFactory.getLogger(SqlQueryController.class);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Execute a SQL query and return results
     */
    @PostMapping("/query")
    public ResponseEntity<?> executeQuery(@RequestBody SqlQueryRequest request) {
        String sql = request.getSql();
        
        if (sql == null || sql.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "SQL query is required"));
        }

        sql = sql.trim();
        logger.info("Executing SQL: {}", sql);

        try {
            // Determine if this is a SELECT query
            boolean isSelect = sql.toUpperCase().startsWith("SELECT");

            if (isSelect) {
                return executeSelectQuery(sql);
            } else {
                return executeUpdateQuery(sql);
            }
        } catch (Exception e) {
            logger.error("Error executing SQL: {}", sql, e);
            return ResponseEntity.status(500).body(Map.of(
                "error", e.getMessage(),
                "type", e.getClass().getSimpleName()
            ));
        }
    }

    /**
     * Get all tables in the database
     * Database-agnostic: works with any JDBC-compliant database
     */
    @GetMapping("/tables")
    public ResponseEntity<?> getTables() {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String schemaPattern = null;
            
            // Try to get the current schema for better filtering
            try {
                schemaPattern = conn.getSchema();
            } catch (Exception e) {
                // Some databases don't support getSchema(), ignore
            }
            
            ResultSet tables = metaData.getTables(null, schemaPattern, "%", new String[]{"TABLE"});
            
            List<Map<String, Object>> tableList = new ArrayList<>();
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                String tableSchema = tables.getString("TABLE_SCHEM");
                
                // Skip system/internal tables (database-agnostic filtering)
                if (isSystemTable(tableName, tableSchema)) {
                    continue;
                }
                
                Map<String, Object> tableInfo = new LinkedHashMap<>();
                tableInfo.put("tableName", tableName);
                tableInfo.put("tableType", tables.getString("TABLE_TYPE"));
                if (tableSchema != null) {
                    tableInfo.put("schema", tableSchema);
                }
                
                // Get row count (with error handling for permissions/access issues)
                try {
                    String countQuery = schemaPattern != null && !schemaPattern.isEmpty()
                        ? String.format("SELECT COUNT(*) FROM %s.%s", schemaPattern, tableName)
                        : String.format("SELECT COUNT(*) FROM %s", tableName);
                    Integer rowCount = jdbcTemplate.queryForObject(countQuery, Integer.class);
                    tableInfo.put("rowCount", rowCount);
                } catch (Exception e) {
                    tableInfo.put("rowCount", 0);
                }
                
                tableList.add(tableInfo);
            }
            
            return ResponseEntity.ok(tableList);
        } catch (Exception e) {
            logger.error("Error getting tables", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Check if a table is a system/internal table
     * Database-agnostic filtering for common system table patterns
     */
    private boolean isSystemTable(String tableName, String schema) {
        if (tableName == null) return true;
        
        String upperName = tableName.toUpperCase();
        String upperSchema = schema != null ? schema.toUpperCase() : "";
        
        // H2 system tables
        if (upperSchema.equals("INFORMATION_SCHEMA") || upperName.startsWith("INFORMATION_SCHEMA")) {
            return true;
        }
        
        // PostgreSQL system tables
        if (upperSchema.equals("PG_CATALOG") || upperName.startsWith("PG_")) {
            return true;
        }
        
        // MySQL system tables
        if (upperSchema.equals("MYSQL") || upperSchema.equals("PERFORMANCE_SCHEMA") || 
            upperSchema.equals("SYS") || upperSchema.equals("INFORMATION_SCHEMA")) {
            return true;
        }
        
        // Oracle system tables
        if (upperName.startsWith("SYS_") || upperSchema.equals("SYS") || 
            upperSchema.equals("SYSTEM")) {
            return true;
        }
        
        // SQL Server system tables
        if (upperName.startsWith("SYS") || upperName.startsWith("MSP") || 
            upperSchema.equals("SYS")) {
            return true;
        }
        
        return false;
    }

    /**
     * Get table schema information
     * Database-agnostic: tries case-sensitive and case-insensitive lookups
     */
    @GetMapping("/tables/{tableName}/schema")
    public ResponseEntity<?> getTableSchema(@PathVariable String tableName) {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            // Try multiple case variations to handle different database conventions
            List<Map<String, Object>> columnList = getColumnsForTable(metaData, tableName);
            
            if (columnList.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of(
                    "error", "Table not found: " + tableName,
                    "suggestion", "Check table name case sensitivity"
                ));
            }
            
            return ResponseEntity.ok(Map.of(
                "tableName", tableName,
                "columns", columnList
            ));
        } catch (Exception e) {
            logger.error("Error getting table schema for {}", tableName, e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get columns for a table, trying different case variations
     * Database-agnostic approach to handle case sensitivity differences
     */
    private List<Map<String, Object>> getColumnsForTable(DatabaseMetaData metaData, String tableName) 
            throws SQLException {
        List<Map<String, Object>> columnList = new ArrayList<>();
        
        // Try: original case, uppercase, lowercase
        String[] variations = {tableName, tableName.toUpperCase(), tableName.toLowerCase()};
        
        for (String variant : variations) {
            ResultSet columns = metaData.getColumns(null, null, variant, null);
            while (columns.next()) {
                Map<String, Object> columnInfo = new LinkedHashMap<>();
                columnInfo.put("columnName", columns.getString("COLUMN_NAME"));
                columnInfo.put("dataType", columns.getString("TYPE_NAME"));
                columnInfo.put("columnSize", columns.getInt("COLUMN_SIZE"));
                columnInfo.put("nullable", columns.getBoolean("NULLABLE"));
                columnList.add(columnInfo);
            }
            columns.close();
            
            if (!columnList.isEmpty()) {
                break; // Found columns, stop trying variations
            }
        }
        
        return columnList;
    }

    private ResponseEntity<?> executeSelectQuery(String sql) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            // Build column metadata
            List<Map<String, Object>> columns = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                Map<String, Object> column = new LinkedHashMap<>();
                column.put("name", metaData.getColumnName(i));
                column.put("type", metaData.getColumnTypeName(i));
                column.put("label", metaData.getColumnLabel(i));
                columns.add(column);
            }
            
            // Build rows
            List<Map<String, Object>> rows = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = rs.getObject(i);
                    
                    // Convert database-specific types to JSON-serializable types
                    // This is database-agnostic and handles common SQL types
                    if (value instanceof Timestamp) {
                        value = value.toString();
                    } else if (value instanceof java.sql.Date) {
                        value = value.toString();
                    } else if (value instanceof Time) {
                        value = value.toString();
                    } else if (value instanceof Clob) {
                        Clob clob = (Clob) value;
                        value = clob.getSubString(1, (int) clob.length());
                    } else if (value instanceof Blob) {
                        Blob blob = (Blob) value;
                        value = "<BLOB " + blob.length() + " bytes>";
                    } else if (value instanceof byte[]) {
                        value = "<BINARY " + ((byte[]) value).length + " bytes>";
                    }
                    
                    row.put(columnName, value);
                }
                rows.add(row);
            }
            
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("columns", columns);
            result.put("rows", rows);
            result.put("rowCount", rows.size());
            result.put("type", "SELECT");
            
            return ResponseEntity.ok(result);
            
        } catch (SQLException e) {
            logger.error("Error executing SELECT query", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", e.getMessage(),
                "sqlState", e.getSQLState(),
                "errorCode", e.getErrorCode()
            ));
        }
    }

    private ResponseEntity<?> executeUpdateQuery(String sql) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            boolean hasResultSet = stmt.execute(sql);
            
            if (hasResultSet) {
                // Query returned a result set (shouldn't happen for UPDATE/INSERT/CREATE)
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "type", "QUERY",
                    "message", "Query executed successfully"
                ));
            } else {
                int updateCount = stmt.getUpdateCount();
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "type", determineQueryType(sql),
                    "rowsAffected", updateCount,
                    "message", "Query executed successfully"
                ));
            }
            
        } catch (SQLException e) {
            logger.error("Error executing UPDATE query", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", e.getMessage(),
                "sqlState", e.getSQLState(),
                "errorCode", e.getErrorCode()
            ));
        }
    }

    private String determineQueryType(String sql) {
        String upperSql = sql.toUpperCase().trim();
        if (upperSql.startsWith("INSERT")) return "INSERT";
        if (upperSql.startsWith("UPDATE")) return "UPDATE";
        if (upperSql.startsWith("DELETE")) return "DELETE";
        if (upperSql.startsWith("CREATE")) return "CREATE";
        if (upperSql.startsWith("DROP")) return "DROP";
        if (upperSql.startsWith("ALTER")) return "ALTER";
        return "UNKNOWN";
    }

    public static class SqlQueryRequest {
        private String sql;

        public String getSql() {
            return sql;
        }

        public void setSql(String sql) {
            this.sql = sql;
        }
    }
}
