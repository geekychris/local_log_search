# Pipeline Flow Diagram

## Multi-Stage Pipeline Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         PIPELINE STAGES                              │
└─────────────────────────────────────────────────────────────────────┘

    ┌──────────┐
    │  Query   │  Base Lucene query (e.g., "status:slow")
    └────┬─────┘
         │
         ▼
    ┌──────────────┐
    │  LogsResult  │  Raw log entries from Lucene index
    └──────┬───────┘
           │
           │ ┌──────────────────────────────────┐
           ├─┤  filter duration > 100           │ Filter logs
           │ └──────────────────────────────────┘
           │
           ▼
    ┌──────────────┐
    │  LogsResult  │  Filtered log entries
    └──────┬───────┘
           │
           │ ┌──────────────────────────────────┐
           ├─┤  stats count avg(duration) by    │ Aggregate data
           │ │  user                            │
           │ └──────────────────────────────────┘
           │
           ▼
    ┌──────────────┐
    │ TableResult  │  Aggregated statistics
    │              │  columns: [user, count, avg(duration)]
    │              │  rows: [{user: "alice", count: 150, ...}, ...]
    └──────┬───────┘
           │
           │ ┌──────────────────────────────────┐
           ├─┤  filter avg(duration) > 1000     │ Filter table
           │ └──────────────────────────────────┘
           │
           ▼
    ┌──────────────┐
    │ TableResult  │  Filtered statistics (high avg duration)
    └──────┬───────┘
           │
           │ ┌──────────────────────────────────┐
           ├─┤  chart type=bar avg(duration)    │ Visualize
           │ │  by user                         │
           │ └──────────────────────────────────┘
           │
           ▼
    ┌──────────────┐
    │ ChartResult  │  Bar chart data
    │              │  chartType: "bar"
    │              │  labels: ["alice", "bob", ...]
    │              │  series: {avg(duration): [1200, 950, ...]}
    └──────────────┘
```

## Supported Transformations

### LogsResult Transformations

```
LogsResult ──filter──> LogsResult
           ──stats──> TableResult
           ──chart──> ChartResult (computes stats internally)
           ──timechart──> TimeChartResult
           ──export──> ExportResult
```

### TableResult Transformations

```
TableResult ──filter──> TableResult
            ──chart──> ChartResult (uses existing data)
```

### Terminal Nodes

```
ChartResult ──(no further pipes)──> API Response
TimeChartResult ──(no further pipes)──> API Response
ExportResult ──(no further pipes)──> API Response
```

## Example Pipeline Flows

### Example 1: Query → Stats → Filter → Chart

```
Query: * | stats count by level | filter count > 50 | chart type=pie count by level

   *  ──────> LogsResult (1000 logs)
              │
              │ stats count by level
              ▼
           TableResult (3 rows: ERROR=150, WARN=75, INFO=800)
              │
              │ filter count > 50
              ▼
           TableResult (3 rows: all pass filter)
              │
              │ chart type=pie count by level
              ▼
           ChartResult (pie chart with 3 slices)
```

### Example 2: Query → Filter → Stats → Filter → Chart

```
Query: status:slow | filter duration > 500 | stats avg(duration) by user | 
       filter avg(duration) > 1000 | chart type=bar avg(duration) by user

   status:slow  ──────> LogsResult (500 logs)
                        │
                        │ filter duration > 500
                        ▼
                     LogsResult (200 logs)
                        │
                        │ stats avg(duration) by user
                        ▼
                     TableResult (10 users)
                        │
                        │ filter avg(duration) > 1000
                        ▼
                     TableResult (3 users: alice=1200, bob=1500, charlie=1100)
                        │
                        │ chart type=bar avg(duration) by user
                        ▼
                     ChartResult (bar chart with 3 bars)
```

### Example 3: Multiple Filters on Table

```
Query: * | stats count avg(duration) by user | 
       filter count > 10 | filter avg(duration) > 500

   *  ──────> LogsResult (10000 logs)
              │
              │ stats count avg(duration) by user
              ▼
           TableResult (50 users)
              │
              │ filter count > 10
              ▼
           TableResult (30 users)
              │
              │ filter avg(duration) > 500
              ▼
           TableResult (5 users: final filtered result)
```

## Command Execution Model

### Sequential Processing

```java
// Pseudocode for pipeline execution

PipeResult result = null;
Iterator<SearchResult> iterator = searchLogs(baseQuery);

for (PipeCommand command : commands) {
    if (result == null) {
        // First command: execute on iterator
        result = command.execute(iterator, totalHits);
    } else {
        // Subsequent commands: route based on types
        result = executeChainedCommand(command, result, totalHits);
    }
}

return result;
```

### Type-Based Routing

```java
// executeChainedCommand logic

if (command instanceof FilterCommand) {
    // Filter works on both LogsResult and TableResult
    return filterCommand.executeOnResult(inputResult);
}

if (command instanceof ChartCommand && inputResult instanceof TableResult) {
    // Chart can consume pre-computed table
    return chartCommand.executeOnTable(inputResult);
}

if (inputResult instanceof LogsResult) {
    // Commands that need logs as input
    Iterator<SearchResult> iter = logsResult.getResults().iterator();
    return command.execute(iter, totalHits);
}

throw new IllegalStateException("Cannot chain " + command.getName() + 
                                " after " + inputResult.getType());
```

## Memory and Performance

### Streaming vs Materialization

```
┌──────────────┐         ┌──────────────┐
│   Lucene     │────────>│   Iterator   │  Streaming: Memory efficient
│   Index      │         │ (one-by-one) │  Used for: filter on logs
└──────────────┘         └──────────────┘

┌──────────────┐         ┌──────────────┐
│  LogsResult  │────────>│   List of    │  Materialized: More memory
│  (logs)      │         │   Logs       │  Used for: stats computation
└──────────────┘         └──────────────┘

┌──────────────┐         ┌──────────────┐
│ TableResult  │────────>│   List of    │  Materialized: Compact
│ (aggregated) │         │   Rows       │  Used for: filter on tables
└──────────────┘         └──────────────┘
```

### Pipeline Optimization

**Good: Filter early**
```
level:ERROR | filter duration > 100 | stats count by user
     ^                ^
     |                |
  1000 logs       200 logs (filtered)      10 rows (stats)
```

**Bad: Filter late**
```
level:ERROR | stats count duration by user | filter avg(duration) > 100
     ^                    ^                          ^
     |                    |                          |
  1000 logs         1000 rows (stats)           50 rows (filtered)
                    (more computation)
```

## Error Handling

### Invalid Pipeline Example

```
* | stats count by level | stats count by user
                          ^
                          |
    ERROR: Cannot execute stats on TableResult
    TableResult doesn't have log entries to aggregate
```

### Solution

```
* | stats count by level
    (Get table result, then start new query if needed)
```

## Best Practices Flow

```
┌─────────────────────────────────────────────────────────────┐
│  1. Start with specific query (narrow result set at query)  │
│     Example: level:ERROR AND timestamp:[now-1h TO now]       │
└──────────────┬──────────────────────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────────────────────┐
│  2. Apply filter on logs (reduce data before aggregation)   │
│     Example: | filter duration > 100                        │
└──────────────┬──────────────────────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────────────────────┐
│  3. Aggregate data (collapse to smaller result set)         │
│     Example: | stats count avg(duration) by user            │
└──────────────┬──────────────────────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────────────────────┐
│  4. Filter aggregated results (refine insights)             │
│     Example: | filter count > 10 | filter avg(duration)>500 │
└──────────────┬──────────────────────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────────────────────┐
│  5. Visualize (terminal stage)                              │
│     Example: | chart type=bar avg(duration) by user         │
└─────────────────────────────────────────────────────────────┘
```
