# Log Generator - How To Operate

## Quick Start

### Running the Generator

The generator is located in the `test-log-generator/target/` directory.

**Basic command:**
```bash
java -jar test-log-generator/target/test-log-generator-1.0-SNAPSHOT-jar-with-dependencies.jar
```

This will:
- Generate logs continuously
- Write to `logs/application.log`, `logs/access.log`, and `logs/error.log`
- Run until you press Ctrl+C to stop

---

## Common Usage Scenarios

### 1. Continuous Real-Time Logs (Default)

**What it does:** Generates logs continuously until stopped

**Command:**
```bash
java -jar test-log-generator/target/test-log-generator-1.0-SNAPSHOT-jar-with-dependencies.jar
```

**Stop it:**
```bash
Press Ctrl+C
```

**Use case:** Demo, development, watching live logs

---

### 2. Generate Specific Number of Logs

**What it does:** Generate exactly N logs then stop automatically

**Command:**
```bash
java -jar test-log-generator/target/test-log-generator-1.0-SNAPSHOT-jar-with-dependencies.jar --max-logs 10000
```

**Examples:**
```bash
# Generate 1,000 logs
java -jar test-log-generator/target/test-log-generator-1.0-SNAPSHOT-jar-with-dependencies.jar --max-logs 1000

# Generate 50,000 logs
java -jar test-log-generator/target/test-log-generator-1.0-SNAPSHOT-jar-with-dependencies.jar --max-logs 50000
```

**Use case:** Load testing, creating specific dataset sizes

---

### 3. Slow/Controlled Generation

**What it does:** Add delay between logs to control generation speed

**Command:**
```bash
java -jar test-log-generator/target/test-log-generator-1.0-SNAPSHOT-jar-with-dependencies.jar --delay 1000
```

**Common delays:**
```bash
# 1 log per second (1000ms delay)
--delay 1000

# 10 logs per second (100ms delay)
--delay 100

# 2 logs per second (500ms delay)
--delay 500
```

**Use case:** Demos, watching logs appear gradually, preventing system overload

---

### 4. Generate Historical Logs

**What it does:** Create logs with timestamps from the past

**Using the helper script (easiest):**
```bash
./generate-historical-logs.sh
```
This generates 10,000 logs spanning the last 7 days.

**Manual command:**
```bash
# Last 7 days
java -jar test-log-generator/target/test-log-generator-1.0-SNAPSHOT-jar-with-dependencies.jar \
  --days-back 7 \
  --max-logs 10000

# Last 30 days
java -jar test-log-generator/target/test-log-generator-1.0-SNAPSHOT-jar-with-dependencies.jar \
  --days-back 30 \
  --max-logs 50000
```

**Use case:** Testing time-based filters, timechart queries, historical analysis

---

### 5. Generate from Specific Date

**What it does:** Start logs from an exact date/time

**Command:**
```bash
java -jar test-log-generator/target/test-log-generator-1.0-SNAPSHOT-jar-with-dependencies.jar \
  --start-date 2025-01-01T00:00:00Z \
  --max-logs 5000
```

**Date format:** ISO 8601 - `YYYY-MM-DDTHH:mm:ssZ`

**Examples:**
```bash
# New Year's Day 2025
--start-date 2025-01-01T00:00:00Z

# December 15, 2024 at 3:30 PM
--start-date 2024-12-15T15:30:00Z

# Last month
--start-date 2024-10-01T00:00:00Z
```

**Use case:** Generating specific date ranges, backfilling data

---

## Running in Background

### Start in Background
```bash
cd /Users/chris/code/warp_experiments/local_log_search
nohup java -jar test-log-generator/target/test-log-generator-1.0-SNAPSHOT-jar-with-dependencies.jar > generator.log 2>&1 &
```

### Check if Running
```bash
ps aux | grep test-log-generator
```

### Stop Background Generator
```bash
pkill -f test-log-generator
```

### View Background Logs
```bash
tail -f generator.log
```

---

## Integration with Search Service

### Using the Demo Script (Recommended)

**Start everything:**
```bash
./start-demo.sh
```

This automatically:
- Starts the log generator
- Starts the search service
- Configures log sources

**Stop everything:**
```bash
./stop-demo.sh
```

### Manual Integration

**1. Start generator:**
```bash
java -jar test-log-generator/target/test-log-generator-1.0-SNAPSHOT-jar-with-dependencies.jar &
```

**2. Add log sources via UI:**
- Go to http://localhost:8080/ui/sources.html
- Add `logs/application.log` → index `app-logs`
- Add `logs/access.log` → index `access-logs`
- Add `logs/error.log` → index `error-logs`

**3. Wait a few seconds for logs to be indexed**

**4. Search:**
- Go to http://localhost:8080/ui/index.html
- Try queries like `class:payment` or `level:ERROR`

---

## Output Files

The generator creates files in the `logs/` directory:

```
logs/
├── application.log   # All 11 log classes
├── access.log        # HTTP access logs
└── error.log         # Error bursts
```

### Viewing Generated Logs

```bash
# View last 20 lines
tail -20 logs/application.log

# Follow logs in real-time
tail -f logs/application.log

# View logs with timestamps from a specific date
grep "2025-11-07" logs/application.log

# Count total lines
wc -l logs/application.log
```

---

## Clearing Old Logs

**Remove all logs:**
```bash
rm logs/*.log
```

**Start fresh:**
```bash
rm logs/*.log
java -jar test-log-generator/target/test-log-generator-1.0-SNAPSHOT-jar-with-dependencies.jar --max-logs 10000
```

---

## Common Patterns

### Pattern 1: Quick Demo Data
```bash
# Generate 5,000 logs with historical timestamps
java -jar test-log-generator/target/test-log-generator-1.0-SNAPSHOT-jar-with-dependencies.jar \
  --days-back 3 \
  --max-logs 5000
```

### Pattern 2: Large Dataset
```bash
# Generate 100,000 logs as fast as possible
java -jar test-log-generator/target/test-log-generator-1.0-SNAPSHOT-jar-with-dependencies.jar \
  --max-logs 100000
```

### Pattern 3: Slow Live Demo
```bash
# Generate 1 log every 2 seconds, continuously
java -jar test-log-generator/target/test-log-generator-1.0-SNAPSHOT-jar-with-dependencies.jar \
  --delay 2000
```

### Pattern 4: Month of Historical Data
```bash
# Generate a month of logs (large dataset)
java -jar test-log-generator/target/test-log-generator-1.0-SNAPSHOT-jar-with-dependencies.jar \
  --days-back 30 \
  --max-logs 100000
```

### Pattern 5: Controlled Historical Generation
```bash
# Generate last week's logs slowly (10 per second)
java -jar test-log-generator/target/test-log-generator-1.0-SNAPSHOT-jar-with-dependencies.jar \
  --days-back 7 \
  --max-logs 10000 \
  --delay 100
```

---

## Troubleshooting

### Generator Won't Start

**Problem:** Nothing happens when running the jar

**Solutions:**
```bash
# Check if Java is installed
java -version

# Try with full path
cd /Users/chris/code/warp_experiments/local_log_search
java -jar test-log-generator/target/test-log-generator-1.0-SNAPSHOT-jar-with-dependencies.jar --help
```

### No Logs Created

**Problem:** Generator runs but no files in `logs/` directory

**Solution:**
```bash
# Create logs directory
mkdir -p logs

# Check permissions
ls -la logs/

# Run generator again
java -jar test-log-generator/target/test-log-generator-1.0-SNAPSHOT-jar-with-dependencies.jar --max-logs 100
```

### Generator Already Running

**Problem:** Want to restart but it's already running

**Solution:**
```bash
# Find and kill existing generator
pkill -f test-log-generator

# Wait a moment
sleep 2

# Start new generator
java -jar test-log-generator/target/test-log-generator-1.0-SNAPSHOT-jar-with-dependencies.jar
```

### Logs Not Appearing in Search

**Problem:** Generator running but search shows no results

**Solutions:**

1. **Check if log sources are configured:**
   - Go to http://localhost:8080/ui/sources.html
   - Verify sources exist for application.log, access.log, error.log

2. **Service starts from end of file:**
   - Wait for NEW logs to be generated
   - Or restart service to pick up existing logs:
     ```bash
     ./stop-demo.sh
     ./start-demo.sh
     ```

3. **Generate fresh logs:**
   ```bash
   rm logs/*.log
   ./start-demo.sh
   ```

### Too Many Logs / System Slow

**Problem:** Generator flooding system with logs

**Solutions:**

1. **Add delay:**
   ```bash
   pkill -f test-log-generator
   java -jar test-log-generator/target/test-log-generator-1.0-SNAPSHOT-jar-with-dependencies.jar --delay 100
   ```

2. **Limit total logs:**
   ```bash
   pkill -f test-log-generator
   java -jar test-log-generator/target/test-log-generator-1.0-SNAPSHOT-jar-with-dependencies.jar --max-logs 10000
   ```

3. **Clear old logs:**
   ```bash
   rm logs/*.log
   ```

---

## Command Reference

### All Options

| Option | Example | Description |
|--------|---------|-------------|
| `--max-logs` | `--max-logs 10000` | Stop after N logs |
| `--delay` | `--delay 1000` | Milliseconds between logs |
| `--days-back` | `--days-back 7` | Start N days ago |
| `--start-date` | `--start-date 2025-01-01T00:00:00Z` | Start from specific date |
| `--help` | `--help` | Show help message |

### Combining Options

You can combine multiple options:

```bash
# Slow historical generation
java -jar test-log-generator/target/test-log-generator-1.0-SNAPSHOT-jar-with-dependencies.jar \
  --days-back 7 \
  --max-logs 5000 \
  --delay 10

# Fast specific date range
java -jar test-log-generator/target/test-log-generator-1.0-SNAPSHOT-jar-with-dependencies.jar \
  --start-date 2024-12-01T00:00:00Z \
  --max-logs 50000
```

---

## Helper Scripts

### generate-historical-logs.sh

**What it does:** Generates 10,000 logs spanning last 7 days

**Usage:**
```bash
./generate-historical-logs.sh
```

**When to use:** Quick way to create historical data for time-based testing

### start-demo.sh

**What it does:** Starts generator + search service + configures sources

**Usage:**
```bash
./start-demo.sh
```

**When to use:** Starting the complete demo environment

### stop-demo.sh

**What it does:** Stops generator + search service

**Usage:**
```bash
./stop-demo.sh
```

**When to use:** Shutting down all services

---

## Quick Reference Card

```bash
# Basic usage
java -jar test-log-generator/target/test-log-generator-1.0-SNAPSHOT-jar-with-dependencies.jar

# Limit logs
--max-logs 10000

# Slow down
--delay 1000

# Historical
--days-back 7

# Stop
Ctrl+C  or  pkill -f test-log-generator

# View logs
tail -f logs/application.log

# Clear logs
rm logs/*.log

# Full demo
./start-demo.sh
./stop-demo.sh
```

---

## Next Steps

1. **Generate some test data:**
   ```bash
   ./generate-historical-logs.sh
   ```

2. **Start the search service:**
   ```bash
   ./start-demo.sh
   ```

3. **Try searching:**
   - Go to http://localhost:8080/ui/index.html
   - Search: `class:payment`
   - Try: `level:ERROR | stats count by class`

4. **Explore different log classes:**
   - See `LOG_CLASSES.md` for all available classes
   - See `LOG_GENERATOR_GUIDE.md` for detailed examples

Happy logging! 📝
