# Multi-Stage Pipeline Guide

## Overview

The local log search application supports multi-stage pipelines, allowing you to chain multiple operations together to transform, filter, aggregate, and visualize your log data. Pipelines use the `|` (pipe) operator to connect commands in a sequence.

## Pipeline Architecture

### Result Types

Pipelines work with different result types that can be transformed through various commands:

1. **LogsResult** - Raw log entries
2. **TableResult** - Tabular aggregated data
3. **ChartResult** - Visualization data
4. **TimeChartResult** - Time-series visualization data

### Command Transformations

```
LogsResult → [filter] → LogsResult
LogsResult → [stats] → TableResult
TableResult → [filter] → TableResult
TableResult → [chart] → ChartResult
LogsResult → [timechart] → TimeChartResult
LogsResult → [export] → ExportResult
```

## Pipeline Commands

### 1. Filter Command

Filters logs or table rows based on field conditions.

**Syntax:**
```
| filter <field> <operator> <value>
```

**Operators:**
- `=` or `==` - Equals
- `!=` - Not equals
- `>` - Greater than
- `>=` - Greater than or equal
- `<` - Less than
- `<=` - Less than or equal
- `regex` or `match` - Regular expression pattern match
- `!regex` or `notmatch` - Negative regex match
- `contains` - String contains
- `startswith` - String starts with
- `endswith` - String ends with

**Examples:**
```
# Filter logs by duration
status:slow | filter duration > 100

# Filter aggregated results
* | stats count by user | filter count > 50

# Filter by string equality
* | stats count by level | filter level = "ERROR"

# Filter using regex pattern
* | filter message regex "error|exception|timeout"

# Filter for specific users with regex
* | filter user regex "^(alice|bob|charlie)$"

# Exclude patterns with negative regex
* | filter message !regex "debug|trace"
```

### 2. Stats Command

Aggregates data and produces tabular results.

**Syntax:**
```
| stats <aggregation>... [by <field>...]
```

**Aggregations:**
- `count` - Count of records
- `avg(field)` - Average of numeric field
- `sum(field)` - Sum of numeric field
- `min(field)` - Minimum value
- `max(field)` - Maximum value
- `dc(field)` - Distinct count

**Examples:**
```
# Simple count
* | stats count

# Count by field
* | stats count by level

# Multiple aggregations
* | stats count avg(duration) max(duration) by user

# Grouped by multiple fields
* | stats count by level user
```

### 3. Transform Command

Transform fields in logs or table results using regex extraction, field merging, renaming, and calculations.

**Syntax:**
```
| transform <operation> [parameters]
```

**Operations:**

**Rename:** Change field name
```
| transform rename oldfield as newfield
```

**Extract:** Extract values using regex capture groups
```
| transform extract <field> regex "pattern" as <newfield>
```

**Replace:** Replace text matching regex pattern
```
| transform replace <field> regex "pattern" with "replacement"
```

**Merge:** Combine multiple fields
```
| transform merge <field1,field2,...> as <newfield> [separator="sep"]
```

**Eval:** Calculate new field from expression
```
| transform eval <newfield> = <expression>
```

**Remove:** Delete a field
```
| transform remove <field>
```

**Examples:**
```
# Rename user field
* | transform rename user as username

# Extract hostname from URL
* | transform extract url regex "host=([^&]+)" as hostname

# Redact SSN patterns
* | transform replace message regex "\d{3}-\d{2}-\d{4}" with "XXX-XX-XXXX"

# Merge user and operation
* | transform merge user,operation as user_op separator="_"

# Convert milliseconds to seconds
* | transform eval duration_sec = duration / 1000

# Extract then aggregate
* | transform extract message regex "code=(\d+)" as status_code | stats count by status_code

# Complex pipeline: extract, filter, aggregate
* | transform extract url regex "host=([^&]+)" as hostname | filter hostname regex "prod.*" | stats count by hostname
```

### 4. Chart Command

Converts tabular data to chart visualization. Can compute stats internally or accept pre-computed table results.

**Syntax:**
```
| chart [type=<type>] <aggregation>... by <field>
```

**Chart Types:**
- `bar` (default)
- `pie`
- `line`

**Examples:**
```
# Chart directly from logs
* | chart type=pie count by level

# Chart from pre-computed stats (after filtering)
* | stats count by user | filter count > 10 | chart type=bar count by user

# Multiple series
* | chart type=line count avg(duration) by operation
```

### 5. TimeChart Command

Creates time-series visualizations.

**Syntax:**
```
| timechart [span=<duration>] <aggregation>... [by <field>]
```

**Examples:**
```
# Count over time
* | timechart span=1h count

# Multiple metrics over time
* | timechart span=5m count avg(duration)

# Split by field
* | timechart span=1h count by level
```

### 6. Export Command

Exports results to a database table.

**Syntax:**
```
| export table=<name> [fields=<fields>] [sample=<size>] [append=<bool>]
```

**Examples:**
```
# Export all fields
level:ERROR | export table=errors append=false

# Export specific fields with sampling
* | export table=logs fields=timestamp,level,user sample=1000
```

## Multi-Stage Pipeline Examples

### Example 1: Query → Stats → Filter → Chart

Find users with high activity and visualize:

```
* | stats count by user | filter count > 100 | chart type=pie count by user
```

**Pipeline Flow:**
1. Query all logs (`*`)
2. Aggregate by user (`stats count by user`) → TableResult
3. Filter for high-activity users (`filter count > 100`) → TableResult
4. Create pie chart (`chart type=pie count by user`) → ChartResult

### Example 2: Query → Filter → Stats → Filter → Chart

Analyze slow operations:

```
status:slow | filter duration > 500 | stats min(duration) max(duration) avg(duration) by user | filter avg(duration) > 1000 | chart type=bar avg(duration) by user
```

**Pipeline Flow:**
1. Query slow operations (`status:slow`)
2. Filter by duration (`filter duration > 500`) → LogsResult
3. Compute statistics per user (`stats...`) → TableResult
4. Filter users with high avg duration (`filter avg(duration) > 1000`) → TableResult
5. Create bar chart (`chart...`) → ChartResult

### Example 3: Complex Multi-Aggregation Pipeline

```
level:ERROR | stats count avg(duration) max(duration) by operation | filter count > 5 | chart type=bar count by operation
```

**Pipeline Flow:**
1. Query errors (`level:ERROR`)
2. Aggregate multiple metrics (`stats count avg(duration) max(duration) by operation`) → TableResult
3. Filter frequent operations (`filter count > 5`) → TableResult
4. Visualize counts (`chart type=bar count by operation`) → ChartResult

### Example 4: Filter Logs Then Aggregate

```
* | filter user = "alice" | stats count by level
```

**Pipeline Flow:**
1. Query all logs (`*`)
2. Filter for specific user (`filter user = "alice"`) → LogsResult
3. Aggregate by level (`stats count by level`) → TableResult

### Example 5: Extract, Filter with Regex, Transform and Aggregate

Extract status codes from messages, filter for production hosts, and analyze:

```
* | transform extract message regex "status=(\d+)" as status_code | transform extract url regex "host=([^&]+)" as hostname | filter hostname regex "prod.*" | filter status_code regex "^[45]" | stats count by status_code,hostname
```

**Pipeline Flow:**
1. Query all logs (`*`)
2. Extract status code from message (`transform extract...`) → LogsResult
3. Extract hostname from URL (`transform extract...`) → LogsResult  
4. Filter for production hosts (`filter hostname regex "prod.*"`) → LogsResult
5. Filter for 4xx/5xx errors (`filter status_code regex "^[45]"`) → LogsResult
6. Aggregate by status code and hostname (`stats count...`) → TableResult

### Example 6: Data Cleaning Pipeline

Clean sensitive data, normalize fields, and analyze:

```
* | transform replace message regex "\d{3}-\d{2}-\d{4}" with "[SSN]" | transform replace message regex "\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b" with "[EMAIL]" | transform eval duration_sec = duration / 1000 | filter duration_sec > 1 | stats count avg(duration_sec) by operation
```

**Pipeline Flow:**
1. Query all logs (`*`)
2. Redact SSN patterns (`transform replace...`) → LogsResult
3. Redact email addresses (`transform replace...`) → LogsResult
4. Convert duration to seconds (`transform eval...`) → LogsResult
5. Filter for slow operations (`filter duration_sec > 1`) → LogsResult
6. Aggregate statistics (`stats...`) → TableResult

## Best Practices

### 1. Filter Early
Apply filters as early as possible to reduce data volume:
```
# Good: Filter before stats
level:ERROR | filter duration > 100 | stats count by user

# Also good: Filter during query
level:ERROR AND duration:[100 TO *] | stats count by user
```

### 2. Use Appropriate Chart Types
- **Pie charts**: Best for proportions (5-7 categories max)
- **Bar charts**: Good for comparisons across categories
- **Line charts**: Best for trends and time-series data

### 3. Layer Filters for Precision
Use multiple filters to narrow results progressively:
```
* | stats count avg(duration) by user | filter count > 10 | filter avg(duration) > 500
```

### 4. Combine with Time Ranges
Use query-level time filters with pipeline operations:
```
timestamp:[2024-01-01 TO 2024-01-31] | stats count by level | chart type=pie count by level
```

## Limitations

1. **Chart commands are terminal** - You cannot pipe from a ChartResult to another command
2. **Export commands are terminal** - Exports end the pipeline
3. **Memory considerations** - Very large result sets should use sampling or filtering
4. **Numeric comparisons** - Filter operators (`>`, `<`, etc.) attempt numeric parsing but fall back to string comparison

## API Usage

### REST API Request

```bash
curl -X POST "http://localhost:8080/api/search" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "* | stats count by level | filter count > 50 | chart type=bar count by level",
    "indices": ["test-logs"],
    "page": 0,
    "pageSize": 100
  }'
```

### Response Types

The response `type` field indicates the result type:

```json
{
  "type": "CHART",
  "chartType": "bar",
  "labels": ["ERROR", "INFO", "WARN"],
  "series": {
    "count": [150, 500, 200]
  },
  "sourceHits": 850
}
```

For table results:
```json
{
  "type": "TABLE",
  "columns": ["level", "count"],
  "rows": [
    {"level": "INFO", "count": 500},
    {"level": "ERROR", "count": 150}
  ],
  "sourceHits": 850
}
```

## Troubleshooting

### "Cannot chain X command after Y result"
This error occurs when trying to chain incompatible command/result types. Check the command transformation diagram above.

### Empty results after filter
- Verify your filter condition is correct
- Check field names match exactly (case-sensitive)
- For numeric comparisons, ensure field contains numeric values

### Chart shows unexpected data
- Verify the table data before charting by removing the chart command
- Ensure aggregation fields match the chart's `by` clause
