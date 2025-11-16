# Export Strategy Change

## What Changed

The export feature now creates **real SQL tables** with actual columns instead of using the EAV (Entity-Attribute-Value) denormalized model.

## Before

Exports created data across multiple tables:
- `exported_tables` - metadata
- `table_columns` - column definitions  
- `exported_rows` - row metadata
- `row_data` - actual data in key-value pairs

**Querying required complex JOINs:**
```sql
SELECT er.timestamp, rd.field_value
FROM exported_rows er
JOIN row_data rd ON er.id = rd.row_id
WHERE er.table_name = 'error_logs' AND rd.field_name = 'user';
```

## After

Exports create normal SQL tables with real columns:

```sql
CREATE TABLE ERROR_LOGS (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  "TIMESTAMP" TIMESTAMP,
  "USER" VARCHAR(255),
  "LEVEL" VARCHAR(255),
  "MESSAGE" VARCHAR(4000)
);
```

**Simple queries:**
```sql
SELECT * FROM ERROR_LOGS WHERE "USER" = 'john';
```

## How to Use

### 1. Pipe Command (Unchanged Syntax)

```bash
level:ERROR | export table=my_errors
```

This now creates a real table `MY_ERRORS` with actual columns.

### 2. REST API (Unchanged Endpoint)

```bash
curl -X POST http://localhost:8080/api/export/results \
  -H "Content-Type: application/json" \
  -d '{
    "indices": ["app-logs"],
    "query": "level:ERROR",
    "tableName": "my_errors"
  }'
```

### 3. New Direct Endpoint (Explicit)

```bash
curl -X POST http://localhost:8080/api/export/direct \
  -H "Content-Type: application/json" \
  -d '{
    "indices": ["app-logs"],
    "query": "level:ERROR",
    "tableName": "my_errors"
  }'
```

**Both `/api/export/results` and `/api/export/direct` now do the same thing - create real tables.**

## Query Your Exports

### Via SQL UI

1. Open http://localhost:8080/ui/sql.html
2. Click on your table in the sidebar
3. Query directly:

```sql
SELECT * FROM MY_ERRORS ORDER BY "TIMESTAMP" DESC LIMIT 50;
```

### Via H2 Console

1. Open http://localhost:8080/h2-console
2. Connect (JDBC URL: `jdbc:h2:file:~/.local_log_search/database/logdb`)
3. Query:

```sql
SELECT * FROM MY_ERRORS;
```

## Features

### Automatic Type Detection

The system analyzes your data and picks appropriate types:
- Numeric values → `BIGINT` or `DOUBLE`
- Short text → `VARCHAR(255)`
- Medium text → `VARCHAR(1000)`  
- Long text → `VARCHAR(4000)`
- Timestamps → `TIMESTAMP`

### Reserved Keyword Handling

Column names are quoted to handle SQL reserved keywords:
- `USER` → `"USER"`
- `ERROR` → `"ERROR"`
- `TIMESTAMP` → `"TIMESTAMP"`

### Table Naming

- Alphanumeric, underscore, dash only
- Converted to UPPERCASE
- Dashes become underscores
- Example: `my-error-logs` → `MY_ERROR_LOGS`

## Examples

### Export All Error Logs

```bash
# Via search UI or API
level:ERROR | export table=all_errors
```

Creates table:
```sql
CREATE TABLE ALL_ERRORS (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  "TIMESTAMP" TIMESTAMP,
  "RAW_TEXT" VARCHAR(4000),
  "LEVEL" VARCHAR(255),
  "USER" VARCHAR(255),
  "MESSAGE" VARCHAR(4000),
  ...
);
```

### Export Specific Fields

```bash
level:ERROR | export table=error_summary fields=timestamp,user,message
```

Creates table with only those columns:
```sql
CREATE TABLE ERROR_SUMMARY (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  "TIMESTAMP" TIMESTAMP,
  "USER" VARCHAR(255),
  "MESSAGE" VARCHAR(4000)
);
```

### Sample Large Result Sets

```bash
*:* | export table=sample_data sample=1000
```

Exports only first 1000 results.

### Replace vs Append

```bash
# Replace existing table
level:ERROR | export table=errors append=false

# Append to existing table (default)
level:ERROR | export table=errors append=true
```

## Migration Notes

### Old EAV Tables

The old tables (`exported_tables`, `exported_rows`, `row_data`, `table_columns`) are still in the database but are **no longer used** by new exports.

You can:
1. **Keep them** - they won't interfere
2. **Query them** via SQL UI if you need old data
3. **Drop them** if you don't need the old exports:

```sql
DROP TABLE IF EXISTS exported_rows;
DROP TABLE IF EXISTS row_data;
DROP TABLE IF EXISTS table_columns;
DROP TABLE IF EXISTS exported_tables;
```

### Querying Old Exports

If you have old exports in the EAV format, you can still query them:

```sql
-- List old exports
SELECT * FROM exported_tables;

-- Query old export data (requires JOINs)
SELECT er.timestamp, rd.field_name, rd.field_value
FROM exported_rows er
JOIN row_data rd ON er.id = rd.row_id
WHERE er.table_name = 'old_table_name'
LIMIT 100;
```

## Benefits

| Aspect | Old (EAV) | New (Direct) |
|--------|-----------|--------------|
| Query Complexity | Complex JOINs | Simple SELECT |
| Performance | Slow (joins) | Fast (indexed) |
| SQL UI Compatible | ⚠️ Complex | ✅ Simple |
| Type Safety | ❌ All strings | ✅ Typed columns |
| Storage Efficiency | Lower | Higher |
| Database Compatibility | Any | Any JDBC |

## Troubleshooting

### Table already exists

If you get "table already exists", either:
- Use `append=true` to add data
- Use `append=false` to replace
- Choose a different table name

### Reserved keyword errors

Column names are automatically quoted. If you see errors, the system handles:
- USER → "USER"
- ERROR → "ERROR"  
- TIMESTAMP → "TIMESTAMP"
- etc.

### Type conversion errors

If a value can't be converted to the detected type, it falls back to VARCHAR.

## Testing

After rebuilding, test the export:

```bash
# 1. Start service
./start-service.sh

# 2. In another terminal, open the search UI
open http://localhost:8080/ui/index.html

# 3. Run a search with export:
#    Query: level:ERROR | export table=test_export

# 4. Open SQL UI
open http://localhost:8080/ui/sql.html

# 5. Query your table:
SELECT * FROM TEST_EXPORT LIMIT 10;
```

## Documentation

- **DIRECT_EXPORT.md** - Detailed API documentation
- **SQL_UI_GUIDE.md** - SQL UI usage guide
- **DATABASE_EXPORT.md** - Old EAV model documentation (deprecated)
