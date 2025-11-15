# Reindex UI Feature Guide

## Overview

The reindex feature is now fully integrated into the Log Sources UI, allowing you to easily reindex log files directly from the web interface.

## Accessing the Reindex Feature

1. Navigate to **Log Sources** page (http://localhost:8080/ui/sources.html)
2. Find the log source you want to reindex in the "Active Log Sources" section
3. Click the orange **Reindex** button next to the source

## UI Components

### Reindex Button

Each log source now has three action buttons:
- **Edit** (blue) - Modify log source configuration
- **Reindex** (orange/warning) - Clear and re-read the entire log file
- **Delete** (red) - Remove the log source

The Reindex button is styled in orange/yellow to indicate it's a potentially destructive operation that requires caution.

### Confirmation Dialog

When you click the Reindex button, you'll see a confirmation dialog that explains:

```
Reindex log source: /path/to/logfile.log?

This will:
1. Clear all documents from index "index_name"
2. Re-read the entire log file from the beginning
3. Re-index all log entries

This cannot be undone!
```

Click **OK** to proceed or **Cancel** to abort.

### Progress Monitoring

After confirming:

1. **Immediate Feedback**: A success message appears showing "Starting reindex for: /path/to/logfile.log..."

2. **Background Processing**: The reindex happens asynchronously in the background

3. **Automatic Refresh**: The Index Metrics section automatically refreshes every 3 seconds for 30 seconds to show progress

4. **Document Count**: Watch the document count in the Index Metrics section increase as the log file is re-read

## Example Workflow

### Scenario: Update Parser Configuration and Reindex

```plaintext
1. Click "Edit" on a log source
2. Update the parser configuration (e.g., change Grok pattern)
3. Click "Save Changes"
4. Click "Reindex" to apply the new parser to existing logs
5. Monitor the Index Metrics section to see re-indexing progress
```

### Visual Flow

```
┌─────────────────────────────────────────┐
│  Log Sources Page                       │
├─────────────────────────────────────────┤
│  Active Log Sources                     │
│  ┌─────────────────────────────────┐   │
│  │ /var/log/app.log                │   │
│  │ Index: app_logs                 │   │
│  │ Parser: grok | Status: Enabled  │   │
│  │                                 │   │
│  │ [Edit] [Reindex] [Delete]      │   │
│  └─────────────────────────────────┘   │
│                                         │
│  Index Metrics                          │
│  ┌─────────────────────────────────┐   │
│  │ app_logs                        │   │
│  │ Documents: 1,523 (↑ increasing) │   │
│  │ Size: 239.9 KB                  │   │
│  │                                 │   │
│  │ [Clear] [Delete]                │   │
│  └─────────────────────────────────┘   │
└─────────────────────────────────────────┘
```

## JavaScript Implementation

### Function Signature

```javascript
async function reindexSource(id, filePath, indexName)
```

### Key Features

1. **Confirmation**: Shows detailed confirmation dialog with all consequences
2. **Error Handling**: Catches and displays errors gracefully
3. **Progress Tracking**: Automatically refreshes index metrics to show progress
4. **Status Messages**: Shows success/error messages to the user
5. **Auto-refresh**: Updates index metrics 10 times at 3-second intervals to show real-time progress

### API Call

```javascript
const response = await fetch(`/api/sources/${encodeURIComponent(id)}/reindex`, {
    method: 'POST'
});
```

## User Experience Notes

### Success Flow
1. User clicks Reindex → Confirmation dialog appears
2. User confirms → Success message: "Starting reindex for: /path/to/file.log..."
3. API responds → Updated message: "Reindexing started for source: source_id. The log file will be re-indexed in the background."
4. Index metrics refresh every 3 seconds showing increasing document count
5. After 30 seconds, auto-refresh stops (user can manually refresh if needed)

### Error Flow
1. User clicks Reindex → Confirmation dialog appears
2. User confirms → Success message appears
3. API returns error → Error message: "Error: [specific error message]"
4. User can retry or investigate the issue

## CSS Styling

### Button Style

```css
button.warning {
    background: #f39c12;
}
button.warning:hover {
    background: #e67e22;
}
```

The orange/amber color scheme:
- Distinguishes reindex from normal operations (blue Edit button)
- Indicates caution without being as severe as the red Delete button
- Provides good visual hierarchy and usability

## When to Use Reindex

✅ **Good Use Cases:**
- After updating parser configuration to reprocess existing logs
- When you've modified Grok patterns or field extraction rules
- To recover from a corrupted index
- After testing with sample data and wanting a fresh start
- When troubleshooting parser issues with real log data

❌ **When NOT to Use:**
- For routine operations (indexes update automatically as logs are written)
- When you just want to clear recent data (use time-based queries instead)
- As a substitute for proper log rotation/archival
- When the log file is extremely large and reindexing would take hours

## Performance Considerations

- **Large Files**: Reindexing a large log file (GB+) can take significant time
- **CPU/Memory**: The reindex process will consume system resources
- **Concurrent Queries**: Search queries can still run during reindexing
- **Progress**: Watch the document count to estimate completion

## Troubleshooting

### Reindex Button Doesn't Appear
- Check that you're on the Log Sources page (http://localhost:8080/ui/sources.html)
- Refresh the page to load the latest UI changes
- Verify the log source exists and is listed

### Reindex Fails
- Check application logs for detailed error messages
- Verify the log file is accessible and readable
- Ensure sufficient disk space for the index
- Check that the index isn't locked by another process

### Progress Seems Stuck
- Large files take time - be patient
- Manually refresh the Index Metrics section
- Check application logs for processing status
- Verify the file tailer is running

## Related Features

- **Edit Source**: Update parser configuration before reindexing
- **Clear Index**: Remove all documents without re-reading (from Index Metrics)
- **Delete Index**: Remove entire index directory (from Index Metrics)
- **Delete Source**: Stop tailing and remove log source configuration

## Technical Details

The reindex operation performs three steps sequentially:

1. **Clear Index**: Calls `IndexManager.clearIndex(indexName)`
   - Closes existing IndexWriter
   - Deletes all documents from the Lucene index
   - Commits the empty index

2. **Remove Checkpoint**: Calls `TailerStateRepository.remove(sourceId)`
   - Deletes the file position checkpoint
   - Forces tailer to start from beginning

3. **Restart Tailer**: Calls `TailerManager.reindexLogSource(config)`
   - Stops existing tailer
   - Starts new tailer with null initial state
   - Begins reading from start of file

All operations are atomic at each step but the overall process is asynchronous.
