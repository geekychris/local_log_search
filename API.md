# REST API Documentation

## Base URL

```
http://localhost:8080/api
```

## Log Sources API

### List All Log Sources

**GET** `/sources`

Returns all configured log sources.

**Response:**
```json
[
  {
    "id": "abc123",
    "filePath": "/path/to/app.log",
    "indexName": "app-logs",
    "parserType": "keyvalue",
    "parserConfig": {},
    "enabled": true
  }
]
```

### Get Single Log Source

**GET** `/sources/{id}`

**Response:**
```json
{
  "id": "abc123",
  "filePath": "/path/to/app.log",
  "indexName": "app-logs",
  "parserType": "keyvalue",
  "parserConfig": {},
  "enabled": true
}
```

### Create Log Source

**POST** `/sources`

**Request Body:**
```json
{
  "filePath": "/path/to/app.log",
  "indexName": "app-logs",
  "parserType": "keyvalue",
  "parserConfig": {}
}
```

**Response:** Created log source with generated `id`

### Update Log Source

**PUT** `/sources/{id}`

**Request Body:**
```json
{
  "filePath": "/path/to/app.log",
  "indexName": "app-logs",
  "parserType": "regex",
  "parserConfig": {
    "pattern": "(\\w+)=(\\S+)",
    "field.1": "key",
    "field.2": "value"
  },
  "enabled": true
}
```

### Delete Log Source

**DELETE** `/sources/{id}`

**Response:** 204 No Content

### Enable Log Source

**POST** `/sources/{id}/enable`

### Disable Log Source

**POST** `/sources/{id}/disable`

## Search API

### Search Logs

**POST** `/search`

**Request Body:**
```json
{
  "indices": ["app-logs", "access-logs"],
  "query": "level:ERROR",
  "maxResults": 100
}
```

**Response:**
```json
{
  "results": [
    {
      "rawText": "2025-01-14T10:30:00Z level=ERROR operation=processOrder user=admin",
      "timestamp": "2025-01-14T10:30:00Z",
      "source": "/path/to/app.log",
      "indexName": "app-logs",
      "fields": {
        "level": "ERROR",
        "operation": "processOrder",
        "user": "admin"
      },
      "score": 1.5
    }
  ],
  "totalHits": 1
}
```

## Query Syntax

### Field Queries

Query specific fields extracted by parsers:

```json
{
  "query": "level:ERROR"
}
```

```json
{
  "query": "user:admin AND operation:processOrder"
}
```

### Boolean Operators

- **AND**: Both terms must match
  ```json
  {"query": "ERROR AND database"}
  ```

- **OR**: Either term must match
  ```json
  {"query": "ERROR OR WARN"}
  ```

- **NOT**: Exclude term
  ```json
  {"query": "ERROR NOT timeout"}
  ```

### Wildcards

```json
{
  "query": "user:john*"
}
```

```json
{
  "query": "endpoint:/api/*"
}
```

### Range Queries

Numeric ranges:
```json
{
  "query": "status:[400 TO 599]"
}
```

```json
{
  "query": "duration:[100 TO *]"
}
```

### Phrase Queries

```json
{
  "query": "raw_text:\"connection timeout\""
}
```

### Multiple Indices

Search across multiple indices:

```json
{
  "indices": ["app-logs", "access-logs", "error-logs"],
  "query": "user:admin"
}
```

## Parser Configuration

### Key-Value Parser

```json
{
  "parserType": "keyvalue",
  "parserConfig": {
    "delimiter": " "
  }
}
```

### Regex Parser

```json
{
  "parserType": "regex",
  "parserConfig": {
    "pattern": "(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z) \\[(\\w+)\\] (.*)",
    "field.1": "timestamp",
    "field.2": "level",
    "field.3": "message",
    "timestamp.group": "1",
    "timestamp.format": "yyyy-MM-dd'T'HH:mm:ssX"
  }
}
```

### Grok Parser

```json
{
  "parserType": "grok",
  "parserConfig": {
    "pattern": "%{TIMESTAMP_ISO8601:timestamp} %{LOGLEVEL:level} %{GREEDYDATA:message}",
    "timestamp.field": "timestamp",
    "timestamp.format": "yyyy-MM-dd'T'HH:mm:ss.SSSX"
  }
}
```

### Custom Parser

```json
{
  "parserType": "custom",
  "parserConfig": {
    "class": "com.example.MyCustomParser"
  }
}
```

## Examples

### Example 1: Add and Search Application Logs

```bash
# 1. Add log source
curl -X POST http://localhost:8080/api/sources \
  -H "Content-Type: application/json" \
  -d '{
    "filePath": "/var/log/myapp/application.log",
    "indexName": "myapp",
    "parserType": "keyvalue"
  }'

# 2. Wait 15-30 seconds for indexing

# 3. Search for errors
curl -X POST http://localhost:8080/api/search \
  -H "Content-Type: application/json" \
  -d '{
    "indices": ["myapp"],
    "query": "level:ERROR",
    "maxResults": 10
  }' | jq
```

### Example 2: Search Across Multiple Indices

```bash
curl -X POST http://localhost:8080/api/search \
  -H "Content-Type: application/json" \
  -d '{
    "indices": ["app-logs", "access-logs", "error-logs"],
    "query": "user:admin OR ip:192.168.1.100",
    "maxResults": 50
  }' | jq
```

### Example 3: Complex Query

```bash
curl -X POST http://localhost:8080/api/search \
  -H "Content-Type: application/json" \
  -d '{
    "indices": ["app-logs"],
    "query": "(level:ERROR OR level:WARN) AND operation:processOrder AND duration:[100 TO *]",
    "maxResults": 20
  }' | jq '.results[] | {timestamp, level: .fields.level, operation: .fields.operation, duration: .fields.duration}'
```

## Error Responses

### 400 Bad Request

```json
{
  "error": "Query cannot be empty"
}
```

```json
{
  "error": "Invalid query: Cannot parse query"
}
```

### 404 Not Found

```json
{
  "error": "Log source not found"
}
```

### 500 Internal Server Error

```json
{
  "error": "Search error: Index corrupted"
}
```
