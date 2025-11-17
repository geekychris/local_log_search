#!/bin/bash

set -e

# Default values
MAX_LOGS=""
DELAY=""
START_DATE=""
DAYS_BACK=""
CONFIG_SEARCH=false
TRUNCATE_INDEX=false
LOG_FILE=""

# Function to print usage
print_usage() {
    cat <<EOF
Usage: $0 [OPTIONS]

Starts the test log generator with optional configuration.

Options:
  --max-logs <N>         Generate N logs then stop (default: unlimited)
  --delay <ms>           Delay in milliseconds between logs (default: 0)
  --start-date <ISO>     Start with specific date (ISO format: 2025-01-01T00:00:00Z)
  --days-back <N>        Start N days ago and increment timestamps
  --log-file <path>      Tail specific log file with --config-search (default: all 3 log files)
  --config-search        Configure search service to tail generated log files
  --truncate-index       Truncate existing indices before configuring search (requires --config-search)
  --help                 Show this help message

Examples:
  # Generate unlimited logs in real-time
  $0

  # Generate 10000 logs
  $0 --max-logs 10000

  # Generate logs slowly (1 per second)
  $0 --delay 1000

  # Generate historical logs from 30 days ago
  $0 --days-back 30 --max-logs 50000

  # Generate logs and configure search service to tail them
  $0 --config-search

  # Generate logs, truncate existing indices, and configure search service
  $0 --config-search --truncate-index

  # Configure only a specific log file
  $0 --config-search --log-file application.log

  # Configure specific log file with truncation
  $0 --config-search --truncate-index --log-file error.log

EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --max-logs)
            MAX_LOGS="$2"
            shift 2
            ;;
        --delay)
            DELAY="$2"
            shift 2
            ;;
        --start-date)
            START_DATE="$2"
            shift 2
            ;;
        --days-back)
            DAYS_BACK="$2"
            shift 2
            ;;
        --log-file)
            LOG_FILE="$2"
            shift 2
            ;;
        --config-search)
            CONFIG_SEARCH=true
            shift
            ;;
        --truncate-index)
            TRUNCATE_INDEX=true
            shift
            ;;
        --help)
            print_usage
            exit 0
            ;;
        *)
            echo "Error: Unknown option $1"
            echo ""
            print_usage
            exit 1
            ;;
    esac
done

# Validate truncate-index requires config-search
if [ "$TRUNCATE_INDEX" = true ] && [ "$CONFIG_SEARCH" = false ]; then
    echo "Error: --truncate-index requires --config-search"
    exit 1
fi

# Create logs directory
mkdir -p test-log-generator/logs

# Build Java arguments
JAVA_ARGS=""
if [ -n "$MAX_LOGS" ]; then
    JAVA_ARGS="$JAVA_ARGS --max-logs $MAX_LOGS"
fi
if [ -n "$DELAY" ]; then
    JAVA_ARGS="$JAVA_ARGS --delay $DELAY"
fi
if [ -n "$START_DATE" ]; then
    JAVA_ARGS="$JAVA_ARGS --start-date $START_DATE"
fi
if [ -n "$DAYS_BACK" ]; then
    JAVA_ARGS="$JAVA_ARGS --days-back $DAYS_BACK"
fi

# Configure search service if requested
if [ "$CONFIG_SEARCH" = true ]; then
    echo "=========================================="
    echo "Configuring Search Service"
    echo "=========================================="
    echo ""
    
    # Check if service is running
    if ! curl -s http://localhost:8080/api/sources > /dev/null 2>&1; then
        echo "❌ Error: LocalLogSearch service is not running on port 8080"
        echo ""
        echo "Please start the service first:"
        echo "  - If using the Mac app: Open LocalLogSearch.app"
        echo "  - If using JAR directly: ./start-service.sh"
        echo ""
        exit 1
    fi
    
    # Get absolute path to logs directory
    LOG_DIR="$(cd test-log-generator/logs && pwd)"
    
    # Determine which log files to configure
    if [ -n "$LOG_FILE" ]; then
        LOG_FILES=("$LOG_FILE")
    else
        LOG_FILES=("application.log" "access.log" "error.log")
    fi
    
    # Configure each log file
    for LOG_FILE_NAME in "${LOG_FILES[@]}"; do
        LOG_PATH="$LOG_DIR/$LOG_FILE_NAME"
        INDEX_NAME=$(echo "$LOG_FILE_NAME" | sed 's/.log$//' | sed 's/[^a-zA-Z0-9_-]/-/g')
        
        echo "Configuring $LOG_FILE_NAME..."
        
        # Check if log file exists
        if [ ! -f "$LOG_PATH" ]; then
            echo "  ⚠️  Log file does not exist yet: $LOG_FILE_NAME (will be created when generator starts)"
        fi
        
        # Truncate index if requested
        if [ "$TRUNCATE_INDEX" = true ]; then
            echo "  Truncating index: $INDEX_NAME"
            curl -s -X POST "http://localhost:8080/api/indices/$INDEX_NAME/truncate" > /dev/null 2>&1 || true
        fi
        
        # Check if source already exists
        EXISTING=$(curl -s "http://localhost:8080/api/sources" | grep -c "\"$LOG_PATH\"" || true)
        
        if [ "$EXISTING" -gt 0 ]; then
            echo "  ⚠️  Source already exists: $LOG_FILE_NAME (skipping)"
        else
            # Add the source
            JSON_PAYLOAD=$(cat <<EOF
{
  "filePath": "$LOG_PATH",
  "indexName": "$INDEX_NAME",
  "parserType": "keyvalue",
  "enabled": true
}
EOF
)
            
            RESPONSE=$(curl -s -X POST http://localhost:8080/api/sources \
                -H "Content-Type: application/json" \
                -d "$JSON_PAYLOAD" \
                2>&1)
            
            if [ $? -eq 0 ]; then
                echo "  ✅ Configured: $LOG_FILE_NAME -> index '$INDEX_NAME'"
            else
                echo "  ❌ Error configuring $LOG_FILE_NAME: $RESPONSE"
            fi
        fi
        echo ""
    done
    
    echo "Search service configured. Wait ~15 seconds for initial indexing."
    echo ""
    echo "To search your logs:"
    echo "  1. Open http://localhost:8080"
    echo "  2. Click 'Search' in the navigation"
    echo "  3. Select index: application, access, or error"
    echo "  4. Enter query (e.g., '*' for all, 'ERROR' for errors)"
    echo "  5. Click 'Search'"
    echo ""
    echo "=========================================="
    echo ""
fi

echo "Starting Test Log Generator..."
echo "Logs will be written to: test-log-generator/logs/"
echo ""
echo "This will generate:"
echo "  - application.log (mixed format with key=value)"
echo "  - access.log (HTTP access logs)"
echo "  - error.log (error logs with bursts)"
echo ""
if [ -n "$MAX_LOGS" ]; then
    echo "Max logs: $MAX_LOGS"
fi
if [ -n "$DELAY" ]; then
    echo "Delay between logs: ${DELAY}ms"
fi
if [ -n "$START_DATE" ]; then
    echo "Starting from date: $START_DATE"
fi
if [ -n "$DAYS_BACK" ]; then
    echo "Starting from: $DAYS_BACK days ago"
fi
echo ""
echo "Press Ctrl+C to stop."
echo ""

cd test-log-generator
java -jar target/test-log-generator-1.0-SNAPSHOT-jar-with-dependencies.jar $JAVA_ARGS
