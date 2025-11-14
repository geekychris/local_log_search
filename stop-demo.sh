#!/bin/bash

# Local Log Search Demo Stop Script

echo "🛑 Stopping Local Log Search Demo..."

# Stop log generator
GENERATOR_PIDS=$(pgrep -f "test-log-generator.*jar" || true)
if [ -n "$GENERATOR_PIDS" ]; then
    echo "   Stopping log generator (PIDs: $GENERATOR_PIDS)..."
    kill $GENERATOR_PIDS
    echo "   ✓ Log generator stopped"
else
    echo "   Log generator not running"
fi

# Stop search service
SERVICE_PIDS=$(pgrep -f "log-search-service.*jar" || true)
if [ -n "$SERVICE_PIDS" ]; then
    echo "   Stopping search service (PIDs: $SERVICE_PIDS)..."
    kill $SERVICE_PIDS
    echo "   ✓ Search service stopped"
else
    echo "   Search service not running"
fi

echo ""
echo "✅ All services stopped"
