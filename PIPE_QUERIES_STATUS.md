# Pipe Query Implementation Status

## ✅ Completed Backend Implementation

### 1. Core Infrastructure
- **PipeCommand** interface - base for all pipe commands
- **PipeResult** abstract class with subtypes:
  - `LogsResult` - raw log entries
  - `TableResult` - tabular data (columns + rows)
  - `ChartResult` - chart data (labels + series)
  - `TimeChartResult` - time-series data (timestamps + series)

### 2. Query Parser
- **PipeQueryParser** - parses Splunk-style pipe syntax
  - Splits queries by `|` respecting quotes
  - Parses command name, parameters (key=value), and arguments
  - Returns `ParsedQuery` with base query + list of pipe commands

### 3. Pipe Commands Implemented
-  **StatsCommand** - aggregation command
  - Syntax: `| stats count [avg(field)] [by groupField]`
  - Aggregation functions: count, sum, avg, min, max, dc (distinct count)
  - Supports grouping by one or more fields
  - Returns TableResult

- **ChartCommand** - converts stats to charts
  - Syntax: `| chart [type=bar] count by field`
  - Chart types: bar, pie, line
  - Converts TableResult to ChartResult

- **TimeChartCommand** - time-series charts
  - Syntax: `| timechart span=1h count [by field]`
  - Span formats: 1s, 5m, 1h, 1d
  - Bins results by time interval
  - Returns TimeChartResult

### 4. Command Factory
- **PipeCommandFactory** - creates command instances from parsed specs
- Handles parameter parsing and defaults

### 5. SearchService Integration
- Modified `search()` method to detect pipe queries
- New `searchWithPipes()` method:
  - Executes base search
  - Applies pipe commands in sequence
  - Returns SearchResponse with PipeResult

### 6. SearchResponse Updates
- Added `resultType` field (LOGS, TABLE, CHART, TIMECHART)
- Added `pipeResult` field to hold pipe command output
- New constructor: `SearchResponse(PipeResult)`

## 🚧 Remaining Work

### 1. REST API Updates
**File:** `SearchController.java`

Need to update `ApiSearchResponse` to serialize pipe results:

```java
public ApiSearchResponse(SearchResponse response) {
    this.resultType = response.getResultType().name();
    
    if (response.getResultType() == PipeResult.ResultType.LOGS) {
        // Existing logic
        this.results = response.getResults();
        this.totalHits = response.getTotalHits();
        // ...
    } else {
        // Pipe result
        this.pipeResult = response.getPipeResult();
    }
}
```

Add fields:
- `String resultType`
- `Object pipeResult` (will serialize based on type)

### 2. UI Updates

#### A. Add Chart.js Library
**File:** `index.html`

In `<head>`:
```html
<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js"></script>
```

#### B. Render Different Result Types
Update `performSearch()` to check `data.resultType`:

```javascript
if (data.resultType === 'TABLE') {
    renderTable(data.pipeResult);
} else if (data.resultType === 'CHART') {
    renderChart(data.pipeResult);
} else if (data.resultType === 'TIMECHART') {
    renderTimeChart(data.pipeResult);
} else {
    // Existing log results rendering
}
```

#### C. Implement Renderers

**renderTable():**
```javascript
function renderTable(tableResult) {
    let html = '<table class="results-table">';
    html += '<thead><tr>';
    tableResult.columns.forEach(col => {
        html += `<th>${col}</th>`;
    });
    html += '</tr></thead><tbody>';
    
    tableResult.rows.forEach(row => {
        html += '<tr>';
        tableResult.columns.forEach(col => {
            html += `<td>${row[col] || ''}</td>`;
        });
        html += '</tr>';
    });
    html += '</tbody></table>';
    
    resultsEl.innerHTML = html;
}
```

**renderChart():**
```javascript
function renderChart(chartResult) {
    const canvas = document.createElement('canvas');
    canvas.id = 'resultChart';
    resultsEl.innerHTML = '';
    resultsEl.appendChild(canvas);
    
    const datasets = [];
    for (const [seriesName, values] of Object.entries(chartResult.series)) {
        datasets.push({
            label: seriesName,
            data: values
        });
    }
    
    new Chart(canvas, {
        type: chartResult.chartType,
        data: {
            labels: chartResult.labels,
            datasets: datasets
        }
    });
}
```

**renderTimeChart():**
```javascript
function renderTimeChart(timeChartResult) {
    // Similar to renderChart but with time-series config
    const canvas = document.createElement('canvas');
    canvas.id = 'resultChart';
    resultsEl.innerHTML = '';
    resultsEl.appendChild(canvas);
    
    const datasets = [];
    for (const [seriesName, values] of Object.entries(timeChartResult.series)) {
        datasets.push({
            label: seriesName,
            data: values,
            borderColor: getRandomColor(),
            fill: false
        });
    }
    
    new Chart(canvas, {
        type: 'line',
        data: {
            labels: timeChartResult.timestamps,
            datasets: datasets
        },
        options: {
            scales: {
                x: { type: 'category' },
                y: { beginAtZero: true }
            }
        }
    });
}
```

### 3. Add Query Examples

In the Examples page, add:

**Aggregation Examples:**
```
level:ERROR | stats count by user
level:ERROR | stats count avg(duration) by operation
status:slow | stats max(duration) min(duration) by user
```

**Chart Examples:**
```
level:ERROR | chart count by user
level:ERROR | chart type=pie count by operation
* | stats count by level | chart type=bar count by level
```

**Time Chart Examples:**
```
* | timechart span=1h count
level:ERROR | timechart span=5m count by user
status:slow | timechart span=30m count
```

### 4. CSS Styling

Add styles for tables and chart container:

```css
.results-table {
    width: 100%;
    border-collapse: collapse;
    margin-top: 1rem;
}

.results-table th {
    background: #3498db;
    color: white;
    padding: 0.75rem;
    text-align: left;
    border: 1px solid #ddd;
}

.results-table td {
    padding: 0.5rem 0.75rem;
    border: 1px solid #ddd;
}

.results-table tr:nth-child(even) {
    background: #f8f9fa;
}

#resultChart {
    max-height: 400px;
}
```

## Example Queries

### Basic Stats
```
level:ERROR | stats count
level:ERROR | stats count by user
level:ERROR | stats count avg(duration) by operation
```

### Charts
```
level:ERROR | chart count by user
level:ERROR | chart type=pie count by operation
status:slow | stats max(duration) by user | chart type=bar max(duration) by user
```

### Time Charts
```
* | timechart span=1h count
level:ERROR | timechart span=5m count
level:ERROR | timechart span=30m count by user
```

### Advanced
```
level:ERROR AND status:slow | stats count avg(duration) max(duration) by operation
* | stats dc(user) dc(operation) by level
(level:ERROR OR level:WARN) | timechart span=15m count by level
```

## Testing Plan

1. **Test Stats Command:**
   ```bash
   curl -X POST http://localhost:8080/api/search \
     -H "Content-Type: application/json" \
     -d '{"indices": ["app-logs"], "query": "level:ERROR | stats count by user"}'
   ```

2. **Test Chart Command:**
   ```bash
   curl -X POST http://localhost:8080/api/search \
     -H "Content-Type: application/json" \
     -d '{"indices": ["app-logs"], "query": "level:ERROR | chart count by user"}'
   ```

3. **Test Timechart:**
   ```bash
   curl -X POST http://localhost:8080/api/search \
     -H "Content-Type: application/json" \
     -d '{"indices": ["app-logs"], "query": "* | timechart span=1h count"}'
   ```

4. **Test in UI:**
   - Enter query with pipes in search box
   - Verify table/chart renders correctly
   - Test different chart types
   - Test time-series charts

## Architecture

```
User Query: "level:ERROR | stats count by user | chart type=bar count by user"
      ↓
PipeQueryParser.parse()
      ↓
ParsedQuery {
  baseQuery: "level:ERROR"
  commands: [
    {command: "stats", args: ["count", "by", "user"]},
    {command: "chart", params: {type: "bar"}, args: ["count", "by", "user"]}
  ]
}
      ↓
SearchService.searchWithPipes()
      ↓
1. Execute base search → List<SearchResult>
2. StatsCommand.execute() → TableResult
3. ChartCommand.execute() → ChartResult
      ↓
SearchResponse(PipeResult)
      ↓
SearchController → JSON
      ↓
UI detects resultType → renderChart()
      ↓
Chart.js displays bar chart
```

## Next Steps

1. Complete SearchController updates for pipe result serialization
2. Add Chart.js to UI
3. Implement render functions for table/chart/timechart
4. Add pipe query examples
5. Test end-to-end
6. Document query syntax for users

## Files Modified/Created

### Created:
- `PipeCommand.java`
- `PipeResult.java`
- `PipeQueryParser.java`
- `StatsCommand.java`
- `ChartCommand.java`
- `TimeChartCommand.java`
- `PipeCommandFactory.java`

### Modified:
- `SearchService.java` - added pipe query support
- `SearchResponse.java` - added pipe result fields

### Need to Modify:
- `SearchController.java` - serialize pipe results
- `index.html` - add Chart.js and rendering logic
