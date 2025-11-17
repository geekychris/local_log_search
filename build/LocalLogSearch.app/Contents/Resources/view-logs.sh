#!/bin/bash
LOG_FILE="${HOME}/.local_log_search/logs/app.log"
if [ -f "$LOG_FILE" ]; then
    tail -f "$LOG_FILE"
else
    echo "No log file found at $LOG_FILE"
fi
