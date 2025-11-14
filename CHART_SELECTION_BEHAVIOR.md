# Chart Time Range Selection - Final Behavior

## Overview
When a user drags to select a time range on a timechart, the selection **sets the time filter and re-runs the search WITHOUT zooming the chart**.

## Behavior Details

### What Happens When You Drag on a Timechart:

1. **User Action**: Click and drag horizontally on the timechart
2. **Visual Feedback**: Blue semi-transparent selection overlay appears during drag
3. **Time Filter Update**: Selected time range is captured and stored in the time picker
4. **Display Update**: Human-readable time range appears below the time picker button
5. **NO CHART ZOOM**: Chart immediately resets to full view (stays unzoomed)
6. **Auto-Search**: Search automatically executes after 500ms with the new time filter
7. **Results Update**: Chart and log results show only the filtered time range

### Key Point: **Chart Never Zooms**

The chart selection is **purely a mechanism for setting the time filter**. The chart itself never zooms in or changes scale. After each selection:
- Chart resets to full view immediately
- New search runs with time filter applied
- Chart re-renders showing only data from the selected time period
- Chart still shows full X-axis range (not zoomed)

## Example Workflow

### Scenario: Investigating Error Spike

**Initial State:**
- Query: `level:ERROR | timechart span=1h count`
- Chart shows 24 hours of data
- Time filter: "All time"

**Step 1: Select Time Range**
- User drags from 1:30 PM to 2:30 PM on chart
- Blue overlay shows during drag

**Step 2: Filter Applied (NO ZOOM)**
- Chart shows drag overlay briefly
- Chart immediately returns to full view (no zoom occurs)
- Time filter updates: "1/14/2025, 1:30:00 PM → 1/14/2025, 2:30:00 PM"
- Notification: "🔍 Time range selected: ... Re-running search..."

**Step 3: Search Re-executes**
- Query becomes: `(level:ERROR) AND timestamp:[2025-01-14T18:30:00.000Z TO 2025-01-14T19:30:00.000Z]`
- Search runs automatically (no button click needed)

**Step 4: Results Update**
- Chart re-renders with only 1:30-2:30 PM data
- Chart X-axis still shows full scale (not zoomed)
- Log results show only entries from that 1-hour window
- User can now analyze the filtered data

**Step 5: Further Investigation**
- User can drag another time range to refine further
- Or click "Clear" in time picker to reset to all time
- Or manually adjust time in the time picker modal

## Technical Implementation

### Chart.js Zoom Plugin Configuration

```javascript
zoom: {
    zoom: {
        drag: {
            enabled: true,  // Enable drag selection
            backgroundColor: 'rgba(52, 152, 219, 0.3)',
            borderColor: 'rgba(52, 152, 219, 0.8)',
            borderWidth: 1
        },
        mode: 'x',
        onZoomComplete: ({chart}) => {
            // Get the selected time range
            handleTimeRangeSelection(chart);
            // IMMEDIATELY RESET ZOOM - chart stays at full view
            chart.resetZoom();
            return false;
        }
    },
    pan: {
        enabled: false  // Pan disabled (not needed without persistent zoom)
    }
}
```

### Key Functions

**`handleTimeRangeSelection(chart)`**:
- Extracts selected time range from chart's X-scale
- Converts to datetime-local format (YYYY-MM-DDTHH:mm)
- Updates time picker inputs and display
- Triggers automatic search after 500ms

**`chart.resetZoom()`**:
- Called immediately after capturing the selection
- Prevents chart from staying zoomed
- Chart returns to full view before search runs

## User Instructions

### On the UI:
**Tip Message**: "💡 Tip: Click and drag to select a time range. Selection will set the time filter and re-run search without zooming the chart."

### How to Use:

1. **Select Time Range**: Click and drag on any timechart
2. **Watch the Filter**: Time range appears below "🕐 Select Time Range" button
3. **Wait for Results**: Search runs automatically (500ms delay)
4. **View Filtered Data**: Chart and logs show only the selected time period
5. **Refine Further**: Select another range, or clear the filter

### What NOT to Expect:

- ❌ Chart will NOT zoom in when you drag
- ❌ You cannot pan the chart left/right
- ❌ Double-click does nothing (no zoom to reset)
- ❌ Pinch zoom is disabled

### What TO Expect:

- ✅ Drag selection sets time filter
- ✅ Search runs automatically
- ✅ Chart always shows full view of current data
- ✅ You can select multiple times to narrow down
- ✅ Clear button resets to "All time"

## Comparison to Traditional Zoom

### Traditional Chart Zoom (NOT our behavior):
1. Drag to select region
2. Chart zooms into that region
3. Can pan left/right while zoomed
4. Double-click to reset zoom
5. Data remains unchanged

### Our Implementation (Time Filter):
1. Drag to select time region
2. **Time filter is set** (not zoom)
3. **Chart stays at full view**
4. **Search re-runs** with filter
5. **Data is filtered** to selected time range
6. Chart shows full view of filtered data

## Benefits of This Approach

1. **Clear Intent**: Selection = filter, not zoom
2. **Consistent Results**: Chart always shows what data exists
3. **No Confusion**: No need to reset zoom or pan around
4. **Fast Workflow**: One action (drag) = filtered results
5. **Predictable**: Chart behavior is consistent after every selection

## Integration with Manual Time Picker

Users have two ways to set time filters:

### Method 1: Chart Selection (Drag)
- Quick visual selection on existing chart
- Automatically captures exact time range
- Immediate search execution

### Method 2: Manual Time Picker (Modal)
- Click "🕐 Select Time Range" button
- Enter specific times via datetime inputs
- Click "Apply" to run search

**Both methods**:
- Update the same time filter
- Show time range in the same display
- Apply to the next search
- Can be cleared with "Clear" button

## URL Sharing

Time filters selected via chart drag are preserved in shared URLs:

```
http://localhost:8080/ui/index.html?q=level:ERROR&from=2025-01-14T13:30&to=2025-01-14T14:30
```

Anyone opening this URL will see results filtered to that exact time range.

## Conclusion

Chart time range selection is a **time filtering mechanism**, not a zoom tool. This provides a fast, intuitive way to drill down into specific time periods without the complexity of managing chart zoom states.
