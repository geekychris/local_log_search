#!/bin/bash

# Script to stop, rebuild, and restart the log search service

set -e  # Exit on error

echo "=== Stopping service ==="
pkill -f "log-search-service" || echo "No service running"
sleep 2

echo ""
echo "=== Cleaning lock files ==="
find ~/.local_log_search/indices -name "write.lock" -delete 2>/dev/null || true

echo ""
echo "=== Building project (including UI) ==="
mvn clean package -DskipTests

echo ""
echo "=== Starting service ==="
nohup java -jar log-search-service/target/log-search-service-1.0-SNAPSHOT.jar > service.log 2>&1 &
SERVICE_PID=$!
echo "Service started with PID: $SERVICE_PID"

echo ""
echo "=== Waiting for service to start ==="
sleep 5

echo ""
echo "=== Service logs (last 30 lines) ==="
tail -30 service.log

echo ""
echo "=== Service status ==="
if curl -s http://localhost:8080/api/indices > /dev/null; then
    echo "✓ Service is responding on http://localhost:8080"
else
    echo "✗ Service is not responding yet"
fi

echo ""
echo "Done! Service is running in background."
echo "To view logs: tail -f service.log"
echo "To stop: pkill -f log-search-service"
