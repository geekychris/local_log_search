# Database Export - Quick Start Guide

Get started with exporting search results to database tables in 5 minutes.

## Prerequisites

1. Service is running with test data:
```bash
# Terminal 1: Start test log generator
cd test-log-generator
java -jar target/test-log-generator-1.0-SNAPSHOT.jar

# Terminal 2: Start service
cd log-search-service
java -jar target/log-search-service-1.0-SNAPSHOT.jar
```

2. Add log sources (if not already done):
```bash
./add-log-source.sh test-log-generator/logs/application.log app-logs
```

Wait 30 seconds for logs to be indexed.

## Quick Examples

### 1. Export Error Logs (Pipe Command)

```bash
curl -X POST http://localhost:8080/api/export/results \
  -H "Content-Type: application/json" \
  -d '{
    "indices": ["app-logs"],
    "query": "level:ERROR | export table=error_logs sample=10"
  }'
```

Result:
```json
{
  "tableName": "error_logs",
  "rowsExported": 10,
  "totalRows": 10,
  "columns": ["duration", "level", "message", "operation", "raw_text", "status", "timestamp", "user"]
}
```

### 2. Export Specific Fields

```bash
curl -X POST http://localhost:8080/api/export/results \
  -H "Content-Type: application/json" \
  -d '{
    "indices": ["app-logs"],
    "query": "level:ERROR",
    "tableName": "errors_minimal",
    "fields": ["timestamp", "user", "operation"],
    "sampleSize": 5
  }'
```

### 3. List Exported Tables

```bash
curl http://localhost:8080/api/export/tables
```

### 4. Query Table Data

```bash
curl "http://localhost:8080/api/export/tables/error_logs/rows?page=0&size=10"
```

### 5. View in H2 Console

1. Open browser: http://localhost:8080/h2-console
2. Settings:
   - JDBC URL: `jdbc:h2:file:~/.local_log_search/database/logdb`
   - User: `sa`
   - Password: (leave empty)
3. Click Connect
4. Run queries:
```sql
-- See all tables
SELECT * FROM exported_tables;

-- See data from a table
SELECT * FROM exported_rows WHERE table_name = 'error_logs';

-- Get specific field values
SELECT er.timestamp, rd.field_value
FROM exported_rows er
JOIN row_data rd ON er.id = rd.row_id
WHERE er.table_name = 'error_logs'
  AND rd.field_name = 'user';
```

## Run Automated Tests

```bash
./test_export.sh
```

## Common Use Cases

### Save Investigation Results
```bash
# Find problematic logs
curl -X POST http://localhost:8080/api/export/results \
  -H "Content-Type: application/json" \
  -d '{
    "indices": ["app-logs"],
    "query": "user:admin AND status:slow",
    "tableName": "admin_slow_ops",
    "fields": ["timestamp", "operation", "duration"]
  }'
```

### Sample Large Result Sets
```bash
# Get 1000 samples from millions of logs
curl -X POST http://localhost:8080/api/export/results \
  -H "Content-Type: application/json" \
  -d '{
    "indices": ["app-logs"],
    "query": "level:INFO",
    "tableName": "info_sample",
    "sampleSize": 1000
  }'
```

### Export Time Range
```bash
# Export specific time window
curl -X POST http://localhost:8080/api/export/results \
  -H "Content-Type: application/json" \
  -d '{
    "indices": ["app-logs"],
    "query": "level:ERROR",
    "tableName": "today_errors",
    "timestampFrom": 1704067200000,
    "timestampTo": 1704153600000
  }'
```

## Clean Up

Delete a table:
```bash
curl -X DELETE http://localhost:8080/api/export/tables/error_logs
```

## Next Steps

- See [DATABASE_EXPORT.md](DATABASE_EXPORT.md) for complete documentation
- Explore H2 Console for SQL queries
- Use exported data for analysis and reporting
