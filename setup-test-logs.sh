#!/bin/bash

BASE_DIR=$(pwd)

echo "Setting up test log sources..."
echo ""

# Wait a moment to ensure service is up
sleep 2

# Add application log
echo "Adding application log source..."
curl -X POST http://localhost:8080/api/sources \
  -H "Content-Type: application/json" \
  -d "{
    \"filePath\": \"${BASE_DIR}/test-log-generator/logs/application.log\",
    \"indexName\": \"app-logs\",
    \"parserType\": \"keyvalue\"
  }"
echo ""

# Add access log
echo "Adding access log source..."
curl -X POST http://localhost:8080/api/sources \
  -H "Content-Type: application/json" \
  -d "{
    \"filePath\": \"${BASE_DIR}/test-log-generator/logs/access.log\",
    \"indexName\": \"access-logs\",
    \"parserType\": \"keyvalue\"
  }"
echo ""

# Add error log
echo "Adding error log source..."
curl -X POST http://localhost:8080/api/sources \
  -H "Content-Type: application/json" \
  -d "{
    \"filePath\": \"${BASE_DIR}/test-log-generator/logs/error.log\",
    \"indexName\": \"error-logs\",
    \"parserType\": \"keyvalue\"
  }"
echo ""

echo ""
echo "✓ Log sources configured successfully!"
echo ""
echo "Wait 15-30 seconds for initial indexing, then try searching:"
echo "  Web UI: http://localhost:8080"
echo "  API: curl -X POST http://localhost:8080/api/search -H 'Content-Type: application/json' -d '{\"indices\": [\"app-logs\"], \"query\": \"level:ERROR\", \"maxResults\": 10}'"
