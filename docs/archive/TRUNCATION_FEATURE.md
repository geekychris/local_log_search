# Index Truncation Feature

This document describes the new index truncation feature added to the Local Log Search service.

## Overview

The truncation feature allows you to automatically or manually limit the size of Lucene indices by removing old log entries based on either time or document volume criteria.

## Features

### Truncation Policies

1. **Time-Based Truncation**
   - Keeps only logs from the last N hours/days
   - Example: "Keep last 24 hours of data" or "Keep last 7 days"
   - Deletes documents older than the specified retention period

2. **Volume-Based Truncation**
   - Keeps only the most recent N documents
   - Example: "Keep last 1,000,000 events"
   - Deletes oldest documents when limit is exceeded

3. **None**
   - No automatic truncation (default)

### Automatic vs Manual Truncation

- **Automatic Truncation**: Set up a schedule to automatically truncate indices at regular intervals
  - Time-based default interval: Same as retention period (max 1 day)
  - Volume-based default interval: Every hour
  - Custom intervals can be configured

- **Manual Truncation**: Trigger truncation on-demand from the UI or API

## How to Use

### Via Web UI

1. Navigate to the "Log Sources" page (`/ui/sources.html`)
2. In the "Index Metrics" section, find the index you want to configure
3. Click the **"Truncate"** button next to the index
4. Configure your truncation policy:
   - Select policy type (Time-Based or Volume-Based)
   - For Time-Based: Enter retention period (e.g., 1 day, 24 hours)
   - For Volume-Based: Enter maximum documents (e.g., 1000000)
   - Optionally enable automatic truncation and set check interval
5. Click **"Save & Apply Configuration"** to save and schedule (if auto-truncate is enabled)
6. Or click **"Truncate Now (Manual)"** to immediately truncate based on saved configuration

### Via REST API

#### Get Truncation Configuration
```bash
GET /api/indices/{indexName}/truncation
```

#### Set Truncation Configuration (Time-Based)
```bash
POST /api/indices/{indexName}/truncation
Content-Type: application/json

{
  "policy": "TIME_BASED",
  "retentionValue": 1,
  "retentionUnit": "days",
  "autoTruncateEnabled": true,
  "intervalValue": 24,
  "intervalUnit": "hours"
}
```

#### Set Truncation Configuration (Volume-Based)
```bash
POST /api/indices/{indexName}/truncation
Content-Type: application/json

{
  "policy": "VOLUME_BASED",
  "maxDocuments": 1000000,
  "autoTruncateEnabled": true,
  "intervalValue": 1,
  "intervalUnit": "hours"
}
```

#### Manual Truncation
```bash
POST /api/indices/{indexName}/truncate
```

#### Remove Truncation Configuration
```bash
DELETE /api/indices/{indexName}/truncation
```

#### Get All Truncation Configurations
```bash
GET /api/indices/truncation
```

## Implementation Details

### Architecture

The truncation feature consists of several components:

1. **Model Classes** (`log-search-core/src/main/java/com/locallogsearch/core/truncation/`)
   - `TruncationPolicy`: Enum defining policy types (TIME_BASED, VOLUME_BASED, NONE)
   - `TruncationConfig`: Configuration class for truncation policies

2. **Index Manager** (`log-search-core/src/main/java/com/locallogsearch/core/index/IndexManager.java`)
   - `truncateByTime()`: Deletes documents older than a cutoff timestamp
   - `truncateByVolume()`: Keeps only the N most recent documents
   - `truncateIndex()`: Executes truncation based on configuration

3. **Truncation Scheduler** (`log-search-service/src/main/java/com/locallogsearch/service/truncation/TruncationScheduler.java`)
   - Manages automatic truncation schedules
   - Persists configurations to disk
   - Executes scheduled truncation tasks

4. **REST Controller** (`log-search-service/src/main/java/com/locallogsearch/service/controller/IndexController.java`)
   - Endpoints for managing truncation configurations
   - Endpoint for manual truncation

5. **Repository** (`log-search-service/src/main/java/com/locallogsearch/service/repository/TruncationConfigRepository.java`)
   - Persists truncation configurations to `state/truncation-configs.json`

### How Truncation Works

#### Time-Based Truncation
1. Calculates cutoff timestamp: `now - retentionPeriod`
2. Uses Lucene's `deleteDocuments()` with a range query: `timestamp:[0 TO cutoffTimestamp]`
3. Commits the deletions to the index

#### Volume-Based Truncation
1. Counts total documents in the index
2. If count exceeds `maxDocuments`:
   - Sorts documents by timestamp descending
   - Finds the timestamp of the Nth newest document (where N = maxDocuments)
   - Deletes all documents older than that timestamp
3. Commits the deletions to the index

### Persistence

Truncation configurations are persisted to `state/truncation-configs.json` and automatically restored when the application restarts. Scheduled truncations resume automatically.

## Examples

### Example 1: Keep Last 24 Hours (Auto-Truncate Every Hour)
```json
{
  "policy": "TIME_BASED",
  "retentionValue": 24,
  "retentionUnit": "hours",
  "autoTruncateEnabled": true,
  "intervalValue": 1,
  "intervalUnit": "hours"
}
```

### Example 2: Keep Last 7 Days (Auto-Truncate Daily)
```json
{
  "policy": "TIME_BASED",
  "retentionValue": 7,
  "retentionUnit": "days",
  "autoTruncateEnabled": true,
  "intervalValue": 1,
  "intervalUnit": "days"
}
```

### Example 3: Keep Last 1 Million Events (Auto-Truncate Hourly)
```json
{
  "policy": "VOLUME_BASED",
  "maxDocuments": 1000000,
  "autoTruncateEnabled": true,
  "intervalValue": 1,
  "intervalUnit": "hours"
}
```

### Example 4: Manual Truncation Only
```json
{
  "policy": "TIME_BASED",
  "retentionValue": 30,
  "retentionUnit": "days",
  "autoTruncateEnabled": false
}
```

## Notes

- Truncation is a destructive operation and cannot be undone
- For time-based truncation, documents must have timestamps indexed
- For volume-based truncation, documents are sorted by timestamp
- Deleted documents are physically removed during Lucene's merge process
- Automatic truncation runs in the background and doesn't block indexing
- Configuration changes take effect immediately for scheduled tasks

## Files Modified/Created

### New Files
- `log-search-core/src/main/java/com/locallogsearch/core/truncation/TruncationPolicy.java`
- `log-search-core/src/main/java/com/locallogsearch/core/truncation/TruncationConfig.java`
- `log-search-service/src/main/java/com/locallogsearch/service/truncation/TruncationScheduler.java`
- `log-search-service/src/main/java/com/locallogsearch/service/repository/TruncationConfigRepository.java`

### Modified Files
- `log-search-core/src/main/java/com/locallogsearch/core/index/IndexManager.java`
  - Added truncation methods
- `log-search-service/src/main/java/com/locallogsearch/service/controller/IndexController.java`
  - Added truncation REST endpoints
- `log-search-service/src/main/resources/static/ui/sources.html`
  - Added truncation UI modal and controls

## Building

To build the project with the new truncation feature:

```bash
cd /path/to/local_log_search
mvn clean install -DskipTests
```

## Running

Start the service:

```bash
cd log-search-service
mvn spring-boot:run
```

Then navigate to http://localhost:8080/ui/sources.html to configure truncation.
