# Future Enhancements

## 1. Query Sharing (Copy as curl / URL)

### Implementation

Add a "Share Query" button after search results that shows a modal with:

**Add to HTML (after results div):**
```html
<button id="shareBtn" style="margin-top: 1rem; display: none;">📋 Share Query</button>

<div id="shareModal" style="display: none; position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); z-index: 1000;">
    <div style="background: white; max-width: 600px; margin: 10% auto; padding: 2rem; border-radius: 8px;">
        <h3>Share Query</h3>
        
        <h4>cURL Command:</h4>
        <textarea id="curlCommand" readonly style="width: 100%; height: 150px; font-family: monospace; font-size: 0.85rem; padding: 0.5rem;"></textarea>
        <button onclick="navigator.clipboard.writeText(document.getElementById('curlCommand').value)">Copy cURL</button>
        
        <h4 style="margin-top: 1rem;">Shareable URL:</h4>
        <input id="shareUrl" readonly style="width: 100%; padding: 0.5rem; font-family: monospace;" />
        <button onclick="navigator.clipboard.writeText(document.getElementById('shareUrl').value)">Copy URL</button>
        
        <button onclick="document.getElementById('shareModal').style.display='none'" style="margin-top: 1rem;">Close</button>
    </div>
</div>
```

**JavaScript functions:**
```javascript
function generateCurlCommand() {
    const indices = document.getElementById('indices').value.split(',').map(s => s.trim());
    const query = document.getElementById('query').value;
    const sortField = document.getElementById('sortField').value;
    const sortOrder = document.getElementById('sortOrder').value;
    
    const payload = JSON.stringify({
        indices: indices,
        query: query,
        page: 0,
        pageSize: 50,
        sortField: sortField,
        sortDescending: sortOrder === 'desc',
        includeFacets: true
    }, null, 2);
    
    return `curl -X POST http://localhost:8080/api/search \\
  -H "Content-Type: application/json" \\
  -d '${payload}'`;
}

function generateShareUrl() {
    const params = new URLSearchParams({
        indices: document.getElementById('indices').value,
        query: document.getElementById('query').value,
        sortField: document.getElementById('sortField').value,
        sortOrder: document.getElementById('sortOrder').value
    });
    
    return `${window.location.origin}${window.location.pathname}?${params.toString()}`;
}

// Show share modal
document.getElementById('shareBtn').addEventListener('click', () => {
    document.getElementById('curlCommand').value = generateCurlCommand();
    document.getElementById('shareUrl').value = generateShareUrl();
    document.getElementById('shareModal').style.display = 'block';
});

// On page load, check for URL parameters
window.addEventListener('load', () => {
    const params = new URLSearchParams(window.location.search);
    if (params.has('query')) {
        document.getElementById('indices').value = params.get('indices') || 'app-logs';
        document.getElementById('query').value = params.get('query');
        document.getElementById('sortField').value = params.get('sortField') || 'timestamp';
        document.getElementById('sortOrder').value = params.get('sortOrder') || 'desc';
        
        // Auto-execute search
        performSearch(0);
    }
});
```

**Show/hide share button:**
```javascript
// In performSearch(), after successful results:
document.getElementById('shareBtn').style.display = 'block';
```

## 2. Time Range Selection on Charts

### Implementation using Chart.js Zoom Plugin

**Add chartjs-plugin-zoom:**
```html
<script src="https://cdn.jsdelivr.net/npm/hammerjs@2.0.8"></script>
<script src="https://cdn.jsdelivr.net/npm/chartjs-plugin-zoom@2.0.1/dist/chartjs-plugin-zoom.min.js"></script>
```

**Update renderTimeChart() options:**
```javascript
function renderTimeChart(timeChartResult) {
    // ... existing code ...
    
    window.myChart = new Chart(canvas, {
        type: 'line',
        data: {
            labels: timeChartResult.timestamps,
            datasets: datasets
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            plugins: {
                legend: {
                    display: true
                },
                zoom: {
                    zoom: {
                        drag: {
                            enabled: true,
                            backgroundColor: 'rgba(52, 152, 219, 0.3)'
                        },
                        mode: 'x',
                        onZoomComplete: handleTimeRangeSelection
                    },
                    pan: {
                        enabled: true,
                        mode: 'x'
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true
                }
            }
        }
    });
}

function handleTimeRangeSelection({chart}) {
    const xScale = chart.scales.x;
    const startIdx = Math.floor(xScale.min);
    const endIdx = Math.ceil(xScale.max);
    
    if (startIdx >= 0 && endIdx < chart.data.labels.length) {
        const startTime = chart.data.labels[startIdx];
        const endTime = chart.data.labels[endIdx];
        
        // Parse timestamps and update date range fields
        const startDate = new Date(startTime);
        const endDate = new Date(endTime);
        
        document.getElementById('dateFrom').value = startDate.toISOString().split('T')[0];
        document.getElementById('dateTo').value = endDate.toISOString().split('T')[0];
        
        // Show confirmation
        if (confirm(`Apply time range filter: ${startTime} to ${endTime}?`)) {
            performSearch(0);
        }
    }
}
```

**Add reset zoom button:**
```html
<button id="resetZoom" style="display: none; margin-top: 0.5rem;">Reset Zoom</button>
```

```javascript
document.getElementById('resetZoom').addEventListener('click', () => {
    if (window.myChart) {
        window.myChart.resetZoom();
    }
});

// Show reset button when rendering time chart
function renderTimeChart(timeChartResult) {
    document.getElementById('resetZoom').style.display = 'block';
    // ... rest of function
}
```

## Benefits

### Query Sharing
- **Collaboration**: Share queries with team members
- **Documentation**: Save queries in wikis/docs
- **Debugging**: Share exact query parameters with support
- **Automation**: Use curl commands in scripts/CI

### Time Range Selection
- **Interactive Exploration**: Drill down into specific time periods
- **Pattern Investigation**: Focus on anomalies or spikes
- **Efficient Analysis**: No need to manually calculate timestamps
- **Visual Feedback**: See selected region highlighted on chart

## Example Usage

### Sharing a Query
1. Run search: `level:ERROR | timechart span=1h count by user`
2. Click "📋 Share Query" button
3. Copy curl command to share with colleague
4. Or copy URL to save in documentation

### Time Range Selection
1. Run: `level:INFO | timechart span=30m count by user`
2. Chart displays with 5 colored lines
3. Click and drag on chart to select time range
4. Confirm to re-run query with selected time range
5. Use "Reset Zoom" to go back to full range

## Testing

```bash
# Test shareable URL
http://localhost:8080/ui/index.html?indices=app-logs&query=level:ERROR%20%7C%20stats%20count%20by%20user

# Test curl command
curl -X POST http://localhost:8080/api/search \
  -H "Content-Type: application/json" \
  -d '{
    "indices": ["app-logs"],
    "query": "level:ERROR | chart count by user",
    "page": 0,
    "pageSize": 50,
    "includeFacets": true
  }'
```
