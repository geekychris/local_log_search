#!/bin/bash
# Test script for database export functionality

BASE_URL="http://localhost:8080"

echo "=========================================="
echo "Database Export Feature Test"
echo "=========================================="
echo ""

# Wait for service to be ready
echo "Checking if service is running..."
if ! curl -s -f "${BASE_URL}/api/sources" > /dev/null 2>&1; then
    echo "ERROR: Service is not running at ${BASE_URL}"
    echo "Please start the service first:"
    echo "  cd log-search-service"
    echo "  java -jar target/log-search-service-1.0-SNAPSHOT.jar"
    exit 1
fi
echo "✓ Service is running"
echo ""

# Test 1: Export using pipe command
echo "Test 1: Export using pipe command"
echo "Query: level:ERROR | export table=error_logs sample=10"
curl -s -X POST "${BASE_URL}/api/export/results" \
  -H "Content-Type: application/json" \
  -d '{
    "indices": ["app-logs"],
    "query": "level:ERROR | export table=error_logs sample=10",
    "tableName": "error_logs"
  }' | jq .

echo ""
echo "---"
echo ""

# Test 2: Export with specific fields
echo "Test 2: Export with specific fields"
echo "Query: level:ERROR"
curl -s -X POST "${BASE_URL}/api/export/results" \
  -H "Content-Type: application/json" \
  -d '{
    "indices": ["app-logs"],
    "query": "level:ERROR",
    "tableName": "error_fields_test",
    "fields": ["timestamp", "user", "operation", "message"],
    "sampleSize": 5,
    "append": false
  }' | jq .

echo ""
echo "---"
echo ""

# Test 3: List all exported tables
echo "Test 3: List all exported tables"
curl -s "${BASE_URL}/api/export/tables" | jq .

echo ""
echo "---"
echo ""

# Test 4: Get metadata for a specific table
echo "Test 4: Get metadata for error_logs table"
curl -s "${BASE_URL}/api/export/tables/error_logs" | jq .

echo ""
echo "---"
echo ""

# Test 5: Query rows from exported table
echo "Test 5: Query rows from error_logs table (page 0, size 3)"
curl -s "${BASE_URL}/api/export/tables/error_logs/rows?page=0&size=3" | jq .

echo ""
echo "---"
echo ""

# Test 6: Export slow operations
echo "Test 6: Export slow operations"
curl -s -X POST "${BASE_URL}/api/export/results" \
  -H "Content-Type: application/json" \
  -d '{
    "indices": ["app-logs"],
    "query": "status:slow",
    "tableName": "slow_operations",
    "fields": ["timestamp", "operation", "duration", "user"],
    "sampleSize": 10
  }' | jq .

echo ""
echo "---"
echo ""

# Test 7: List tables again to see both tables
echo "Test 7: List all exported tables (should show multiple tables)"
curl -s "${BASE_URL}/api/export/tables" | jq .

echo ""
echo "=========================================="
echo "Tests Complete!"
echo "=========================================="
echo ""
echo "You can also:"
echo "1. Access H2 Console: ${BASE_URL}/h2-console"
echo "   JDBC URL: jdbc:h2:file:~/.local_log_search/database/logdb"
echo "   Username: sa"
echo "   Password: (leave empty)"
echo ""
echo "2. Query exported data via SQL:"
echo "   SELECT * FROM exported_tables;"
echo "   SELECT * FROM exported_rows WHERE table_name = 'error_logs';"
echo ""
echo "3. Delete a table:"
echo "   curl -X DELETE ${BASE_URL}/api/export/tables/error_logs"
echo ""
