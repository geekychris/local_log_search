# Reindex Guide

## Overview

The reindex feature allows you to clear an existing index and re-read/re-index a log file from the beginning. This is useful when:

- You want to reprocess logs with updated parser configurations
- You need to rebuild an index that may have become corrupted
- You want to start fresh after testing or debugging
- You've modified field extraction patterns and want to apply them to existing logs

## How It Works

The reindex mechanism performs three operations:

1. **Clear the Index**: Deletes all documents from the Lucene index
2. **Remove Tailer State**: Deletes the checkpoint that tracks the file reading position
3. **Restart Tailer**: Starts a new tailer from the beginning of the log file

## REST API

### Reindex a Log Source

```bash
POST /api/sources/{sourceId}/reindex
```

**Parameters:**
- `sourceId` (path): The ID of the log source to reindex

**Response:**
```json
{
  "message": "Reindexing started for source: {sourceId}",
  "indexName": "{indexName}",
  "filePath": "{filePath}"
}
```

**Error Response:**
```json
{
  "error": "Failed to reindex: {error message}"
}
```

### Example Usage

```bash
# Reindex a log source with ID "my-app-logs"
curl -X POST http://localhost:8080/api/sources/my-app-logs/reindex

# Response
{
  "message": "Reindexing started for source: my-app-logs",
  "indexName": "my_app_logs",
  "filePath": "/var/log/myapp.log"
}
```

## Implementation Details

### IndexManager.clearIndex()

Located in `log-search-core/src/main/java/com/locallogsearch/core/index/IndexManager.java`

```java
public void clearIndex(String indexName) throws IOException
```

- Closes the existing IndexWriter for the specified index
- Opens the index directory
- Calls `deleteAll()` on a new IndexWriter
- Commits the changes to persist the empty index

### TailerManager.reindexLogSource()

Located in `log-search-core/src/main/java/com/locallogsearch/core/tailer/TailerManager.java`

```java
public void reindexLogSource(LogSourceConfig config)
```

- Removes the existing tailer for the log source
- Creates a new tailer with no initial state (null)
- When a tailer starts with null state, it begins reading from the start of the file

### LogSourceController Endpoint

Located in `log-search-service/src/main/java/com/locallogsearch/service/controller/LogSourceController.java`

```java
@PostMapping("/{id}/reindex")
public ResponseEntity<Map<String, String>> reindexSource(@PathVariable String id)
```

- Validates that the log source exists
- Calls `indexManager.clearIndex()` to empty the index
- Calls `stateRepository.remove()` to delete the checkpoint
- Calls `tailerManager.reindexLogSource()` to restart tailing from the beginning

## Important Notes

### Data Loss
⚠️ **Warning**: Reindexing will delete all existing documents in the index. This operation cannot be undone.

### Async Operation
The reindex operation starts immediately but processes asynchronously. The API returns as soon as the reindex is initiated, but the actual re-reading and re-indexing of the log file happens in the background.

### Large Files
For large log files, reindexing may take significant time. You can monitor progress by:
- Checking the index document count via `/api/indices`
- Watching the application logs for indexing progress

### Active Tailing
If the log source is actively tailing (enabled), the reindex will:
1. Stop the current tailer
2. Start a new tailer from the beginning
3. Continue tailing new entries as they're written

## Use Cases

### Scenario 1: Update Parser Configuration

```bash
# 1. Update the log source parser configuration
curl -X PUT http://localhost:8080/api/sources/my-logs \
  -H "Content-Type: application/json" \
  -d '{
    "id": "my-logs",
    "filePath": "/var/log/app.log",
    "indexName": "app_logs",
    "parserType": "grok",
    "parserConfig": {
      "pattern": "%{TIMESTAMP_ISO8601:timestamp} %{LOGLEVEL:level} %{GREEDYDATA:message}"
    },
    "enabled": true
  }'

# 2. Reindex to apply the new parser to existing logs
curl -X POST http://localhost:8080/api/sources/my-logs/reindex
```

### Scenario 2: Clean Slate After Testing

```bash
# Reindex to start fresh
curl -X POST http://localhost:8080/api/sources/test-logs/reindex
```

### Scenario 3: Recover from Corruption

```bash
# If an index appears corrupted, reindex from source
curl -X POST http://localhost:8080/api/sources/corrupted-logs/reindex
```

## Related Endpoints

- `GET /api/indices` - List all indices and their document counts
- `DELETE /api/indices/{indexName}` - Delete an entire index (without re-reading)
- `POST /api/indices/{indexName}/clear` - Clear an index (without re-reading)
- `GET /api/sources` - List all log sources
- `POST /api/sources/{id}/enable` - Enable a log source
- `POST /api/sources/{id}/disable` - Disable a log source

## Monitoring Reindex Progress

```bash
# Check current document count
curl http://localhost:8080/api/indices

# Response shows progress
{
  "name": "app_logs",
  "path": "/path/to/indices/app_logs",
  "documentCount": 1523,  # Increases as reindexing progresses
  "sizeBytes": 245678,
  "sizeFormatted": "239.9 KB"
}
```
