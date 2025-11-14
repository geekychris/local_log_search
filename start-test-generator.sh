#!/bin/bash

# Create logs directory
mkdir -p test-log-generator/logs

echo "Starting Test Log Generator..."
echo "Logs will be written to: test-log-generator/logs/"
echo ""
echo "This will generate:"
echo "  - application.log (mixed format with key=value)"
echo "  - access.log (HTTP access logs)"
echo "  - error.log (error logs with bursts)"
echo ""
echo "Press Ctrl+C to stop."
echo ""

cd test-log-generator
java -jar target/test-log-generator-1.0-SNAPSHOT-jar-with-dependencies.jar
