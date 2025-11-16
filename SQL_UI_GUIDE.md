# SQL Query UI Guide

A web-based SQL query interface for directly querying the H2 database.

## Access

Once the service is running, access the SQL UI at:

```
http://localhost:8080/ui/sql.html
```

## Features

### 1. Query Tab
Execute SELECT queries and view results in a formatted table.

**Quick Query Examples:**
- **List All Tables**: View metadata about exported tables
- **Recent Tables**: See recently created tables sorted by date
- **Show Columns**: Display table column definitions

**Sample Queries:**
```sql
-- View all exported tables
SELECT * FROM exported_tables;

-- Get table metadata with row counts
SELECT table_name, row_count, created_at 
FROM exported_tables 
ORDER BY created_at DESC;

-- Query exported data
SELECT * FROM exported_rows 
WHERE table_name = 'error_logs' 
LIMIT 100;

-- Join with row data
SELECT er.timestamp, rd.field_name, rd.field_value
FROM exported_rows er
JOIN row_data rd ON er.id = rd.row_id
WHERE er.table_name = 'error_logs';
```

**Features:**
- Syntax highlighting with monospace font
- SQL formatting button for readability
- Displays column types and row counts
- Handles NULL values gracefully

### 2. Create Table Tab
Visual interface for creating custom tables.

**Steps:**
1. Enter a table name
2. Add columns with names and data types:
   - BIGINT - Large integers
   - VARCHAR(255) - Short text (up to 255 characters)
   - VARCHAR(4000) - Long text (up to 4000 characters)
   - TIMESTAMP - Date and time values
   - INTEGER - Standard integers
   - BOOLEAN - True/false values
   - DOUBLE - Decimal numbers
3. Click "Create Table" to execute

**Example Use Case:**
```sql
CREATE TABLE custom_metrics (
  id BIGINT,
  metric_name VARCHAR(255),
  metric_value DOUBLE,
  recorded_at TIMESTAMP
);
```

**Preview SQL Button:** View the generated CREATE TABLE statement before executing.

### 3. Insert Data Tab
Execute INSERT statements to populate tables.

**Sample INSERT:**
```sql
INSERT INTO custom_metrics (id, metric_name, metric_value, recorded_at) 
VALUES (1, 'cpu_usage', 87.5, CURRENT_TIMESTAMP);
```

**Batch Inserts:**
```sql
INSERT INTO custom_metrics (id, metric_name, metric_value, recorded_at) VALUES
  (1, 'cpu_usage', 87.5, CURRENT_TIMESTAMP),
  (2, 'memory_usage', 62.3, CURRENT_TIMESTAMP),
  (3, 'disk_usage', 45.8, CURRENT_TIMESTAMP);
```

### 4. Tables Sidebar
- Lists all database tables
- Shows row counts for each table
- Click a table to generate a SELECT query
- Refresh button to update the list

## Supported SQL Operations

### SELECT Queries
```sql
-- Simple select
SELECT * FROM exported_tables;

-- With WHERE clause
SELECT * FROM exported_rows WHERE table_name = 'error_logs';

-- With aggregation
SELECT table_name, COUNT(*) as row_count 
FROM exported_rows 
GROUP BY table_name;

-- With ORDER BY and LIMIT
SELECT * FROM exported_tables 
ORDER BY created_at DESC 
LIMIT 10;
```

### CREATE TABLE
```sql
CREATE TABLE my_table (
  id BIGINT,
  name VARCHAR(255),
  created_at TIMESTAMP
);
```

### INSERT
```sql
INSERT INTO my_table (id, name, created_at) 
VALUES (1, 'test', CURRENT_TIMESTAMP);
```

### UPDATE
```sql
UPDATE my_table 
SET name = 'updated' 
WHERE id = 1;
```

### DELETE
```sql
DELETE FROM my_table WHERE id = 1;
```

### DROP TABLE
```sql
DROP TABLE my_table;
```

## Database Schema

The log search application uses these main tables:

### exported_tables
Metadata about exported log data tables.

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| table_name | VARCHAR(255) | Unique table name |
| description | VARCHAR(4000) | Optional description |
| created_at | TIMESTAMP | Creation timestamp |
| row_count | INTEGER | Number of rows |
| source_query | VARCHAR(4000) | Original search query |

### table_columns
Column definitions for exported tables.

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| table_id | BIGINT | Foreign key to exported_tables |
| column_name | VARCHAR(255) | Column name |
| column_order | INTEGER | Display order |

### exported_rows
Individual log entries in exported tables.

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| table_name | VARCHAR(255) | Table this row belongs to |
| timestamp | TIMESTAMP | Log entry timestamp |

### row_data
Key-value pairs for each exported row.

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| row_id | BIGINT | Foreign key to exported_rows |
| field_name | VARCHAR(255) | Field/column name |
| field_value | VARCHAR(4000) | Field value |

## Use Cases

### 1. Data Analysis
Query exported logs for patterns:
```sql
SELECT field_value as error_type, COUNT(*) as count
FROM row_data
WHERE field_name = 'level' AND field_value = 'ERROR'
GROUP BY field_value
ORDER BY count DESC;
```

### 2. Custom Reporting Tables
Create aggregated views:
```sql
CREATE TABLE error_summary (
  id BIGINT,
  date VARCHAR(255),
  error_count INTEGER,
  created_at TIMESTAMP
);

INSERT INTO error_summary (id, date, error_count, created_at)
SELECT 
  ROW_NUMBER() OVER () as id,
  CAST(timestamp AS VARCHAR(255)) as date,
  COUNT(*) as error_count,
  CURRENT_TIMESTAMP
FROM exported_rows
WHERE table_name = 'error_logs'
GROUP BY CAST(timestamp AS VARCHAR(255));
```

### 3. Data Cleanup
Remove old or invalid data:
```sql
-- Delete old exports
DELETE FROM exported_rows 
WHERE table_name = 'temp_logs' 
  AND timestamp < DATEADD('DAY', -30, CURRENT_TIMESTAMP);

-- Clean up metadata
DELETE FROM exported_tables 
WHERE table_name = 'temp_logs';
```

### 4. Testing and Validation
Verify exported data integrity:
```sql
-- Check for missing data
SELECT er.id, er.table_name
FROM exported_rows er
LEFT JOIN row_data rd ON er.id = rd.row_id
WHERE rd.id IS NULL;

-- Validate row counts match
SELECT 
  et.table_name,
  et.row_count as expected_count,
  COUNT(DISTINCT er.id) as actual_count
FROM exported_tables et
LEFT JOIN exported_rows er ON et.table_name = er.table_name
GROUP BY et.table_name, et.row_count
HAVING et.row_count != COUNT(DISTINCT er.id);
```

## Tips and Best Practices

1. **Use LIMIT**: When querying large tables, always use LIMIT to avoid overwhelming the browser
   ```sql
   SELECT * FROM large_table LIMIT 100;
   ```

2. **Format Queries**: Use the "Format" button to make complex queries more readable

3. **Transaction Safety**: The UI executes queries directly - be careful with UPDATE and DELETE operations

4. **Column Selection**: Select only needed columns for better performance
   ```sql
   SELECT id, timestamp, message FROM logs;  -- Good
   SELECT * FROM logs;  -- Can be slow for wide tables
   ```

5. **Use Indexes**: Consider adding indexes for frequently queried columns
   ```sql
   CREATE INDEX idx_table_name ON exported_rows(table_name);
   ```

6. **Export Results**: Copy result data from the table for use in spreadsheets or reports

## API Endpoints

The SQL UI uses these REST endpoints:

### Execute Query
```
POST /api/sql/query
Content-Type: application/json

{
  "sql": "SELECT * FROM exported_tables"
}
```

### List Tables
```
GET /api/sql/tables
```

### Get Table Schema
```
GET /api/sql/tables/{tableName}/schema
```

## Alternative: H2 Console

For advanced features, you can also use the built-in H2 console:

```
http://localhost:8080/h2-console
```

**Connection details:**
- JDBC URL: `jdbc:h2:file:~/.local_log_search/database/logdb`
- Username: `sa`
- Password: (empty)

The H2 console provides additional features like:
- Schema visualization
- Auto-complete
- Query history
- Export to CSV
- Execution plans

## Troubleshooting

### Connection Errors
- Ensure the service is running on port 8080
- Check that the H2 database file exists at `~/.local_log_search/database/logdb`

### Query Errors
- Use the error message to identify syntax issues
- Check table and column names (H2 uses uppercase by default)
- Verify data types match in INSERT statements

### Performance Issues
- Add LIMIT clauses to large queries
- Consider adding indexes for frequently queried columns
- Use WHERE clauses to filter data early

### Empty Results
- Check that data has been exported using the export feature
- Verify table names match exactly (case-sensitive)
- Use `SELECT * FROM exported_tables` to see available tables
