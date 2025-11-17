# SQL Query UI - Quick Access

## 🚀 Access the SQL UI

### Direct URL
```
http://localhost:8080/ui/sql.html
```

### Via Navigation
The SQL Query UI is now linked in the main navigation:

1. **From Search Page**: http://localhost:8080/ui/index.html
   - Click **"🗄️ SQL Query"** in the top navigation

2. **From Log Sources Page**: http://localhost:8080/ui/sources.html
   - Click **"🗄️ SQL Query"** in the top navigation

## 📊 What You Can Do

### Query Tab
- Execute SELECT queries
- View results in formatted tables
- Quick query buttons for common operations
- SQL formatter for readability

### Create Table Tab
- Visual table designer
- Add/remove columns with type selector
- Preview SQL before creating
- Supports common data types (BIGINT, VARCHAR, TIMESTAMP, etc.)

### Insert Data Tab
- Execute INSERT statements
- Supports single and batch inserts
- Immediate validation and feedback

### Tables Sidebar
- Browse all database tables
- See row counts
- Click to auto-generate SELECT queries
- Refresh button to update list

## 🔧 Database Configuration

The SQL UI connects to your H2 database:
- **Location**: `~/.local_log_search/database/logdb`
- **JDBC URL**: `jdbc:h2:file:~/.local_log_search/database/logdb`
- **Type**: File-based (persistent)
- **Credentials**: sa / (empty password)

## 🔄 Database-Agnostic Design

The SQL controller is designed to work with any JDBC-compliant database:
- Uses standard JDBC metadata APIs
- Handles different case sensitivity rules
- Filters system tables for major databases (H2, PostgreSQL, MySQL, Oracle, SQL Server)
- Converts database-specific types to JSON-serializable formats

To switch databases in the future, just update `application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/mydb
spring.datasource.driverClassName=org.postgresql.Driver
spring.datasource.username=myuser
spring.datasource.password=mypass
```

## 📚 Documentation

- **User Guide**: [SQL_UI_GUIDE.md](SQL_UI_GUIDE.md) - Complete usage guide with examples
- **Implementation**: [SQL_UI_SUMMARY.md](SQL_UI_SUMMARY.md) - Technical details and architecture
- **Database Export**: [DATABASE_EXPORT.md](DATABASE_EXPORT.md) - Schema and export features

## 🧪 Test the API

Run the test script:
```bash
./test_sql_ui.sh
```

Or test manually:
```bash
# List tables
curl http://localhost:8080/api/sql/tables

# Execute a query
curl -X POST http://localhost:8080/api/sql/query \
  -H "Content-Type: application/json" \
  -d '{"sql":"SELECT * FROM exported_tables"}'
```

## 🎯 Quick Examples

### Example 1: Query Exported Tables
```sql
SELECT table_name, row_count, created_at 
FROM exported_tables 
ORDER BY created_at DESC;
```

### Example 2: Create a Custom Table
```sql
CREATE TABLE my_metrics (
  id BIGINT,
  metric_name VARCHAR(255),
  metric_value DOUBLE,
  created_at TIMESTAMP
);
```

### Example 3: Insert Data
```sql
INSERT INTO my_metrics (id, metric_name, metric_value, created_at)
VALUES (1, 'cpu_usage', 87.5, CURRENT_TIMESTAMP);
```

### Example 4: Query with Joins
```sql
SELECT er.timestamp, rd.field_name, rd.field_value
FROM exported_rows er
JOIN row_data rd ON er.id = rd.row_id
WHERE er.table_name = 'error_logs'
LIMIT 100;
```

## 🛑 Stop the Service

```bash
# Find the process
lsof -ti:8080

# Stop it
kill -9 $(lsof -ti:8080)
```

## 🔄 Restart After Changes

```bash
# Rebuild
mvn clean package -DskipTests

# Stop existing service
lsof -ti:8080 | xargs kill -9

# Start service
./start-service.sh
```

## 💡 Tips

1. **Use LIMIT**: Always add LIMIT to queries on large tables
2. **Format SQL**: Click the "Format" button for better readability
3. **Click Tables**: Click any table in the sidebar to auto-generate a query
4. **Quick Queries**: Use the pre-built query buttons for common operations
5. **Preview First**: Use "Preview SQL" button before creating tables
