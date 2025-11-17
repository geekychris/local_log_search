# Testing the Timestamp Click Feature

## Overview
The date/time refinement popup feature has been successfully implemented and deployed. This guide will help you test it.

## What Was Done
1. ✅ Rebuilt the application with `mvn clean package -DskipTests`
2. ✅ Restarted the service with the updated code
3. ✅ Verified the timestamp-clickable CSS class is in the packaged JAR
4. ✅ Verified the click handler JavaScript is in place
5. ✅ Confirmed search results include timestamp data

## How to Test

### Step 1: Access the UI
Open your browser and navigate to: **http://localhost:8080/ui/**

### Step 2: Perform a Search
1. In the search box, enter: `level:ERROR`
2. Click the **Search** button
3. Wait for results to appear

### Step 3: Click a Timestamp
Look for search results in the Results section. Each result should show:
```
Time: [DATE/TIME HERE] | Index: app-logs | Score: 1.36
```

The **timestamp** (date/time) should be:
- **Underlined with a dotted line**
- **Blue in color** (#3498db)
- **Show a pointer cursor** when you hover over it
- **Show a tooltip**: "Click to refine search by time"

### Step 4: Use the Popup
When you click on a timestamp, a popup menu should appear with these options:

**Quick Actions:**
- Before this time
- After this time
- Around this time (±5 min)

**Before by...**
- 1 second, 5 seconds, 1 minute, 5 minutes, 1 hour

**After by...**
- 1 second, 5 seconds, 1 minute, 5 minutes, 1 hour

**Around by...**
- ±1 second, ±5 seconds, ±1 minute, ±5 minutes, ±1 hour

**Custom Range:**
- Input field for amount and unit selection

### Step 5: Verify Behavior
After clicking any option:
1. The popup should close
2. The time range should update in the main search form
3. The search should automatically re-run with the new time filter
4. You should see a green success message showing the new time range

## Troubleshooting

### If timestamps don't appear clickable:

1. **Hard refresh the page**: Press `Cmd+Shift+R` (Mac) or `Ctrl+Shift+R` (Windows/Linux)
2. **Clear browser cache**: 
   - Chrome: Settings → Privacy and Security → Clear browsing data → Cached images and files
   - Safari: Develop → Empty Caches (or enable Develop menu first)
   - Firefox: Preferences → Privacy & Security → Cookies and Site Data → Clear Data

3. **Check browser console for errors**:
   - Press `F12` or `Cmd+Option+I` (Mac) / `Ctrl+Shift+I` (Windows/Linux)
   - Look for any JavaScript errors in the Console tab

4. **Verify service is running**:
   ```bash
   ps aux | grep log-search-service | grep -v grep
   ```

5. **Check service logs**:
   ```bash
   tail -f /tmp/log-search-service.log
   ```

### If the popup doesn't appear:

1. Check browser console for JavaScript errors
2. Verify the timestamp has the `timestamp-clickable` class:
   - Right-click on the timestamp → Inspect Element
   - Look for: `<span class="timestamp-clickable" data-timestamp="...">...</span>`

3. Check if the event listener is attached:
   - Open browser console
   - Run: `document.querySelectorAll('.timestamp-clickable').length`
   - Should return a number > 0 if results are displayed

## Technical Details

### CSS Classes
- `.timestamp-clickable`: Makes timestamps blue, underlined, and clickable
- `.time-refinement-popup`: The popup container
- `.time-refinement-popup.active`: Shows the popup

### JavaScript Functions
- `showTimeRefinementPopup(timestamp, x, y)`: Shows the popup at mouse position
- `hideTimeRefinementPopup()`: Hides the popup
- `applyTimeRefinement(action, amount, unit)`: Applies the selected time filter

### Data Flow
1. Search results include `timestamp` field in ISO format
2. JavaScript renders timestamp as clickable span
3. Click handler captures event and shows popup
4. User selects option → time range is set → search re-runs

## Service Status

Current service status:
- **Running**: Yes (PID: 11305)
- **Port**: 8080
- **Log file**: /tmp/log-search-service.log
- **Available indices**: app-logs, error-logs, access-logs, search-log, generator-index, service-index

## Quick Commands

**Restart service:**
```bash
cd /Users/chris/code/warp_experiments/local_log_search
pkill -f log-search-service
cd log-search-service
nohup java -jar target/log-search-service-1.0-SNAPSHOT.jar > /tmp/log-search-service.log 2>&1 &
```

**Rebuild and restart:**
```bash
cd /Users/chris/code/warp_experiments/local_log_search
mvn clean package -DskipTests
pkill -f log-search-service
cd log-search-service
nohup java -jar target/log-search-service-1.0-SNAPSHOT.jar > /tmp/log-search-service.log 2>&1 &
```

**Check if running:**
```bash
curl -s http://localhost:8080/api/indices | jq '.[] | .name'
```
