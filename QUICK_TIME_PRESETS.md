# Quick Time Range Presets (Like Splunk)

## Overview
A Splunk-style quick time range selector has been added to the search UI. This allows you to easily filter logs to show only recent events (e.g., "Last 5 minutes", "Last 1 hour") without having to manually enter timestamps.

## What's New

### Quick Time Range Dropdown
Located in the search form, right above the "Custom" button:

```
Time Range
┌─────────────────────────┐  ┌────────┐
│ All time (default)   ▼  │  │ 🕐 Custom │
└─────────────────────────┘  └────────┘
Last 5 minutes →
Last 15 minutes →
Last 30 minutes →
Last 1 hour →
Last 2 hours →
Last 4 hours →
Last 12 hours →
Last 24 hours →
Last 7 days →
Last 30 days →
Custom range... →
```

## How It Works

### Default Behavior
- **All time (default):** Searches entire index without time filtering
- This is the default when the page loads

### Quick Presets
When you select a preset like "Last 15 minutes":
1. Automatically calculates time range from NOW back N minutes/hours/days
2. Sets `from` timestamp to (now - N)
3. Sets `to` timestamp to NOW
4. Updates the display to show the selected preset
5. Applies to all subsequent searches

### Custom Override
- Selecting "Custom range..." opens the detailed time picker modal
- Setting a custom range via the modal automatically switches dropdown to "Custom range..."
- Clicking timestamp popup and using time refinement also switches to "Custom"

### Clearing Time Filters
- Select "All time (default)" from dropdown to clear time filters
- Click "Clear" button to reset everything including time range
- Click "Clear" in the custom time picker modal

## Usage Examples

### Example 1: Last 5 Minutes
**Use Case:** Check recent errors

1. Select "Last 5 minutes" from dropdown
2. Enter query: `level:ERROR`
3. Click Search
4. See only errors from the last 5 minutes

**Result:** Shows logs from now back to 5 minutes ago

### Example 2: Last 24 Hours
**Use Case:** Daily log review

1. Select "Last 24 hours" from dropdown
2. Enter query: `*` or any query
3. Click Search
4. See all matching logs from the last day

### Example 3: Switch Between Presets
**Use Case:** Zoom in on a time window

1. Start with "Last 1 hour"
2. Find interesting events
3. Switch to "Last 15 minutes" to narrow focus
4. Or switch to "Last 4 hours" to broaden scope

### Example 4: Preset + Custom
**Use Case:** Start broad, then narrow

1. Select "Last 1 hour" → Search
2. Find timestamp: `11/15/2025, 10:05:32 AM`
3. Click timestamp → Select "Around ±5 seconds"
4. Dropdown automatically shows "Custom range..."
5. Continue refining as needed

## Technical Details

### Time Calculation
Presets calculate time ranges dynamically:

```javascript
// Example: "Last 15 minutes"
const now = new Date();
const from = new Date(now.getTime() - (15 * 60 * 1000)); // 15 minutes in ms
const to = now;

// Stored as ISO timestamps:
// from: "2025-11-15T09:52:30.123Z"
// to:   "2025-11-15T10:07:30.123Z"
```

### Preset Values

| Dropdown Option | Value | Milliseconds Back |
|----------------|-------|-------------------|
| Last 5 minutes | `5m` | 300,000 |
| Last 15 minutes | `15m` | 900,000 |
| Last 30 minutes | `30m` | 1,800,000 |
| Last 1 hour | `1h` | 3,600,000 |
| Last 2 hours | `2h` | 7,200,000 |
| Last 4 hours | `4h` | 14,400,000 |
| Last 12 hours | `12h` | 43,200,000 |
| Last 24 hours | `24h` | 86,400,000 |
| Last 7 days | `7d` | 604,800,000 |
| Last 30 days | `30d` | 2,592,000,000 |

### API Impact
When a preset is selected, the search API receives:

```json
{
  "indices": ["app-logs"],
  "query": "level:ERROR",
  "timestampFrom": 1731677150123,  // (now - N milliseconds)
  "timestampTo": 1731677450123,    // now
  "page": 0,
  "pageSize": 50
}
```

### Override Behavior
1. **Preset selected** → Custom timestamps cleared, dropdown value shown
2. **Custom modal used** → Dropdown switches to "Custom range..."
3. **Timestamp popup used** → Dropdown switches to "Custom range..."
4. **"All time" selected** → All timestamps cleared, back to default

## Integration with Other Features

### Works With:
✅ **Timestamp Click Popup** - Clicking timestamp switches dropdown to "Custom"
✅ **Custom Time Picker** - Selecting "Custom range..." opens the modal
✅ **Search History** - Time presets are saved in history
✅ **Clear Button** - Resets dropdown to "All time"
✅ **Millisecond Precision** - Presets use full timestamp precision

### Behavior Matrix

| Action | Dropdown State | Time Range | Display |
|--------|---------------|------------|---------|
| Page load | "All time (default)" | None | "All time" |
| Select "Last 5 minutes" | "Last 5 minutes" | now-5m to now | "Last 5 minutes" |
| Select "Custom range..." | "Custom range..." | Opens modal | Previous |
| Click timestamp → refine | "Custom range..." | Custom set | ISO timestamps |
| Open custom modal → Apply | "Custom range..." | Custom set | ISO timestamps |
| Click Clear | "All time (default)" | None | "All time" |

## Comparison to Splunk

### Similar Features
✅ Quick time presets (Last N minutes/hours/days)
✅ "All time" default option
✅ Custom time picker for precise control
✅ Real-time calculation (relative to NOW)

### Differences
- **Splunk:** Has "Real-time" streaming options
- **This:** Static time ranges (snapshot at search time)

- **Splunk:** More granular presets (e.g., "Last 4 hours", "Last 3 days")
- **This:** Common presets covering typical use cases

## Best Practices

### Performance Considerations
1. **Start narrow:** Use "Last 15 minutes" or "Last 1 hour" for faster queries
2. **Expand as needed:** If no results, try "Last 4 hours" or "Last 24 hours"
3. **Avoid "All time"** for large indices (millions of logs)

### Workflow Recommendations
1. **Initial search:** Use "Last 1 hour" as a good starting point
2. **Found results:** Use timestamp popup to zoom in (±5 seconds)
3. **No results:** Expand to "Last 4 hours" or "Last 24 hours"
4. **Historical analysis:** Use "Last 7 days" or "Last 30 days"

### Common Patterns

**Pattern 1: Recent Error Investigation**
```
1. Select "Last 15 minutes"
2. Search: level:ERROR
3. Review results
4. Click timestamp for context
5. Use "Around ±5 seconds" for detailed view
```

**Pattern 2: Daily Review**
```
1. Select "Last 24 hours"
2. Search: level:ERROR OR level:WARN
3. Review counts and patterns
4. Drill down with timestamp clicks
```

**Pattern 3: Performance Analysis**
```
1. Select "Last 1 hour"
2. Search: status:slow
3. Click timestamp of slow event
4. Use "Around ±5 minutes" to see related events
```

## Testing

### Test Case 1: Basic Preset
1. Open http://localhost:8080/ui/
2. Select "Last 5 minutes" from dropdown
3. Search: `level:ERROR`
4. Verify display shows "Last 5 minutes"
5. Verify results are recent (within 5 minutes)

### Test Case 2: Preset to Custom
1. Select "Last 15 minutes"
2. Search: `level:ERROR`
3. Click a timestamp
4. Select "Around ±5 seconds"
5. Verify dropdown shows "Custom range..."
6. Verify search uses custom range

### Test Case 3: Clear and Reset
1. Select "Last 1 hour"
2. Search: `*`
3. Click "Clear" button
4. Verify dropdown resets to "All time (default)"
5. Verify display shows "All time"

### Test Case 4: Multiple Preset Switches
1. Select "Last 5 minutes" → Search
2. Select "Last 15 minutes" → Search
3. Select "Last 1 hour" → Search
4. Verify each search uses correct time range
5. Verify display updates each time

## Browser Console Testing

```javascript
// After selecting "Last 15 minutes"
console.log(currentTimeRange);
// Output: 
// {
//   from: "2025-11-15T09:52:30.123Z",
//   to: "2025-11-15T10:07:30.123Z"
// }

// Check dropdown value
document.getElementById('quickTimeRange').value
// Output: "15m"

// Check display text
document.getElementById('timeRangeDisplay').textContent
// Output: "Last 15 minutes"
```

## Service Status

- **Running:** Yes (PID: 13243)
- **Port:** 8080
- **Features:** Quick time presets + millisecond precision + timestamp popup
- **Version:** 1.0-SNAPSHOT

## Quick Reference

**Common Time Presets:**
- Debugging recent issues: `Last 5 minutes` or `Last 15 minutes`
- Hourly monitoring: `Last 1 hour`
- Shift analysis: `Last 4 hours` or `Last 12 hours`
- Daily review: `Last 24 hours`
- Weekly trends: `Last 7 days`
- Monthly analysis: `Last 30 days`

**When to Use Custom:**
- Need specific timestamp precision
- Investigating historical events
- Correlating with external timestamps
- Sub-second analysis required
