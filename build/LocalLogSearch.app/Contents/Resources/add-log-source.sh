#!/bin/bash

# Helper script to add log sources to LocalLogSearch
# This script helps users get the full absolute path and add log sources via API

set -e

echo "=========================================="
echo "LocalLogSearch - Add Log Source Helper"
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

# Get file path
echo "Enter the path to your log file:"
echo "(You can drag and drop the file here, or type/paste the path)"
read -r -p "File path: " FILE_PATH

# Remove any quotes that might have been added
FILE_PATH="${FILE_PATH//\"/}"
FILE_PATH="${FILE_PATH//\'/}"

# Expand tilde to home directory
if [[ "$FILE_PATH" == ~* ]]; then
    FILE_PATH="${FILE_PATH/#\~/$HOME}"
fi

# Convert to absolute path if relative
if [[ "$FILE_PATH" != /* ]]; then
    FILE_PATH="$(cd "$(dirname "$FILE_PATH")" && pwd)/$(basename "$FILE_PATH")"
fi

echo ""
echo "Resolved path: $FILE_PATH"

# Check if file exists
if [ ! -f "$FILE_PATH" ]; then
    echo "❌ Error: File does not exist: $FILE_PATH"
    exit 1
fi

# Check if file is readable
if [ ! -r "$FILE_PATH" ]; then
    echo "❌ Error: File is not readable: $FILE_PATH"
    echo "Try: chmod 644 $FILE_PATH"
    exit 1
fi

echo "✅ File exists and is readable"
echo ""

# Get index name
DEFAULT_INDEX=$(basename "$FILE_PATH" .log | sed 's/[^a-zA-Z0-9_-]/-/g')
read -r -p "Index name [$DEFAULT_INDEX]: " INDEX_NAME
INDEX_NAME=${INDEX_NAME:-$DEFAULT_INDEX}

echo ""

# Select parser type
echo "Select parser type:"
echo "  1) keyvalue (default) - Extracts key=value pairs"
echo "  2) regex - Custom regex pattern"
echo "  3) grok - Grok patterns (like Logstash)"
read -r -p "Parser type [1]: " PARSER_CHOICE
PARSER_CHOICE=${PARSER_CHOICE:-1}

case $PARSER_CHOICE in
    1)
        PARSER_TYPE="keyvalue"
        PARSER_CONFIG="null"
        ;;
    2)
        PARSER_TYPE="regex"
        echo ""
        echo "Enter regex pattern (e.g., \"(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z) \\[(\\w+)\\] (.*)\"):"
        read -r REGEX_PATTERN
        echo "Enter field names (comma-separated, e.g., \"timestamp,level,message\"):"
        read -r FIELD_NAMES
        
        # Build parser config
        IFS=',' read -ra FIELDS <<< "$FIELD_NAMES"
        CONFIG_FIELDS=""
        for i in "${!FIELDS[@]}"; do
            FIELD_NUM=$((i + 1))
            FIELD_NAME="${FIELDS[$i]}"
            CONFIG_FIELDS="$CONFIG_FIELDS, \"field.$FIELD_NUM\": \"$FIELD_NAME\""
        done
        
        PARSER_CONFIG="{\"pattern\": \"$REGEX_PATTERN\"$CONFIG_FIELDS}"
        ;;
    3)
        PARSER_TYPE="grok"
        echo ""
        echo "Enter Grok pattern (e.g., \"%{TIMESTAMP_ISO8601:timestamp} %{LOGLEVEL:level} %{GREEDYDATA:message}\"):"
        read -r GROK_PATTERN
        PARSER_CONFIG="{\"pattern\": \"$GROK_PATTERN\"}"
        ;;
    *)
        echo "Invalid choice, using keyvalue parser"
        PARSER_TYPE="keyvalue"
        PARSER_CONFIG="null"
        ;;
esac

echo ""
echo "Summary:"
echo "  File Path:   $FILE_PATH"
echo "  Index Name:  $INDEX_NAME"
echo "  Parser Type: $PARSER_TYPE"
if [ "$PARSER_CONFIG" != "null" ]; then
    echo "  Parser Config: $PARSER_CONFIG"
fi
echo ""

read -r -p "Add this log source? (y/n) " CONFIRM
if [[ ! "$CONFIRM" =~ ^[Yy]$ ]]; then
    echo "Cancelled."
    exit 0
fi

# Build JSON payload
if [ "$PARSER_CONFIG" = "null" ]; then
    JSON_PAYLOAD=$(cat <<EOF
{
  "filePath": "$FILE_PATH",
  "indexName": "$INDEX_NAME",
  "parserType": "$PARSER_TYPE",
  "enabled": true
}
EOF
)
else
    JSON_PAYLOAD=$(cat <<EOF
{
  "filePath": "$FILE_PATH",
  "indexName": "$INDEX_NAME",
  "parserType": "$PARSER_TYPE",
  "parserConfig": $PARSER_CONFIG,
  "enabled": true
}
EOF
)
fi

# Add the source via API
echo ""
echo "Adding log source..."
RESPONSE=$(curl -s -X POST http://localhost:8080/api/sources \
    -H "Content-Type: application/json" \
    -d "$JSON_PAYLOAD" \
    2>&1)

if [ $? -eq 0 ]; then
    echo "✅ Log source added successfully!"
    echo ""
    echo "The file will be tailed and indexed. Wait ~15 seconds for initial indexing."
    echo ""
    echo "To search your logs:"
    echo "  1. Open http://localhost:8080"
    echo "  2. Click 'Search' in the navigation"
    echo "  3. Enter index name: $INDEX_NAME"
    echo "  4. Enter query (e.g., '*' for all, 'ERROR' for errors)"
    echo "  5. Click 'Search'"
    echo ""
else
    echo "❌ Error adding log source:"
    echo "$RESPONSE"
    exit 1
fi
