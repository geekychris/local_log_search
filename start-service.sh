#!/bin/bash

echo "Starting Local Log Search Service..."
echo ""
echo "The service will be available at:"
echo "  Web UI:  http://localhost:8080"
echo "  API:     http://localhost:8080/api"
echo ""
echo "Press Ctrl+C to stop."
echo ""

cd log-search-service
java -jar target/log-search-service-1.0-SNAPSHOT.jar
