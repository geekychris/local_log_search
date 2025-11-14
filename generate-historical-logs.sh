#!/bin/bash

# Generate Historical Logs Script
# Creates a week's worth of logs for testing time-based filtering

PROJECT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$PROJECT_DIR"

echo "🕐 Generating historical logs..."
echo ""

# Stop current generator if running
pkill -f test-log-generator 2>/dev/null

# Generate historical logs (7 days of data)
echo "Generating 10,000 logs spanning 7 days..."
java -jar test-log-generator/target/test-log-generator-1.0-SNAPSHOT-jar-with-dependencies.jar \
  --days-back 7 \
  --max-logs 10000

echo ""
echo "✅ Historical log generation complete!"
echo ""
echo "📊 Check the logs:"
echo "   Application: logs/application.log"
echo "   Access:      logs/access.log"
echo ""
echo "You can now test time-based queries in the search UI"
