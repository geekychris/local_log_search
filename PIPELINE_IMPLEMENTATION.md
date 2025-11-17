# Multi-Stage Pipeline Implementation Summary

## Overview

This implementation adds comprehensive support for multi-stage pipelines (3+ commands) in the log search application, enabling complex data transformation workflows like:

```
query | filter | stats | filter | chart
```

## What Was Added

### 1. New FilterCommand (`FilterCommand.java`)

**Location:** `log-search-core/src/main/java/com/locallogsearch/core/pipe/commands/FilterCommand.java`

**Features:**
- Filters both `LogsResult` and `TableResult` types
- Supports numeric comparisons: `>`, `>=`, `<`, `<=`, `=`, `!=`
- Supports string operations: `contains`, `startswith`, `endswith`
- Automatic numeric/string comparison detection

**Example Usage:**
```
* | stats count by user | filter count > 50
status:slow | filter duration > 100 | stats avg(duration) by user
```

### 2. Enhanced ChartCommand

**Changes:** Added `executeOnTable()` method to accept pre-computed `TableResult`

**Before:** Chart command always computed stats internally
**After:** Chart command can accept stats results from previous pipeline stage

**Enables:**
```
* | stats count by user | filter count > 100 | chart type=pie count by user
```

### 3. Updated PipeCommandFactory

**Changes:** 
- Added `FilterCommand` import and factory method
- Added `createFilterCommand()` to parse filter syntax
- Handles quoted values and multi-word values

**Parses:**
```
filter count > 10
filter user = "alice"
filter duration >= 100
```

### 4. Enhanced Pipeline Execution

**Location:** `SearchService.searchWithPipes()` and new `executeChainedCommand()`

**Key Improvements:**
- Supports chaining of different result types (Logs → Table → Chart)
- Intelligently routes commands based on input/output types
- Maintains backward compatibility with existing 2-stage pipes

**Transformation Flow:**
```
LogsResult → [filter] → LogsResult → [stats] → TableResult
TableResult → [filter] → TableResult → [chart] → ChartResult
```

## Pipeline Command Matrix

| From \ To | filter | stats | chart | timechart | export |
|-----------|--------|-------|-------|-----------|--------|
| **LogsResult** | ✅ | ✅ | ✅ | ✅ | ✅ |
| **TableResult** | ✅ | ❌ | ✅ | ❌ | ❌ |
| **ChartResult** | ❌ | ❌ | ❌ | ❌ | ❌ |

## Example Queries

### 1. Basic Multi-Stage

```bash
# Query → Stats → Filter → Chart
* | stats count by level | filter count > 50 | chart type=bar count by level
```

### 2. Complex Aggregation Pipeline

```bash
# Query → Filter → Stats → Filter → Chart
status:slow | filter duration > 500 | stats min(duration) max(duration) avg(duration) by user | filter avg(duration) > 1000 | chart type=bar avg(duration) by user
```

### 3. Multiple Filters

```bash
# Stats with multiple filter stages
* | stats count avg(duration) by user | filter count > 10 | filter avg(duration) > 500
```

### 4. Filter Before and After Aggregation

```bash
# Filter logs → Aggregate → Filter results
level:ERROR | filter duration > 100 | stats count by user | filter count > 5
```

## API Response Structure

### TableResult Example

```json
{
  "type": "TABLE",
  "columns": ["user", "count"],
  "rows": [
    {"user": "alice", "count": 150},
    {"user": "bob", "count": 100}
  ],
  "sourceHits": 1000
}
```

### ChartResult Example

```json
{
  "type": "CHART",
  "chartType": "bar",
  "labels": ["alice", "bob"],
  "series": {
    "count": [150, 100]
  },
  "sourceHits": 1000
}
```

## Testing

### Test Script

Created `test_multistage_pipeline.sh` with 6 comprehensive tests:

1. Basic stats command
2. Stats with filter
3. Stats → filter → chart (full pipeline)
4. Multi-aggregation with filter
5. Direct stats to chart
6. Filter logs before stats

### Running Tests

```bash
# Ensure service is running
./start-service.sh

# Run test suite
./test_multistage_pipeline.sh
```

## Documentation

### Created Files

1. **`docs/PIPELINE_GUIDE.md`** - Complete reference guide covering:
   - Pipeline architecture
   - All command types and syntax
   - Multi-stage examples
   - Best practices
   - Troubleshooting
   - API usage

2. **`test_multistage_pipeline.sh`** - Automated test suite

3. **`PIPELINE_IMPLEMENTATION.md`** - This file (implementation summary)

### Updated Files

1. **`README.md`** - Added:
   - Quick examples of multi-stage pipelines
   - Link to complete pipeline guide
   - Filter command documentation
   - Updated pipe commands section

## Architecture Changes

### Before

```
Query → PipeCommand (single) → Result
```

**Limitation:** Could only chain LogsResult → LogsResult

### After

```
Query → PipeCommand₁ → PipeCommand₂ → ... → PipeCommandₙ → Result
```

**Supports:**
- LogsResult → TableResult → ChartResult
- TableResult → TableResult (filtering)
- Multiple transformation stages

### Key Design Decisions

1. **Backward Compatibility:** All existing 2-stage pipelines work unchanged
2. **Type Safety:** Commands check input types and throw clear errors for invalid chains
3. **Streaming:** Maintains memory-efficient streaming for log processing
4. **Materialization:** Only materializes results when necessary for type transformations

## Future Enhancements

### Potential Additions

1. **Sort Command** - Sort table results by column
2. **Head/Tail Commands** - Limit table rows
3. **Join Command** - Combine multiple table results
4. **Eval Command** - Calculate new fields
5. **Rename Command** - Rename table columns
6. **Dedup Command** - Remove duplicate rows

### Example Future Pipelines

```bash
# With sort
* | stats count by user | sort -count | head 10

# With eval
* | stats count avg(duration) by user | eval score = count * avg(duration) | sort -score

# With dedup
* | filter level = "ERROR" | dedup user | stats count
```

## Performance Considerations

1. **Filter Early:** Place filter commands as early as possible to reduce data volume
2. **Aggregation Position:** Stats commands reduce result set size
3. **Memory Usage:** Large table results are kept in memory for filtering
4. **Index Efficiency:** Use query-level filters (Lucene) before pipe filters when possible

## Compilation and Deployment

```bash
# Build
mvn clean package -DskipTests

# The changes are in log-search-core module
# Automatically included in log-search-service when built
```

## Summary

This implementation enables powerful data analysis workflows by supporting:
- ✅ Multi-stage pipelines (3+ commands)
- ✅ Filter command for logs and tables
- ✅ Chaining different result types
- ✅ Backward compatibility
- ✅ Clear error messages
- ✅ Comprehensive documentation

Users can now build sophisticated queries like:
```
status:slow | filter duration > 500 | stats min(duration) max(duration) avg(duration) by user | filter avg(duration) > 1000 | chart type=bar avg(duration) by user
```
