# Parser Testing and Log Source Editing

## Overview

The Log Sources Management UI now includes functionality to edit log sources and test parser configurations in real-time. This allows users to:

1. Edit existing log source configurations (parser type, parser config, index name)
2. Test parser configurations with sample log entries
3. See how parser changes affect field extraction in real-time

## Features

### Edit Log Source

From the Log Sources page (`/ui/sources.html`), each configured log source now has an "Edit" button that opens a modal dialog with:

- **File Path** (read-only): The source file path cannot be changed
- **Index Name**: The Lucene index to store logs
- **Parser Type**: Choose between keyvalue, regex, or grok
- **Parser Configuration**: JSON configuration for the selected parser

### Live Parser Testing

The edit modal includes a parser testing section that allows you to:

1. Paste a sample log entry into a textarea
2. See the parsed fields in real-time as you type
3. Modify parser configuration and immediately see the effect
4. Validate that your parser extracts the expected fields

The parser test updates automatically 500ms after you stop typing (debounced).

## API Endpoints

### Test Parser
```
POST /api/parser/test
Content-Type: application/json

{
  "logEntry": "timestamp=2025-01-15T10:30:00Z level=INFO message=\"User login\" user=john",
  "parserType": "keyvalue",
  "parserConfig": null
}
```

**Response:**
```json
{
  "success": true,
  "timestamp": 1736937000000,
  "fields": {
    "level": "INFO",
    "timestamp": "2025-01-15T10:30:00Z",
    "message": "User login",
    "user": "john"
  },
  "rawText": "timestamp=2025-01-15T10:30:00Z level=INFO message=\"User login\" user=john"
}
```

**Error Response:**
```json
{
  "success": false,
  "error": "Parser configuration error: invalid regex pattern",
  "rawText": "..."
}
```

### Update Source
```
PUT /api/sources/{sourceId}
Content-Type: application/json

{
  "filePath": "/tmp/test.log",
  "indexName": "test-logs",
  "parserType": "regex",
  "parserConfig": {
    "pattern": "^(?<timestamp>\\S+) (?<level>\\S+) (?<message>.*)$",
    "field.1": "timestamp",
    "field.2": "level",
    "field.3": "message"
  },
  "enabled": true
}
```

## Implementation Details

### Frontend (sources.html)

**Edit Button:**
The edit button passes all source configuration as parameters:
```javascript
editSource(id, filePath, indexName, parserType, parserConfigJson)
```

**Parser Test with Debouncing:**
- User types in sample log entry or modifies parser config
- JavaScript waits 500ms after last input
- Makes POST request to `/api/parser/test`
- Displays parsed fields or error message

**Parsed Output Display:**
- Success: Shows all extracted fields with green border
- Error: Shows error message with red border
- Empty: Clears display when textarea is empty

### Backend (ParserTestController.java)

**How It Works:**
1. Receives log entry, parser type, and optional parser config
2. Creates a parser using `ParserFactory.createParser()`
3. Creates a temporary `LogEntry` object
4. Calls `parser.parse(entry)` to extract fields in-place
5. Returns extracted fields and timestamp

**Parser Support:**
- **KeyValue**: Parses `key=value` pairs (default)
- **Regex**: Requires `pattern` and `field.N` mappings in config
- **Grok**: Requires grok pattern in config

## Usage Examples

### Example 1: Testing KeyValue Parser

**Sample Log:**
```
timestamp=2025-01-15T10:30:00Z level=INFO message="User logged in" user=john sessionId=abc123
```

**Parser Type:** `keyvalue`  
**Parser Config:** (empty)

**Result:**
```
timestamp: 2025-01-15T10:30:00Z
level: INFO
message: "User logged in"
user: john
sessionId: abc123
```

### Example 2: Testing Regex Parser

**Sample Log:**
```
[2025-01-15 10:30:00] INFO Application started on port 8080
```

**Parser Type:** `regex`  
**Parser Config:**
```json
{
  "pattern": "^\\[(?<timestamp>[^\\]]+)\\] (?<level>\\w+) (?<message>.*)$",
  "field.1": "timestamp",
  "field.2": "level",
  "field.3": "message"
}
```

**Result:**
```
timestamp: 2025-01-15 10:30:00
level: INFO
message: Application started on port 8080
```

### Example 3: Testing Grok Parser

**Sample Log:**
```
127.0.0.1 - - [15/Jan/2025:10:30:00 +0000] "GET /api/users HTTP/1.1" 200 1234
```

**Parser Type:** `grok`  
**Parser Config:**
```json
{
  "pattern": "%{COMBINEDAPACHELOG}"
}
```

**Result:**
```
clientip: 127.0.0.1
timestamp: 15/Jan/2025:10:30:00 +0000
verb: GET
request: /api/users
httpversion: 1.1
response: 200
bytes: 1234
```

## UI Components

### Modal CSS Classes
- `.modal`: Overlay container
- `.modal-content`: Modal dialog box
- `.modal-close`: Close button (×)
- `.parser-test-section`: Yellow-highlighted test area
- `.parsed-output`: Result display area
- `.parsed-field`: Individual field display
- `.field-key`: Field name (red)
- `.field-value`: Field value (dark gray)

### Button Styles
- **Edit**: Orange/amber (`#f39c12`)
- **Save Changes**: Blue (`#3498db`)
- **Cancel**: Gray (`#95a5a6`)
- **Delete**: Red (`#e74c3c`)

## Files Modified

1. **sources.html**: Added edit modal HTML, CSS, and JavaScript functions
2. **ParserTestController.java**: New REST controller for parser testing
3. **LogEntry.java**: No changes (already existed)
4. **LogParser.java**: No changes (already existed)

## Future Enhancements

- Save parser test examples with log source
- Export/import parser configurations
- Regex pattern builder/helper
- Grok pattern library browser
- Field mapping validation
- Timestamp format detection and validation
