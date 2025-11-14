# Local Log Search - Demo Guide

## Quick Start

### 1. Start the Demo

```bash
./start-demo.sh
```

This script will:
- ✅ Start the test log generator (creates continuous logs)
- ✅ Start the search service (indexes and searches logs)
- ✅ Configure 3 log sources automatically:
  - `logs/application.log` → `app-logs` index
  - `logs/access.log` → `access-logs` index
  - `logs/error.log` → `error-logs` index

### 2. Access the UI

**Search Interface:**
http://localhost:8080/ui/index.html

**Log Sources Management:**
http://localhost:8080/ui/sources.html

### 3. Stop the Demo

```bash
./stop-demo.sh
```

---

## What's Running

### Test Log Generator
**Purpose:** Generates realistic log entries continuously

**Output Files:**
- `logs/application.log` - Application logs with operations, users, performance metrics
- `logs/access.log` - HTTP access logs with endpoints, methods, response codes
- `logs/error.log` - Error logs (generated occasionally in bursts)

**Log Format:** Key-value pairs (e.g., `timestamp=... level=INFO user=john operation=login duration=125ms`)

**Monitoring:** `tail -f generator.log`

### Search Service
**Purpose:** Indexes logs in real-time and provides search/analytics

**Features:**
- Real-time log indexing (tails log files)
- Full-text search with Lucene
- Field-based filtering
- Aggregations (stats, charts, timecharts)
- Time range selection
- Query sharing

**Monitoring:** `tail -f service.log`

---

## Using the Search Interface

### Basic Searches

**Find all INFO logs:**
```
level:INFO
```

**Find slow operations:**
```
status:slow
```

**Find logs for specific user:**
```
user:john
```

**Find operations taking > 400ms:**
```
duration:[400 TO *]
```

### Aggregation Queries

**Count by log level:**
```
level:* | stats count by level
```

**Top 10 slowest operations:**
```
status:slow | stats max(duration) by operation | sort max(duration) desc | head 10
```

**Operations per user:**
```
* | stats count by user, operation
```

**Timeline of error rates:**
```
level:ERROR | timechart span=1m count
```

### Advanced Features

**Time Range Selection:**
- Click "🕐 Select Time Range" button
- Choose start/end times
- Or select a region on the timechart (zoom in)

**Share Queries:**
- Click "📋 Share Query" button
- Copy URL or cURL command
- Share with team members

---

## Managing Log Sources

### Via UI (Recommended)

1. Go to http://localhost:8080/ui/sources.html
2. Click "Browse" to select a log file
3. Click "Load Preview" to see sample lines
4. Uncheck banner/header lines
5. Select representative log entries
6. Choose parser type and configuration
7. See live preview of how lines will be parsed
8. Click "Add Source" when satisfied

### Via API

**Add a new log source:**
```bash
curl -X POST http://localhost:8080/api/sources \
  -H "Content-Type: application/json" \
  -d '{
    "filePath": "/path/to/logfile.log",
    "indexName": "my-logs",
    "parserType": "keyvalue",
    "enabled": true
  }'
```

**List all sources:**
```bash
curl http://localhost:8080/api/sources | jq .
```

**Delete a source:**
```bash
curl -X DELETE http://localhost:8080/api/sources/{source-id}
```

---

## Parser Types

### 1. Key-Value (Default)
**Best for:** Logs with `key=value` pairs

**Example:**
```
timestamp=2025-01-15T10:30:00Z level=INFO user=john message="Login successful"
```

**Extracted fields:**
- timestamp: 2025-01-15T10:30:00Z
- level: INFO
- user: john
- message: Login successful

### 2. Regex
**Best for:** Structured logs with custom formats

**Configuration:**
```json
{
  "pattern": "^(?<timestamp>\\S+) (?<level>\\S+) (?<message>.*)$",
  "field.1": "timestamp",
  "field.2": "level",
  "field.3": "message"
}
```

### 3. Grok
**Best for:** Common log formats (Apache, Nginx, etc.)

**Configuration:**
```json
{
  "pattern": "%{COMBINEDAPACHELOG}"
}
```

---

## Index Management

### Via UI

Go to Log Sources page → **Index Metrics** section

**Features:**
- View document counts and sizes
- Clear indices (delete all documents)
- Delete indices completely

### Via API

**List indices with metrics:**
```bash
curl http://localhost:8080/api/indices | jq .
```

**Clear an index:**
```bash
curl -X POST http://localhost:8080/api/indices/app-logs/clear
```

**Delete an index:**
```bash
curl -X DELETE http://localhost:8080/api/indices/app-logs
```

---

## Sample Queries to Try

### Application Logs

**Slow operations by user:**
```
status:slow | stats count by user
```

**Average duration by operation:**
```
* | stats avg(duration) by operation
```

**Error rate over time:**
```
level:ERROR | timechart span=5m count
```

### Access Logs

**Status code distribution:**
```
* | stats count by status
```

**Top endpoints:**
```
* | stats count by endpoint | sort count desc | head 10
```

**Response time trends:**
```
* | timechart span=1m avg(responseTime)
```

**4xx and 5xx errors:**
```
status:[400 TO 599] | stats count by status, endpoint
```

---

## Troubleshooting

### No logs appearing in search

**Issue:** Logs generated before adding sources won't be indexed

**Solution:** The tailer starts from the end of existing files. Wait for new logs to be generated (happens continuously), or clear and restart:
```bash
./stop-demo.sh
rm logs/*.log
./start-demo.sh
```

### Service won't start

**Issue:** Port 8080 already in use

**Solution:** Check for other services using the port:
```bash
lsof -i :8080
```

### Parser not extracting fields correctly

**Issue:** Log format doesn't match parser configuration

**Solution:** Use the preview feature in Log Sources UI:
1. Load sample lines from your log
2. Select representative entries
3. Adjust parser type/config
4. See live preview of extracted fields

---

## Log File Locations

All logs are stored in the `logs/` directory:

```
local_log_search/
├── logs/
│   ├── application.log  # Generated app logs
│   ├── access.log        # Generated access logs
│   └── error.log         # Generated error logs
├── service.log           # Search service output
├── generator.log         # Log generator output
└── data/                 # Lucene indices (auto-created)
    ├── app-logs/
    ├── access-logs/
    └── error-logs/
```

---

## Manual Operations

### Run log generator only:
```bash
java -jar test-log-generator/target/test-log-generator-1.0-SNAPSHOT-jar-with-dependencies.jar
```

### Run search service only:
```bash
java -jar log-search-service/target/log-search-service-1.0-SNAPSHOT.jar
```

### View running processes:
```bash
ps aux | grep java
```

### Kill specific process:
```bash
kill <PID>
```

---

## Next Steps

1. **Try different queries** - Experiment with Lucene syntax and pipe commands
2. **Add your own logs** - Point the service at real application logs
3. **Create custom parsers** - Use regex or grok for specialized formats
4. **Build dashboards** - Use timechart queries for monitoring
5. **Share queries** - Collaborate with team using shareable URLs

Enjoy exploring your logs! 🚀
