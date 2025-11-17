# Quick Start: Millisecond Time Filtering

## ✅ Changes Deployed

The service now supports **millisecond-resolution time filtering**!

**Service Status:**
- ✅ Rebuilt with millisecond support
- ✅ Running on port 8080 (PID: 12658)
- ✅ Ready to test

## 🚀 Quick Test

### Option 1: Use Timestamp Click Popup

1. Open http://localhost:8080/ui/
2. Search: `level:ERROR`
3. **Click on any timestamp** in the results
4. Select **"5 seconds"** from the "Before by..." section
5. **Look at the time inputs** - they now show full ISO format with milliseconds!

### Option 2: Manual Time Entry

1. Open http://localhost:8080/ui/
2. Click **"🕐 Select Time Range"**
3. You'll see text inputs instead of datetime pickers
4. Click **"Now"** button - it populates: `2025-11-15T18:01:23.789Z`
5. Manually edit to add specific milliseconds: `2025-11-15T10:30:45.123Z`
6. Click **Apply** and search

### Option 3: Precise Time Window

**Find all logs in a 5-second window:**

1. Search: `level:ERROR`
2. Click on a timestamp (e.g., `11/14/2025, 1:25:32 AM`)
3. In popup, select **"Around by..."** → **"±5 seconds"**
4. Time filter automatically updates with **exact timestamp including milliseconds**
5. Results show events within exactly 10 seconds (±5s)

## 📋 Key Changes

| Feature | Before | After |
|---------|--------|-------|
| Input Type | `datetime-local` | `text` with ISO format |
| Resolution | Minutes only | Milliseconds |
| Format | `2025-11-15T10:30` | `2025-11-15T10:30:45.123Z` |
| Precision | ±60 seconds | ±1 millisecond |
| Helper | None | "Now" buttons |

## 🎯 Example Scenarios

### Scenario 1: Debug a specific moment
You have a timestamp from an error: `2025-11-14T01:25:32.383Z`

**Before:** Had to search entire minute (60 seconds of logs)
**Now:** Search exact millisecond or ±100ms window

```
From: 2025-11-14T01:25:32.283Z
To:   2025-11-14T01:25:32.483Z
Result: 200ms window (instead of 60,000ms)
```

### Scenario 2: Find rapid succession events
Multiple events happened within 5 seconds:

**Before:** Minimum 1-minute window, saw ~60 seconds of logs
**Now:** Use "5 seconds" option, see exactly 5 seconds

### Scenario 3: High-frequency logging
Your app logs 1000 events/second:

**Before:** 60,000 events per search (1 minute)
**Now:** 5 events per search (5ms window)

## 💡 Tips

1. **Use "Now" buttons** - Fastest way to get correct format
2. **Click timestamps** - Popup preserves full precision
3. **Custom milliseconds** - Set amount to `500`, unit to `Milliseconds`
4. **Partial timestamps work** - `2025-11-15T10:30Z` is valid (treated as :00.000)

## 🔧 Format Examples

Valid timestamp formats:
```
2025-11-15T10:30:45.123Z       ✓ Full format with milliseconds
2025-11-15T10:30:45Z           ✓ No milliseconds (treated as .000)
2025-11-15T10:30Z              ✓ No seconds (treated as :00.000)
2025-11-15T10:30:45.1Z         ✓ Partial millis (treated as .100)
2025-11-15T10:30:45+00:00      ✓ With timezone offset
```

Invalid formats:
```
2025-11-15 10:30:45            ✗ Missing 'T' separator
11/15/2025 10:30:45            ✗ Not ISO format
2025-11-15T10:30:45.123        ✗ Missing 'Z' or timezone
```

## 🧪 Browser Console Test

Open browser console (F12) and test:

```javascript
// After setting a time range, check precision:
console.log(currentTimeRange);
// Output: {from: "2025-11-15T10:30:45.123Z", to: "2025-11-15T10:30:50.456Z"}

// Verify milliseconds:
new Date(currentTimeRange.from).getMilliseconds()
// Output: 123

// Check epoch milliseconds sent to API:
new Date(currentTimeRange.from).getTime()
// Output: 1731677445123
```

## 📖 Full Documentation

For complete details, see: `MILLISECOND_TIME_FILTERING.md`

## 🐛 Troubleshooting

**Problem:** Time filter shows "Invalid timestamp format"
**Solution:** Ensure format includes `T` separator and ends with `Z` or timezone

**Problem:** Milliseconds showing as 0
**Solution:** This is normal - `getMilliseconds()` returns 0-999 component only

**Problem:** Changes not visible
**Solution:** Hard refresh browser (Cmd+Shift+R or Ctrl+Shift+R)

## 🎉 What This Enables

✅ Debug specific moments in high-frequency logs
✅ Investigate rapid event sequences  
✅ Precise performance analysis (sub-second timing)
✅ Accurate log correlation across systems
✅ Fine-grained time-series analysis

Enjoy millisecond-precision log searching! 🚀
