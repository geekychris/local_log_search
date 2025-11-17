# Quick Start Guide

Get up and running with Local Log Search in 5 minutes!

## Step 1: Build the Project

```bash
mvn clean package
```

This creates:
- `log-search-core/target/log-search-core-1.0-SNAPSHOT.jar`
- `log-search-service/target/log-search-service-1.0-SNAPSHOT.jar`
- `test-log-generator/target/test-log-generator-1.0-SNAPSHOT.jar`

## Step 2: Start the Test Log Generator

In one terminal:

```bash
./start-test-generator.sh
```

This generates sample logs in `test-log-generator/logs/`:
- **application.log** - Application logs with key=value format
- **access.log** - HTTP access logs  
- **error.log** - Error logs with occasional bursts

Leave this running!

## Step 3: Start the Service

In another terminal:

```bash
./start-service.sh
```

Wait for the message: "Started LocalLogSearchApplication"

The service is now running at `http://localhost:8080`

## Step 4: Configure Log Sources

In a third terminal, run the setup script:

```bash
./setup-test-logs.sh
```

This automatically configures the three test log files.

## Step 5: Search!

### Option A: Web UI

1. Open `http://localhost:8080` in your browser
2. You'll see the Search view
3. Enter index name: `app-logs`
4. Enter query: `level:ERROR`
5. Click "Search"

### Option B: REST API

```bash
# Find all errors
curl -X POST http://localhost:8080/api/search \
  -H "Content-Type: application/json" \
  -d '{
    "indices": ["app-logs"],
    "query": "level:ERROR",
    "maxResults": 10
  }' | jq
```

## More Search Examples

### Find slow operations
```bash
curl -X POST http://localhost:8080/api/search \
  -H "Content-Type: application/json" \
  -d '{
    "indices": ["app-logs"],
    "query": "status:slow",
    "maxResults": 10
  }' | jq '.results[].rawText'
```

### Find specific user activity
```bash
curl -X POST http://localhost:8080/api/search \
  -H "Content-Type: application/json" \
  -d '{
    "indices": ["app-logs"],
    "query": "user:admin",
    "maxResults": 10
  }' | jq '.results[].rawText'
```

### Find HTTP errors (500+)
```bash
curl -X POST http://localhost:8080/api/search \
  -H "Content-Type: application/json" \
  -d '{
    "indices": ["access-logs"],
    "query": "status:[500 TO 599]",
    "maxResults": 10
  }' | jq '.results[].rawText'
```

### Search across multiple indices
```bash
curl -X POST http://localhost:8080/api/search \
  -H "Content-Type: application/json" \
  -d '{
    "indices": ["app-logs", "access-logs", "error-logs"],
    "query": "ERROR",
    "maxResults": 20
  }' | jq '.results[] | {index: .indexName, time: .timestamp, log: .rawText}'
```

## Using the Web UI

### Adding Log Sources

1. Click "Log Sources" in the sidebar
2. Click "Add Log Source"
3. Fill in:
   - **File Path**: Full path to your log file
   - **Index Name**: Logical name for the index (e.g., "nginx-logs")
   - **Parser Type**: Choose "keyvalue" for most logs
4. Click "Save"

### Searching

1. Click "Search" in the sidebar
2. Enter **Indices**: Comma-separated list (e.g., "app-logs, access-logs")
3. Enter **Query**: Use Lucene syntax
   - Simple: `ERROR`
   - Field: `level:ERROR`
   - Boolean: `level:ERROR AND user:admin`
   - Range: `duration:[100 TO 500]`
   - Wildcard: `user:john*`
4. Click "Search"

Results show:
- **Timestamp**: When the log entry was created
- **Source**: Which file it came from
- **Log Entry**: The raw log text
- **Score**: Relevance score

## Your Own Logs

To index your own log files:

### Via Web UI

1. Go to `http://localhost:8080` → "Log Sources"
2. Click "Add Log Source"
3. Enter your log file path
4. Choose a parser:
   - **keyvalue**: For logs like `key1=value1 key2=value2`
   - **regex**: For custom patterns
   - **grok**: For Logstash-style patterns

### Via REST API

```bash
curl -X POST http://localhost:8080/api/sources \
  -H "Content-Type: application/json" \
  -d '{
    "filePath": "/path/to/your/app.log",
    "indexName": "myapp",
    "parserType": "keyvalue"
  }'
```

Wait 15-30 seconds for indexing, then search!

## Troubleshooting

### "No results found"
- Wait 15-30 seconds after adding log source (default commit interval)
- Check that the log file exists and has content
- Verify the index name matches what you configured

### Service won't start
- Check port 8080 is available: `lsof -i :8080`
- Check Java version: `java -version` (needs Java 21+)

### Logs not being indexed
- Verify file path is absolute and correct
- Check file permissions (service needs read access)
- Look at service console output for errors

## Next Steps

- Read the full [README.md](README.md) for detailed documentation
- Check [API.md](API.md) for complete REST API reference
- Customize `application.properties` to change:
  - Commit interval (default 15s)
  - Index location
  - Log levels

## Tips

- **Performance**: Adjust commit interval in `application.properties`
  - Lower = more real-time, higher I/O
  - Higher = better performance, less real-time

- **Organization**: Use separate indices for different log types
  - Better search performance
  - Easier to manage

- **Queries**: Start simple and add complexity
  - `ERROR` → `level:ERROR` → `level:ERROR AND user:admin`

Enjoy searching your logs! 🔍
