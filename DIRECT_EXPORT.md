# Direct Table Export

Export search results to **real SQL tables** with actual columns that you can query directly with simple SQL.

## Overview

Unlike the EAV-based export (which uses `exported_tables`, `exported_rows`, `row_data`), the direct export creates normal SQL tables that you can query like any other table:

```sql
-- Simple query - no joins needed!
SELECT * FROM my_error_logs WHERE user = 'john';
```

## API Endpoint

```
POST /api/export/direct
```

### Request Body

```json
{
  "indices": ["app-logs"],
  "query": "level:ERROR",
  "tableName": "my_error_logs",
  "fields": ["timestamp", "user", "level", "message"],
  "sampleSize": 1000,
  "append": false,
  "timestampFrom": null,
  "timestampTo": null
}
```

**Parameters:**
- `indices` (required): List of indices to search
- `query` (required): Lucene query string
- `tableName` (required): Name for the SQL table (alphanumeric, underscore, dash only)
- `fields` (optional): Columns to export. If omitted, exports all fields plus timestamp and raw_text
- `sampleSize` (optional): Max rows to export. If omitted, exports all results
- `append` (optional): true = append to existing table, false = replace. Default: true
- `timestampFrom` (optional): Filter by timestamp (epoch millis)
- `timestampTo` (optional): Filter by timestamp (epoch millis)

### Response

```json
{
  "tableName": "MY_ERROR_LOGS",
  "rowsExported": 500,
  "totalRows": 500,
  "columns": ["TIMESTAMP", "USER", "LEVEL", "MESSAGE"]
}
```

## Examples

### Example 1: Export Error Logs

```bash
curl -X POST http://localhost:8080/api/export/direct \
  -H "Content-Type: application/json" \
  -d '{
    "indices": ["app-logs"],
    "query": "level:ERROR",
    "tableName": "error_logs"
  }'
```

This creates a table: `ERROR_LOGS` with columns auto-detected from the data.

**Query it:**
```sql
SELECT * FROM ERROR_LOGS LIMIT 10;
```

### Example 2: Specific Columns

```bash
curl -X POST http://localhost:8080/api/export/direct \
  -H "Content-Type: application/json" \
  -d '{
    "indices": ["app-logs"],
    "query": "user:john*",
    "tableName": "john_activity",
    "fields": ["timestamp", "user", "operation", "duration"]
  }'
```

Creates table: `JOHN_ACTIVITY (id, TIMESTAMP, USER, OPERATION, DURATION)`

**Query it:**
```sql
SELECT operation, AVG(CAST(duration AS DOUBLE)) as avg_duration
FROM JOHN_ACTIVITY
GROUP BY operation
ORDER BY avg_duration DESC;
```

### Example 3: Sample Large Results

```bash
curl -X POST http://localhost:8080/api/export/direct \
  -H "Content-Type: application/json" \
  -d '{
    "indices": ["app-logs"],
    "query": "*:*",
    "tableName": "sample_logs",
    "sampleSize": 1000
  }'
```

Exports only first 1000 results to `SAMPLE_LOGS`.

### Example 4: Replace Existing Table

```bash
curl -X POST http://localhost:8080/api/export/direct \
  -H "Content-Type: application/json" \
  -d '{
    "indices": ["app-logs"],
    "query": "level:WARN",
    "tableName": "warnings",
    "append": false
  }'
```

Drops and recreates `WARNINGS` table with fresh data.

## Created Table Structure

The service automatically:

1. **Analyzes your data** to determine column types
2. **Creates the table** with appropriate SQL types:
   - Numeric values → `BIGINT` or `DOUBLE`
   - Short text (≤255 chars) → `VARCHAR(255)`
   - Medium text (≤1000 chars) → `VARCHAR(1000)`
   - Long text → `VARCHAR(4000)`
   - Timestamps → `TIMESTAMP`

3. **Adds an ID column**: Every table gets an auto-increment `id BIGINT PRIMARY KEY`

### Example Table

If you export with fields `["timestamp", "user", "message", "count"]`:

```sql
CREATE TABLE MY_LOGS (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  TIMESTAMP TIMESTAMP,
  USER VARCHAR(255),
  MESSAGE VARCHAR(4000),
  COUNT BIGINT
);
```

## Using with SQL UI

1. Export your data:
   ```bash
   curl -X POST http://localhost:8080/api/export/direct \
     -H "Content-Type: application/json" \
     -d '{
       "indices": ["app-logs"],
       "query": "level:ERROR",
       "tableName": "errors"
     }'
   ```

2. Open SQL UI: http://localhost:8080/ui/sql.html

3. Query your table:
   ```sql
   SELECT * FROM ERRORS ORDER BY TIMESTAMP DESC LIMIT 50;
   ```

4. Join with other tables:
   ```sql
   SELECT e.USER, e.MESSAGE, u.email
   FROM ERRORS e
   LEFT JOIN users u ON e.USER = u.username;
   ```

## Database-Agnostic

The direct export works with **any JDBC database**:

- **H2** (default)
- **PostgreSQL**
- **MySQL/MariaDB**
- **Oracle**
- **SQL Server**
- Any other JDBC-compliant database

Just change your `application.properties` and the export will work the same way.

## Table Naming Rules

- Only alphanumeric, underscore (_), and dash (-)
- Max 64 characters
- Dashes converted to underscores
- Converted to UPPERCASE automatically

Examples:
- `my-logs` → `MY_LOGS`
- `error_logs_2024` → `ERROR_LOGS_2024`

## Column Naming

Column names are automatically sanitized:
- Converted to UPPERCASE
- Dashes and dots replaced with underscores

Examples:
- `user.name` → `USER_NAME`
- `response-time` → `RESPONSE_TIME`

## Performance

- Uses **batch inserts** (1000 rows per batch) for efficiency
- Auto-detects types from first 100 samples
- Handles large exports efficiently

## Comparison: Direct vs EAV Export

| Feature | Direct Export | EAV Export |
|---------|--------------|------------|
| Endpoint | `/api/export/direct` | `/api/export/results` |
| Table Structure | Real columns | Generic key-value |
| Query Complexity | Simple SELECT | Requires JOINs |
| SQL UI Compatible | ✅ Yes, directly | ⚠️ Complex queries |
| Type Detection | ✅ Automatic | ❌ All strings |
| Performance | ⚡ Fast | 🐌 Slower (joins) |
| Storage | 💾 Efficient | 💾 More space |

## Best Practices

1. **Use specific columns** when possible to reduce storage
2. **Sample large result sets** with `sampleSize`
3. **Use meaningful table names** that describe the data
4. **Set append=false** when refreshing data to avoid duplicates
5. **Query exported tables** through the SQL UI for easy analysis

## Limitations

- Table names must be unique
- Column values truncated at VARCHAR(4000) limit
- Cannot append to a table with different column structure
- AUTO_INCREMENT for ID column (H2 syntax - may need adjustment for other DBs)

## Troubleshooting

### Table already exists
```json
{
  "error": "Table already exists"
}
```
**Solution**: Use `"append": false` to replace, or choose a different table name.

### Invalid table name
```json
{
  "error": "Table name can only contain letters, numbers, underscores, and dashes"
}
```
**Solution**: Use only `[a-zA-Z0-9_-]` characters.

### No results found
```json
{
  "rowsExported": 0,
  "totalRows": 0
}
```
**Solution**: Check your query matches documents in the index.

## Examples with SQL UI

### Export and Analyze

```bash
# 1. Export error logs
curl -X POST http://localhost:8080/api/export/direct \
  -H "Content-Type: application/json" \
  -d '{
    "indices": ["app-logs"],
    "query": "level:ERROR",
    "tableName": "errors_last_week",
    "fields": ["timestamp", "user", "operation", "message"]
  }'

# 2. Open SQL UI
open http://localhost:8080/ui/sql.html

# 3. Analyze errors by user
```sql
SELECT user, COUNT(*) as error_count
FROM ERRORS_LAST_WEEK
GROUP BY user
ORDER BY error_count DESC;
```

### Multiple Exports and Joins

```bash
# Export errors
curl -X POST http://localhost:8080/api/export/direct \
  -H "Content-Type: application/json" \
  -d '{"indices": ["app-logs"], "query": "level:ERROR", "tableName": "errors"}'

# Export warnings
curl -X POST http://localhost:8080/api/export/direct \
  -H "Content-Type: application/json" \
  -d '{"indices": ["app-logs"], "query": "level:WARN", "tableName": "warnings"}'
```

Then in SQL UI:
```sql
-- Compare error and warning frequency by hour
SELECT 
  DATE_TRUNC('HOUR', timestamp) as hour,
  'ERROR' as level,
  COUNT(*) as count
FROM ERRORS
GROUP BY DATE_TRUNC('HOUR', timestamp)

UNION ALL

SELECT 
  DATE_TRUNC('HOUR', timestamp) as hour,
  'WARN' as level,
  COUNT(*) as count
FROM WARNINGS
GROUP BY DATE_TRUNC('HOUR', timestamp)

ORDER BY hour DESC;
```
