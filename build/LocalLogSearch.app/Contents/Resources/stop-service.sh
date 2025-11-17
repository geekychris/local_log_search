#!/bin/bash
PID_FILE="${HOME}/.local_log_search/service.pid"
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if kill -0 $PID 2>/dev/null; then
        echo "Stopping LocalLogSearch (PID: $PID)..."
        kill $PID
        rm "$PID_FILE"
        echo "Service stopped."
    else
        echo "Service not running (stale PID file removed)."
        rm "$PID_FILE"
    fi
else
    echo "Service is not running."
fi
