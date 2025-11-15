# Time Refinement Feature

## Overview

This feature adds a Splunk-like time refinement capability to the Local Log Search UI. Users can click on timestamps in search results to refine their search by time using an intuitive popup menu.

## Features

### Clickable Timestamps
- All timestamps in search results are now clickable (displayed with blue underlined text)
- Clicking on a timestamp opens a popup menu with time refinement options

### Quick Actions
1. **Before this time** - Shows all logs before the selected timestamp
2. **After this time** - Shows all logs after the selected timestamp  
3. **Around this time (±5 min)** - Shows logs within 5 minutes before and after the selected timestamp

### Predefined Time Offsets

#### Before by...
- 1 second
- 5 seconds
- 1 minute
- 5 minutes
- 1 hour

Shows logs that occurred before (timestamp - offset)

#### After by...
- 1 second
- 5 seconds
- 1 minute
- 5 minutes
- 1 hour

Shows logs that occurred after (timestamp + offset)

#### Around by...
- ±1 second
- ±5 seconds
- ±1 minute
- ±5 minutes
- ±1 hour

Shows logs within the specified time range on both sides of the timestamp

### Custom Range
Users can specify a custom time offset with:
- **Amount**: Any positive number
- **Unit**: Milliseconds, Seconds, Minutes, Hours, or Days
- **Actions**: Before, After, or Around buttons

## Usage

1. Perform a search to get log results
2. Click on any timestamp in the results
3. Select a time refinement option from the popup menu
4. The search will automatically be re-executed with the new time filter applied

## Implementation Details

### Files Modified

1. **BOOT-INF/classes/static/ui/index.html**
   - Added CSS styles for the popup
   - Added HTML structure for the time refinement popup
   - Added JavaScript functions for popup management

2. **log-search-service/src/main/resources/static/ui/index.html** (main service file)
   - Same changes as above
   - Integrated with existing time picker modal

### Key Functions

- `showTimeRefinementPopup(timestamp, x, y)` - Displays popup at cursor position
- `hideTimeRefinementPopup()` - Closes the popup
- `getMilliseconds(amount, unit)` - Converts time amounts to milliseconds
- `applyTimeRefinement(action, amount, unit)` - Applies selected time filter to search

### Integration
- Works with existing time range functionality
- Updates the time picker inputs when a refinement is selected
- Automatically triggers a new search with the updated time range
- Closes on outside click or when an option is selected

## User Experience

- Clean, modern UI with hover effects
- Positioned near click location but stays on screen
- Clear visual indicators with icons (◀, ▶, ◆)
- Tooltip on timestamps: "Click to refine search by time"
- Non-blocking - can be closed by clicking outside

## Testing

To test the feature:

1. Start the Local Log Search service:
   ```bash
   ./start-service.sh
   ```

2. Open the UI in a browser: http://localhost:8080/ui/

3. Perform a search that returns results with timestamps

4. Click on any timestamp to see the time refinement popup

5. Try different refinement options to verify they work correctly

## Future Enhancements

Potential improvements:
- Remember last used custom time offset
- Add "Last N minutes/hours" quick options
- Support for relative time ranges (e.g., "last hour", "today")
- Keyboard shortcuts for common actions
- Visual timeline showing selected time range
