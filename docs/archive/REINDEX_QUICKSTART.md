# Reindex Feature - Quick Start

## What is Reindex?

Reindex clears your index and re-reads the entire log file from the beginning. Use it when you've updated your parser configuration and want to reprocess existing logs.

## How to Reindex

### Step 1: Open Log Sources Page

Navigate to: **http://localhost:8080/ui/sources.html**

### Step 2: Find Your Log Source

Look for the log source in the "Active Log Sources" section.

### Step 3: Click Reindex

Click the orange **Reindex** button next to your log source.

```
┌─────────────────────────────────────────────────────────┐
│ /var/log/application.log                               │
│ Index: app_logs | Parser: grok | Status: Enabled       │
│                                                         │
│ [Edit] [Reindex] [Delete]                             │
│        └─────┘                                          │
│        Orange button - Click here!                      │
└─────────────────────────────────────────────────────────┘
```

### Step 4: Confirm

Read the confirmation dialog and click **OK** if you're sure.

### Step 5: Watch Progress

The Index Metrics section below will automatically refresh, showing the document count increasing as logs are re-indexed.

```
Index Metrics
┌─────────────────────────────────────────────────────┐
│ app_logs                                            │
│ Documents: 523 → 1,247 → 2,891 (↑ increasing)     │
│ Size: 156.2 KB → 312.8 KB → 724.1 KB              │
└─────────────────────────────────────────────────────┘
```

## Common Use Case: Update Parser & Reindex

### Problem
You added a new Grok pattern to extract a field from your logs, but existing indexed logs don't have this field.

### Solution
1. Click **Edit** on your log source
2. Update the **Parser Configuration** with your new Grok pattern
3. Click **Save Changes**
4. Click **Reindex** to apply the new parser to all existing logs
5. Wait for reindexing to complete
6. Now all logs (old and new) have the extracted field!

## Example Scenario

**Before Reindex:**
```
Parser: keyvalue
Indexed logs: 10,000
Fields extracted: timestamp, level, message
```

**Update Parser:**
```
Change to: grok
Pattern: %{TIMESTAMP_ISO8601:timestamp} %{LOGLEVEL:level} user=%{USERNAME:user} %{GREEDYDATA:message}
```

**After Reindex:**
```
Parser: grok
Indexed logs: 10,000 (same logs, reprocessed)
Fields extracted: timestamp, level, user, message (NEW!)
```

Now you can search by the `user` field: `user:john.doe`

## Important Notes

⚠️ **Warning**: Reindexing deletes all documents from the index first, then re-reads the log file. This means:
- You cannot undo a reindex
- Large files will take time to reprocess
- The operation runs in the background

✅ **Safe**: You can still search the index while reindexing is in progress (you'll just see fewer results initially as the index rebuilds)

## Quick Troubleshooting

| Problem | Solution |
|---------|----------|
| Can't see Reindex button | Refresh the page |
| Reindex seems stuck | Check Index Metrics for document count increasing |
| Reindex fails | Check logs at `service.log` for error details |
| Need to stop reindex | Restart the service (reindex will resume from where it left off) |

## What Gets Reindexed?

✅ **Yes - Reindexed:**
- All log entries in the file from beginning to end
- Parser configuration changes applied
- Field extractions updated
- Timestamps re-parsed

❌ **No - Not Affected:**
- The original log file (never modified)
- Other log sources
- Other indices
- Service configuration

## Time Estimates

| Log File Size | Estimated Reindex Time |
|---------------|------------------------|
| < 10 MB | Seconds |
| 10-100 MB | 1-5 minutes |
| 100 MB - 1 GB | 5-30 minutes |
| > 1 GB | 30+ minutes |

*Times vary based on system resources and parser complexity*

## When NOT to Reindex

- ❌ Just testing queries (use the search interface instead)
- ❌ Want to delete recent logs only (they'll be re-indexed)
- ❌ File hasn't changed and parser hasn't changed (no benefit)
- ❌ Running low on disk space (reindex needs space)

## REST API Alternative

Prefer command line? Use curl:

```bash
# Reindex a log source
curl -X POST http://localhost:8080/api/sources/YOUR_SOURCE_ID/reindex

# Response
{
  "message": "Reindexing started for source: YOUR_SOURCE_ID",
  "indexName": "app_logs",
  "filePath": "/var/log/app.log"
}
```

## Summary

1. **Navigate** to Log Sources page
2. **Click** orange Reindex button
3. **Confirm** the operation
4. **Monitor** Index Metrics for progress
5. **Done** when document count stops increasing

Need more details? See [REINDEX_GUIDE.md](REINDEX_GUIDE.md) and [REINDEX_UI_GUIDE.md](REINDEX_UI_GUIDE.md)
