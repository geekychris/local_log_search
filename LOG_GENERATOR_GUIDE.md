# Enhanced Log Generator Guide

## Overview

The test log generator now creates **11 diverse log classes** representing major system components. It supports multiple generation modes for testing different scenarios.

---

## Log Classes (11 Total)

### 1. **Service** (`class=service`) - 12%
Microservice operations
- Fields: `service`, `operation`, `duration`, `requestId`, `status`
- Services: auth-service, payment-service, notification-service, analytics-service, reporting-service

### 2. **Database** (`class=database`) - 12%
Database query tracking
- Fields: `database`, `queryType`, `table`, `queryTime`, `rowsAffected`, `pool`
- Databases: postgres, mysql, mongodb, redis, elasticsearch

### 3. **Cache** (`class=cache`) - 10%
Cache operations
- Fields: `operation`, `key`, `result`, `ttl`, `hitRate`
- Operations: get, set, delete, expire, flush

### 4. **Auth** (`class=auth`) - 10%
Authentication and sessions
- Fields: `action`, `user`, `ip`, `success`, `sessionId`, `mfa`, `sessionDuration`
- Actions: login, logout

### 5. **Payment** (`class=payment`) - 10%
Payment transactions
- Fields: `transactionId`, `amount`, `currency`, `gateway`, `status`, `processingTime`
- Gateways: stripe, paypal
- Currencies: USD, EUR, GBP

### 6. **Business** (`class=business`) - 10%
General business logic
- Fields: `operation`, `user`, `duration`, `requestId`, `status`

###7. **Queue** (`class=queue`) - 10%
Message queue operations
- Fields: `queue`, `action`, `queueSize`, `messageAge`, `processingTime`, `lag`
- Queues: orders-queue, notifications-queue, analytics-queue, email-queue

### 8. **API Gateway** (`class=api-gateway`) - 8%
API gateway requests
- Fields: `method`, `endpoint`, `version`, `status`, `responseTime`, `clientId`, `rateLimitRemaining`
- Versions: v1, v2, v3

### 9. **Container** (`class=container`) - 8%
Container/orchestration metrics
- Fields: `container`, `region`, `cpuPercent`, `memoryMB`, `restartCount`, `status`
- Regions: us-east-1, us-west-2, eu-west-1, ap-southeast-1
- Containers: api-server-1, api-server-2, worker-1, worker-2, scheduler-1

### 10. **Storage** (`class=storage`) - 5%
Storage operations
- Fields: `storage`, `operation`, `bucket`, `bytes`, `latency`, `throughput`
- Storage types: s3, nfs, local-disk

### 11. **Search** (`class=search`) - 5%
Search engine operations
- Fields: `index`, `queryType`, `hits`, `searchTime`, `shards`, `cacheHit`
- Indices: products, users, orders, logs

---

## Generation Modes

### 1. Continuous Mode (Default)
Generates logs continuously until stopped.

```bash
java -jar test-log-generator.jar
```

### 2. Limited Mode
Generate a specific number of logs then stop.

```bash
# Generate 10,000 logs
java -jar test-log-generator.jar --max-logs 10000
```

### 3. Slow/Throttled Mode
Add delay between log entries to prevent flooding.

```bash
# Generate 1 log per second
java -jar test-log-generator.jar --delay 1000

# Generate 10 logs per second
java -jar test-log-generator.jar --delay 100
```

### 4. Historical Mode - Days Back
Generate logs with timestamps starting N days ago, incrementing over time.

```bash
# Generate logs spanning last 7 days
java -jar test-log-generator.jar --days-back 7 --max-logs 10000

# Generate logs from 30 days ago
java -jar test-log-generator.jar --days-back 30 --max-logs 50000
```

### 5. Historical Mode - Specific Date
Start from a specific date/time.

```bash
# Start from January 1, 2025
java -jar test-log-generator.jar \
  --start-date 2025-01-01T00:00:00Z \
  --max-logs 5000
```

### 6. Combined Modes
Mix and match options for specific scenarios.

```bash
# Generate 1 month of historical logs at 100 logs/sec
java -jar test-log-generator.jar \
  --days-back 30 \
  --max-logs 100000 \
  --delay 10

# Slowly generate last week's logs (1 per second)
java -jar test-log-generator.jar \
  --days-back 7 \
  --max-logs 5000 \
  --delay 1000
```

---

## Command-Line Options

| Option | Description | Default |
|--------|-------------|---------|
| `--max-logs <N>` | Stop after generating N logs | unlimited |
| `--delay <ms>` | Delay in milliseconds between logs | 0 (no delay) |
| `--days-back <N>` | Start N days ago, increment timestamps | current time |
| `--start-date <ISO>` | Start from specific ISO date | current time |
| `--help` | Show help message | - |

---

## Use Cases

### Testing Time-Based Queries

**Generate a week of data:**
```bash
./generate-historical-logs.sh
```

Or manually:
```bash
java -jar test-log-generator.jar --days-back 7 --max-logs 10000
```

Then in the search UI, try:
- Select last 24 hours in time picker
- Use timechart queries: `class:payment | timechart span=1h sum(amount)`
- Filter by date ranges

### Load Testing

**Generate lots of logs quickly:**
```bash
java -jar test-log-generator.jar --max-logs 100000
```

### Development/Demo

**Slow, continuous generation:**
```bash
java -jar test-log-generator.jar --delay 500
```

### Backfilling Historical Data

**Create 30 days of logs:**
```bash
java -jar test-log-generator.jar \
  --days-back 30 \
  --max-logs 50000
```

---

## Sample Queries for New Classes

### Queue Monitoring
```
class:queue AND lag:high
class:queue | stats avg(queueSize) by queue
class:queue | timechart span=5m avg(queueSize) by queue
```

### API Gateway Analytics
```
class:api-gateway AND status:[400 TO 599]
class:api-gateway | stats count by version, status
class:api-gateway | stats avg(responseTime) by endpoint
```

### Container Health
```
class:container AND status:critical
class:container | stats avg(cpuPercent), avg(memoryMB) by container
class:container AND region:us-east-1 | timechart span=10m avg(cpuPercent)
```

### Storage Performance
```
class:storage | stats avg(throughput) by storage, operation
class:storage AND operation:write | stats sum(bytes) by bucket
```

### Search Performance
```
class:search | stats avg(searchTime) by index
class:search AND cacheHit:false | stats count by index
```

---

## Integration with Demo

### Update start-demo.sh

The `start-demo.sh` script runs the generator in continuous mode. To use historical mode, run:

```bash
# Stop current generator
pkill -f test-log-generator

# Start with historical data
java -jar test-log-generator/target/test-log-generator-1.0-SNAPSHOT-jar-with-dependencies.jar \
  --days-back 7 \
  --max-logs 20000 &
```

### One-Time Historical Generation

Use the helper script:
```bash
./generate-historical-logs.sh
```

This generates 10,000 logs spanning 7 days, perfect for testing time-based features.

---

## Log File Output

All logs are written to the `logs/` directory:

- **application.log** - All application logs (11 classes)
- **access.log** - HTTP access logs
- **error.log** - Error bursts

---

## Timestamp Behavior

### Current Time Mode (Default)
Each log entry uses the current system time when generated.

### Historical Mode
- Starts at the specified date (`--start-date` or `--days-back`)
- Each log advances time by 1-10 seconds randomly
- Provides realistic time distribution over the specified period
- Useful for testing:
  - Time range filters
  - Timechart queries
  - Date-based aggregations
  - Historical trend analysis

---

## Performance

**Generation Rates:**
- **No delay**: ~10,000-50,000 logs/second (limited by I/O)
- **With delay**: Controlled rate (e.g., --delay 100 = 10 logs/sec)

**Recommendations:**
- For continuous monitoring: Use default (no delay) or --delay 100-500
- For load testing: Use --max-logs without delay
- For historical backfill: Use --days-back with --max-logs
- For demos: Use --delay 500-1000 for visible log flow

---

## Examples

### Example 1: Weekly Summary Data
```bash
java -jar test-log-generator.jar \
  --days-back 7 \
  --max-logs 10000
```

Then search:
```
class:payment | timechart span=1d sum(amount)
```

### Example 2: Slow Real-Time Demo
```bash
java -jar test-log-generator.jar --delay 500
```

Watch logs appear in real-time in the UI.

### Example 3: Month of Container Metrics
```bash
java -jar test-log-generator.jar \
  --days-back 30 \
  --max-logs 50000
```

Then analyze:
```
class:container | timechart span=1d avg(cpuPercent) by region
```

### Example 4: Specific Date Range
```bash
java -jar test-log-generator.jar \
  --start-date 2024-12-01T00:00:00Z \
  --max-logs 20000
```

Generate December 2024 logs for year-end analysis.

---

## Troubleshooting

**Generator stops immediately:**
- Check if `--max-logs` was reached
- Check logs directory permissions

**Logs not appearing in search:**
- Ensure log sources are configured
- Service tailer starts from end of file - wait for new logs or restart service

**Timestamps not historical:**
- Verify `--days-back` or `--start-date` is specified
- Check console output confirms "Using historical timestamps"

**Too many logs:**
- Use `--delay` to slow down
- Use `--max-logs` to limit total
- Clear log files: `rm logs/*.log`

---

## Next Steps

1. Generate historical data: `./generate-historical-logs.sh`
2. Configure log sources in UI
3. Try time-based queries
4. Explore new log classes
5. Build monitoring dashboards

Happy log exploring! 🚀
