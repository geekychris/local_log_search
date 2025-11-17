# Millisecond-Resolution Time Filtering

## Overview
The time filtering system has been updated to support millisecond-resolution timestamps, allowing precise time-based filtering down to the millisecond level.

## What Changed

### 1. Input Format
**Before:** `datetime-local` inputs (limited to minute resolution)
```html
<input type="datetime-local" id="timeFrom">
<!-- Result: 2025-11-15T10:30 (loses seconds and milliseconds) -->
```

**After:** Text inputs accepting full ISO 8601 format with milliseconds
```html
<input type="text" id="timeFrom" placeholder="2025-11-15T10:30:45.123Z">
<!-- Result: 2025-11-15T10:30:45.123Z (preserves milliseconds) -->
```

### 2. Time Storage Format
- **Internal format:** Full ISO 8601 strings with milliseconds (e.g., `2025-11-15T10:30:45.123Z`)
- **Display format:** Locale string with milliseconds appended (e.g., `11/15/2025, 10:30:45 AM.123`)
- **API format:** Unix epoch milliseconds (converted from ISO string)

### 3. Timestamp Popup Refinement
When clicking timestamps in search results, the popup now preserves full precision:
- "5 seconds" option now truly filters to 5-second windows
- "1 second" option provides sub-second precision
- Custom millisecond ranges are supported

### 4. Time Picker Modal Enhancements
**New Features:**
- Text inputs with ISO format validation
- "Now" buttons to quickly populate current timestamp with milliseconds
- Format helper text: `YYYY-MM-DDTHH:mm:ss.sssZ`
- Validation alerts for invalid timestamp formats

## Usage Examples

### Using the Time Picker Modal

1. **Manual Entry:**
   ```
   From: 2025-11-15T10:30:45.123Z
   To:   2025-11-15T10:30:50.456Z
   ```
   This creates a 5.333-second window.

2. **Using "Now" Buttons:**
   - Click "Now" button next to From/To fields
   - Automatically populates with current time including milliseconds
   - Example: `2025-11-15T18:01:23.789Z`

3. **Partial Timestamps:**
   The system accepts various ISO formats:
   - Full: `2025-11-15T10:30:45.123Z`
   - No millis: `2025-11-15T10:30:45Z` (treated as .000)
   - No seconds: `2025-11-15T10:30Z` (treated as :00.000)

### Using Timestamp Click Popup

When you click a timestamp in search results:

**Quick Actions:**
- **Before this time:** All events before the clicked timestamp
- **After this time:** All events after the clicked timestamp  
- **Around this time (±5 min):** 10-minute window centered on timestamp

**Precise Options:**
- **1 second:** Events within 1 second before the timestamp
- **5 seconds:** Events within 5 seconds before the timestamp
- All millisecond precision is preserved

**Custom Range:**
- Amount: `5`
- Unit: `Seconds`
- Click "Before" → Shows events from 5 seconds before the clicked timestamp

## Technical Details

### Data Flow

1. **User Input:**
   ```javascript
   timeFrom.value = "2025-11-15T10:30:45.123Z"
   ```

2. **Validation:**
   ```javascript
   const date = new Date(value);
   if (isNaN(date.getTime())) {
       alert('Invalid timestamp format');
   }
   ```

3. **Storage:**
   ```javascript
   currentTimeRange.from = date.toISOString();
   // Stores: "2025-11-15T10:30:45.123Z"
   ```

4. **API Conversion:**
   ```javascript
   timestampFrom = new Date(currentTimeRange.from).getTime();
   // Sends: 1731677445123 (Unix epoch milliseconds)
   ```

5. **Backend Processing:**
   - Backend receives millisecond-precision epoch timestamp
   - Lucene query uses exact millisecond values
   - Results include full timestamp precision

### Backwards Compatibility

The system gracefully handles legacy timestamps:
- Old format (minutes only): Converted to `:00.000Z`
- New format (with millis): Used as-is
- History entries: Updated automatically on next save

### Display Format

Time ranges are displayed with millisecond precision:
```javascript
const fromStr = new Date(currentTimeRange.from).toLocaleString() 
    + '.' + new Date(currentTimeRange.from).getMilliseconds();
// Result: "11/15/2025, 10:30:45 AM.123"
```

## Testing

### Test Case 1: Millisecond Precision Filter
1. Open UI: http://localhost:8080/ui/
2. Search: `level:ERROR`
3. Click on a timestamp
4. Select "5 seconds" → "After by..."
5. Verify the time filter shows exact timestamp with milliseconds
6. Verify search results are within 5-second window

### Test Case 2: Manual Millisecond Entry
1. Click "🕐 Select Time Range"
2. Enter From: `2025-11-14T01:25:32.383Z`
3. Enter To: `2025-11-14T01:25:32.500Z`
4. Click Apply
5. Search: `level:ERROR`
6. Verify only events in that 117-millisecond window appear

### Test Case 3: "Now" Button
1. Click "🕐 Select Time Range"
2. Click "Now" button for From field
3. Observe format: `2025-11-15T18:01:23.789Z`
4. Verify milliseconds are included and changing

### Test Case 4: Custom Millisecond Range
1. Search and click a timestamp
2. In popup, set custom range:
   - Amount: `500`
   - Unit: `Milliseconds`
3. Click "Around"
4. Verify ±500ms window is created

## API Request Format

The search API now receives:
```json
{
  "indices": ["app-logs"],
  "query": "level:ERROR",
  "timestampFrom": 1731677445123,
  "timestampTo": 1731677445500,
  "page": 0,
  "pageSize": 50
}
```

Where `timestampFrom` and `timestampTo` are Unix epoch milliseconds with full precision.

## Browser Console Testing

You can verify millisecond precision in browser console:
```javascript
// Check stored time range
console.log(currentTimeRange);
// Output: {from: "2025-11-15T10:30:45.123Z", to: "2025-11-15T10:30:50.456Z"}

// Check milliseconds are preserved
new Date(currentTimeRange.from).getMilliseconds()
// Output: 123
```

## Common Patterns

### Find events in 1-second window
1. Click timestamp: `2025-11-14T01:25:32.383Z`
2. Select "Around by..." → "±1 second"
3. Result: Events from `.383` to `33.383` (2-second total window)

### Find events within milliseconds
1. Click timestamp
2. Custom: `100` milliseconds, "Around"
3. Result: Events from `.283Z` to `.483Z` (200ms total window)

### Filter last 5 seconds of logs
1. Click most recent timestamp
2. Select "Before by..." → "5 seconds"
3. Result: Last 5 seconds before that timestamp

## Troubleshooting

### Time filter not working
- **Check format:** Must be valid ISO 8601
- **Check timezone:** Use `Z` for UTC or include offset like `+00:00`
- **Check milliseconds:** Use 3 digits (000-999)

### Invalid timestamp error
```
Invalid "From" timestamp format. Use ISO format: YYYY-MM-DDTHH:mm:ss.sssZ
```
**Solution:** Ensure format matches exactly, including the `T` and `Z`

### Milliseconds showing as 0
- JavaScript `getMilliseconds()` only returns millisecond component (0-999)
- Use `getTime()` for full epoch milliseconds
- Display uses `toLocaleString()` + `.getMilliseconds()`

## Service Status

- **Running:** Yes (PID: 12658)
- **Port:** 8080
- **Version:** 1.0-SNAPSHOT with millisecond precision support
- **Log file:** /tmp/log-search-service.log

## Files Modified

1. `log-search-service/src/main/resources/static/ui/index.html`
   - Changed input type from `datetime-local` to `text`
   - Added "Now" buttons
   - Updated validation logic
   - Preserved milliseconds in all conversions
   - Added format helpers and placeholders
