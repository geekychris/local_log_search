# Database Export Feature

Export search results to database tables for later querying and analysis.

## Overview

The database export feature allows you to:
- Export search results to H2 database tables
- Define custom table names and column mappings
- Sample results (export all or N rows)
- Query exported data later
- Use exported data in future searches

## Configuration

The service uses an embedded H2 database stored at:
```
~/.local_log_search/database/logdb
```

### H2 Console Access

Access the H2 console at: http://localhost:8080/h2-console

- **JDBC URL**: `jdbc:h2:file:~/.local_log_search/database/logdb`
- **Username**: `sa`
- **Password**: (empty)

## Usage

### 1. Pipe Command Syntax

Export search results using the pipe command:

```
<query> | export table=<tablename> [fields=<columns>] [sample=<N>] [append=true|false]
```

**Parameters:**
- `table` (required): Name of the table to create/append to
- `fields` (optional): Comma-separated list of fields to export (default: all fields)
- `sample` (optional): Maximum number of rows to export (default: all rows)
- `append` (optional): Append to existing table or replace (default: true)

**Examples:**

```bash
# Export all error logs to a table
level:ERROR | export table=error_logs

# Export specific fields with sampling
level:ERROR | export table=error_sample fields=timestamp,user,operation,message sample=1000

# Replace existing table
level:WARN | export table=warnings append=false

# Export all fields from slow operations
status:slow | export table=slow_ops fields=timestamp,operation,duration,user
```

### 2. REST API

#### Export Search Results

```bash
POST /api/export/results
Content-Type: application/json

{
  "indices": ["app-logs"],
  "query": "level:ERROR",
  "tableName": "error_logs",
  "fields": ["timestamp", "user", "operation", "message"],
  "sampleSize": 1000,
  "append": true,
  "timestampFrom": 1704067200000,
  "timestampTo": 1704153600000
}
```

Response:
```json
{
  "tableName": "error_logs",
  "rowsExported": 1000,
  "totalRows": 1000,
  "columns": ["message", "operation", "timestamp", "user"]
}
```

#### List Exported Tables

```bash
GET /api/export/tables
```

Response:
```json
[
  {
    "id": 1,
    "tableName": "error_logs",
    "description": null,
    "createdAt": "2025-01-14T10:00:00Z",
    "rowCount": 1000,
    "sourceQuery": "level:ERROR",
    "columns": ["timestamp", "user", "operation", "message"]
  }
]
```

#### Get Table Metadata

```bash
GET /api/export/tables/{tableName}
```

#### Query Table Data

```bash
GET /api/export/tables/{tableName}/rows?page=0&size=50
```

Response:
```json
{
  "content": [
    {
      "id": 1,
      "tableName": "error_logs",
      "timestamp": "2025-01-14T10:30:00Z",
      "data": {
        "timestamp": "2025-01-14T10:30:00Z",
        "user": "john.doe",
        "operation": "processOrder",
        "message": "Database connection timeout"
      }
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 50
  },
  "totalElements": 1000,
  "totalPages": 20
}
```

#### Delete Table

```bash
DELETE /api/export/tables/{tableName}
```

## Use Cases

### 1. Debugging with Historical Data

Export error logs for a specific time window:
```bash
level:ERROR timestamp:[2025-01-14 TO 2025-01-15] | export table=jan14_errors
```

Later, query the exported data:
```bash
curl http://localhost:8080/api/export/tables/jan14_errors/rows
```

### 2. Statistical Analysis

Export slow operations for analysis:
```bash
status:slow | export table=slow_operations fields=operation,duration,user
```

### 3. Sampling Large Result Sets

Export a representative sample from millions of logs:
```bash
level:INFO | export table=info_sample sample=10000
```

### 4. Building Test Data Sets

Export specific patterns for testing:
```bash
user:testuser AND operation:checkout | export table=test_checkout_logs
```

### 5. Cross-Reference Analysis

Export user activities to compare with other systems:
```bash
operation:payment | export table=payment_events fields=timestamp,user,amount,status
```

## Querying Exported Data

### Using H2 Console

1. Open http://localhost:8080/h2-console
2. Connect with credentials above
3. Query using SQL:

```sql
-- View all exported tables
SELECT * FROM exported_tables;

-- View rows from a specific table
SELECT * FROM exported_rows WHERE table_name = 'error_logs';

-- Join with row data
SELECT er.timestamp, rd.field_value
FROM exported_rows er
JOIN row_data rd ON er.id = rd.row_id
WHERE er.table_name = 'error_logs'
  AND rd.field_name = 'user';
```

### Using REST API

```bash
# Get paginated results
curl "http://localhost:8080/api/export/tables/error_logs/rows?page=0&size=100"

# Get table metadata
curl "http://localhost:8080/api/export/tables/error_logs"
```

## Advanced: Using Exported Data in Future Queries

While the current implementation stores data in H2, you can:

1. **Export to CSV** for external analysis:
   - Query via H2 console and export results
   - Use REST API to fetch data and convert to CSV

2. **Re-index into Lucene** (future enhancement):
   - Read from exported tables
   - Index back into Lucene for fast searching

3. **Join with live data** (future enhancement):
   - Query both live indices and exported tables
   - Combine results for historical analysis

## Database Schema

### exported_tables
- `id`: Primary key
- `table_name`: Unique table name
- `description`: Optional description
- `created_at`: Creation timestamp
- `row_count`: Number of rows
- `source_query`: Original query that produced the data

### table_columns
- `table_id`: Foreign key to exported_tables
- `column_name`: Name of the column
- `column_order`: Order of the column

### exported_rows
- `id`: Primary key
- `table_name`: Table this row belongs to
- `timestamp`: Timestamp from the log entry

### row_data
- `row_id`: Foreign key to exported_rows
- `field_name`: Name of the field
- `field_value`: Value of the field (up to 4000 chars)

## Performance Considerations

1. **Sampling**: Use `sample=N` for large result sets to avoid memory issues
2. **Indexing**: The `table_name` field is indexed for fast queries
3. **Pagination**: Always use pagination when querying large tables
4. **Column Selection**: Export only needed columns to reduce storage

## Limitations

1. **Field Value Length**: Maximum 4000 characters per field value
2. **Memory**: Exporting very large result sets requires adequate memory
3. **Concurrent Access**: H2 in file mode has limited concurrent write support
4. **Storage**: Exported data is stored on disk, monitor disk usage

## Future Enhancements

- [ ] Export to CSV/JSON files
- [ ] Export to external databases (PostgreSQL, MySQL)
- [ ] Re-index exported data into Lucene
- [ ] Schedule automatic exports
- [ ] Retention policies for exported tables
- [ ] UI for browsing exported tables
- [ ] SQL query interface for exported data
- [ ] Join exported tables with live search results
