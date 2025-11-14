#!/bin/bash

# Local Log Search Demo Startup Script
# This script starts the log generator and configures log sources

set -e

PROJECT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$PROJECT_DIR"

echo "🚀 Starting Local Log Search Demo..."
echo ""

# Create logs directory
mkdir -p logs

# Check if log generator is already running
if pgrep -f "test-log-generator.*jar" > /dev/null; then
    echo "⚠️  Log generator is already running"
else
    echo "📝 Starting log generator..."
    nohup java -jar test-log-generator/target/test-log-generator-1.0-SNAPSHOT-jar-with-dependencies.jar > generator.log 2>&1 &
    GENERATOR_PID=$!
    echo "   Log generator started (PID: $GENERATOR_PID)"
    sleep 3
fi

# Check if search service is already running
if pgrep -f "log-search-service.*jar" > /dev/null; then
    echo "✅ Search service is already running"
    SERVICE_RUNNING=true
else
    echo "🔍 Starting search service..."
    nohup java -jar log-search-service/target/log-search-service-1.0-SNAPSHOT.jar > service.log 2>&1 &
    SERVICE_PID=$!
    echo "   Search service started (PID: $SERVICE_PID)"
    echo "   Waiting for service to start..."
    sleep 10
    SERVICE_RUNNING=true
fi

# Configure log sources if service is running
if [ "$SERVICE_RUNNING" = true ]; then
    echo ""
    echo "📂 Configuring log sources..."
    
    # Check if sources already exist
    SOURCES=$(curl -s http://localhost:8080/api/sources 2>/dev/null || echo "[]")
    APP_LOG_EXISTS=$(echo "$SOURCES" | grep -c "application.log" || true)
    ACCESS_LOG_EXISTS=$(echo "$SOURCES" | grep -c "access.log" || true)
    ERROR_LOG_EXISTS=$(echo "$SOURCES" | grep -c "error.log" || true)
    
    if [ "$APP_LOG_EXISTS" -eq 0 ]; then
        echo "   Adding application.log..."
        curl -s -X POST http://localhost:8080/api/sources \
          -H "Content-Type: application/json" \
          -d "{
            \"filePath\": \"$PROJECT_DIR/logs/application.log\",
            \"indexName\": \"app-logs\",
            \"parserType\": \"keyvalue\",
            \"enabled\": true
          }" > /dev/null
    else
        echo "   ✓ application.log already configured"
    fi
    
    if [ "$ACCESS_LOG_EXISTS" -eq 0 ]; then
        echo "   Adding access.log..."
        curl -s -X POST http://localhost:8080/api/sources \
          -H "Content-Type: application/json" \
          -d "{
            \"filePath\": \"$PROJECT_DIR/logs/access.log\",
            \"indexName\": \"access-logs\",
            \"parserType\": \"keyvalue\",
            \"enabled\": true
          }" > /dev/null
    else
        echo "   ✓ access.log already configured"
    fi
    
    if [ "$ERROR_LOG_EXISTS" -eq 0 ]; then
        echo "   Adding error.log..."
        curl -s -X POST http://localhost:8080/api/sources \
          -H "Content-Type: application/json" \
          -d "{
            \"filePath\": \"$PROJECT_DIR/logs/error.log\",
            \"indexName\": \"error-logs\",
            \"parserType\": \"keyvalue\",
            \"enabled\": true
          }" > /dev/null
    else
        echo "   ✓ error.log already configured"
    fi
fi

echo ""
echo "✨ Demo is ready!"
echo ""
echo "📊 Access the UI:"
echo "   Search:      http://localhost:8080/ui/index.html"
echo "   Log Sources: http://localhost:8080/ui/sources.html"
echo ""
echo "📁 Log files:"
echo "   Application: $PROJECT_DIR/logs/application.log"
echo "   Access:      $PROJECT_DIR/logs/access.log"
echo "   Error:       $PROJECT_DIR/logs/error.log"
echo ""
echo "🔧 Manage processes:"
echo "   View generator: tail -f generator.log"
echo "   View service:   tail -f service.log"
echo "   Stop all:       ./stop-demo.sh"
echo ""
