#!/bin/bash

# Test script for multi-stage pipeline functionality
# Tests: query | filter | stats | chart

BASE_URL="http://localhost:8080"

echo "===== Testing Multi-Stage Pipeline Functionality ====="
echo ""

# Test 1: Basic stats command
echo "Test 1: Basic stats command"
echo "Query: * | stats count by level"
curl -s -X POST "${BASE_URL}/api/search" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "* | stats count by level",
    "indices": ["test-logs"],
    "page": 0,
    "pageSize": 100
  }' | jq -r '.type, .columns, .rows[] | "\(.level): \(.count)"'
echo ""
echo "---"
echo ""

# Test 2: Stats with filter
echo "Test 2: Stats with filter on results"
echo "Query: * | stats count by level | filter count > 50"
curl -s -X POST "${BASE_URL}/api/search" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "* | stats count by level | filter count > 50",
    "indices": ["test-logs"],
    "page": 0,
    "pageSize": 100
  }' | jq -r '.type, .columns, .rows[]'
echo ""
echo "---"
echo ""

# Test 3: Stats with filter and chart
echo "Test 3: Stats | filter | chart"
echo "Query: * | stats count by level | filter count > 50 | chart type=bar count by level"
curl -s -X POST "${BASE_URL}/api/search" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "* | stats count by level | filter count > 50 | chart type=bar count by level",
    "indices": ["test-logs"],
    "page": 0,
    "pageSize": 100
  }' | jq '.'
echo ""
echo "---"
echo ""

# Test 4: Stats with multiple aggregations and filter
echo "Test 4: Multi-aggregation stats with filter"
echo "Query: * | stats count avg(duration) by user | filter count > 10"
curl -s -X POST "${BASE_URL}/api/search" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "* | stats count avg(duration) by user | filter count > 10",
    "indices": ["test-logs"],
    "page": 0,
    "pageSize": 100
  }' | jq -r '.type, .columns, .rows[]'
echo ""
echo "---"
echo ""

# Test 5: Direct chart without intermediate filter
echo "Test 5: Direct stats to chart"
echo "Query: * | stats count by level | chart type=pie count by level"
curl -s -X POST "${BASE_URL}/api/search" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "* | stats count by level | chart type=pie count by level",
    "indices": ["test-logs"],
    "page": 0,
    "pageSize": 100
  }' | jq '.type, .chartType, .labels, .series'
echo ""
echo "---"
echo ""

# Test 6: Filter logs before stats
echo "Test 6: Filter logs before stats"
echo "Query: level:ERROR | filter duration > 100 | stats count by user"
curl -s -X POST "${BASE_URL}/api/search" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "level:ERROR | filter duration > 100 | stats count by user",
    "indices": ["test-logs"],
    "page": 0,
    "pageSize": 100
  }' | jq -r '.type, .columns, .rows[]'
echo ""

echo ""
echo "===== All tests completed ====="
