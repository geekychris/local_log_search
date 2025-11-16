#!/bin/bash

# Test script for SQL UI functionality

BASE_URL="http://localhost:8080"
API_URL="${BASE_URL}/api/sql"

echo "Testing SQL Query UI API..."
echo ""

# Test 1: List tables
echo "1. Testing GET /api/sql/tables..."
curl -s "${API_URL}/tables" | jq '.' 2>/dev/null || echo "Failed - is the service running?"
echo ""

# Test 2: Execute a simple SELECT query
echo "2. Testing POST /api/sql/query (SELECT)..."
curl -s -X POST "${API_URL}/query" \
  -H "Content-Type: application/json" \
  -d '{"sql":"SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = '\''PUBLIC'\''"}' \
  | jq '.' 2>/dev/null || echo "Failed"
echo ""

# Test 3: Create a test table
echo "3. Testing POST /api/sql/query (CREATE TABLE)..."
curl -s -X POST "${API_URL}/query" \
  -H "Content-Type: application/json" \
  -d '{"sql":"CREATE TABLE IF NOT EXISTS test_table (id BIGINT, name VARCHAR(255), created_at TIMESTAMP)"}' \
  | jq '.' 2>/dev/null || echo "Failed"
echo ""

# Test 4: Insert test data
echo "4. Testing POST /api/sql/query (INSERT)..."
curl -s -X POST "${API_URL}/query" \
  -H "Content-Type: application/json" \
  -d '{"sql":"INSERT INTO test_table (id, name, created_at) VALUES (1, '\''Test Record'\'', CURRENT_TIMESTAMP)"}' \
  | jq '.' 2>/dev/null || echo "Failed"
echo ""

# Test 5: Query the test table
echo "5. Testing POST /api/sql/query (SELECT from test_table)..."
curl -s -X POST "${API_URL}/query" \
  -H "Content-Type: application/json" \
  -d '{"sql":"SELECT * FROM test_table"}' \
  | jq '.' 2>/dev/null || echo "Failed"
echo ""

# Test 6: Get table schema
echo "6. Testing GET /api/sql/tables/{tableName}/schema..."
curl -s "${API_URL}/tables/test_table/schema" | jq '.' 2>/dev/null || echo "Failed"
echo ""

# Test 7: Clean up - drop test table
echo "7. Testing POST /api/sql/query (DROP TABLE)..."
curl -s -X POST "${API_URL}/query" \
  -H "Content-Type: application/json" \
  -d '{"sql":"DROP TABLE IF EXISTS test_table"}' \
  | jq '.' 2>/dev/null || echo "Failed"
echo ""

echo "✅ SQL UI API tests complete!"
echo ""
echo "Access the web UI at: ${BASE_URL}/ui/sql.html"
echo "Access the H2 Console at: ${BASE_URL}/h2-console"
echo "  JDBC URL: jdbc:h2:file:~/.local_log_search/database/logdb"
echo "  Username: sa"
echo "  Password: (empty)"
