# LocalLogSearch Mac Application Guide

## Overview

LocalLogSearch is packaged as a native Mac application that runs a local web service for log indexing and search.

## Important: File Access on macOS

### Browser Security Restrictions

When using LocalLogSearch through your browser, **browsers cannot access full file paths** due to security restrictions. This is a browser limitation, not an app limitation.

### How to Add Log Sources

**Method 1: Type Full Path Manually (Recommended)**

1. Open LocalLogSearch.app
2. Navigate to "Log Sources" 
3. In the "File Path" field, **type the complete absolute path**:
   ```
   /Users/yourname/logs/application.log
   ```
4. Click "Add Source"

**Method 2: Find Your Log File Path First**

Before adding the source in the UI, find the full path:

```bash
# In Terminal, navigate to your log directory
cd ~/logs

# Get the full path
pwd
# Output: /Users/yourname/logs

# List files
ls -la
# Your log file is here
```

Then copy the full path: `/Users/yourname/logs/application.log`

**Method 3: Drag and Drop to Terminal**

1. Open Terminal
2. Type `echo ` (with a space after)
3. Drag your log file from Finder into Terminal
4. Press Enter - the full path will be displayed
5. Copy this path and paste it into LocalLogSearch

### Troubleshooting File Access

#### "File does not exist" Error

**Problem:** You entered a relative path or incorrect path.

**Solution:** Use the absolute path starting with `/`:
- ✅ Correct: `/Users/chris/logs/app.log`
- ❌ Wrong: `~/logs/app.log`
- ❌ Wrong: `logs/app.log`
- ❌ Wrong: `app.log`

#### "File is not readable" Error

**Problem:** The app doesn't have permission to read the file.

**Solution 1 - Grant Permissions:**
```bash
chmod 644 /path/to/your/logfile.log
```

**Solution 2 - Check File Ownership:**
```bash
ls -l /path/to/your/logfile.log
# If you don't own the file, you may need sudo or different permissions
```

**Solution 3 - macOS Full Disk Access (for system logs):**

If you're trying to access system logs or files in protected directories:

1. Open **System Settings** → **Privacy & Security** → **Full Disk Access**
2. Click the **+** button
3. Navigate to `/Applications/LocalLogSearch.app`
4. Add it to the list
5. Restart LocalLogSearch

#### Browse Button Doesn't Show Full Path

This is **expected behavior** - browsers don't allow web pages to see full file paths for security reasons.

**What to do:**
1. Use the "Browse" button to preview the file content
2. The app will show you a preview and the filename
3. **Manually type or paste the full path** in the "File Path" field
4. Then click "Add Source"

## File Access Permissions by Location

| Directory | Access | Notes |
|-----------|--------|-------|
| `/Users/yourname/Documents` | ✅ Direct | User documents |
| `/Users/yourname/logs` | ✅ Direct | User directories |
| `/var/log` | ⚠️ Requires sudo | System logs |
| `/Library/Logs` | ⚠️ May need FDA* | System logs |
| `/tmp` | ✅ Direct | Temporary files |

*FDA = Full Disk Access permission

## Recommended Log File Locations

For easiest access, store your log files in:

1. **Your home directory:**
   ```
   /Users/yourname/logs/
   ```

2. **Application-specific locations:**
   ```
   /Users/yourname/Library/Application Support/YourApp/logs/
   ```

3. **Project directories:**
   ```
   /Users/yourname/code/myproject/logs/
   ```

## Load Preview Feature

The "Load Preview" button:
- ✅ Works when you type the full path manually
- ✅ Reads the file from disk via the backend
- ✅ Shows you how your parser will handle the logs
- ❌ Doesn't work with relative paths or `~/` shortcuts

**Usage:**
1. Enter full path: `/Users/chris/logs/app.log`
2. Click "Load Preview"
3. Select sample lines to test
4. Adjust your parser configuration
5. Click "Add Source"

## Using with Test Log Generator

The test log generator creates logs in the project directory:

```bash
# Start the generator
cd /Users/chris/code/warp_experiments/local_log_search/test-log-generator
java -jar target/test-log-generator-1.0-SNAPSHOT.jar
```

The logs are created in:
```
/Users/chris/code/warp_experiments/local_log_search/test-log-generator/logs/
```

Add them using these paths:
- `/Users/chris/code/warp_experiments/local_log_search/test-log-generator/logs/application.log`
- `/Users/chris/code/warp_experiments/local_log_search/test-log-generator/logs/access.log`
- `/Users/chris/code/warp_experiments/local_log_search/test-log-generator/logs/error.log`

## Common File Path Patterns

### Expanding ~ (tilde)

The `~` shortcut doesn't work in the UI. Expand it:
- ❌ `~/logs/app.log`
- ✅ `/Users/yourname/logs/app.log`

Find your home directory:
```bash
echo $HOME
```

### Environment Variables

Environment variables don't work in the UI:
- ❌ `$HOME/logs/app.log`
- ❌ `${PWD}/logs/app.log`
- ✅ `/Users/yourname/logs/app.log`

### Relative Paths

Relative paths don't work:
- ❌ `./logs/app.log`
- ❌ `../logs/app.log`
- ✅ `/Users/yourname/project/logs/app.log`

## Quick Start Example

1. **Create a test log:**
   ```bash
   mkdir -p ~/my-logs
   echo "$(date -u +"%Y-%m-%dT%H:%M:%SZ") level=INFO message=Test" > ~/my-logs/test.log
   ```

2. **Find the full path:**
   ```bash
   ls -la ~/my-logs/test.log
   # Note the full path shown
   ```

3. **In LocalLogSearch UI:**
   - File Path: `/Users/yourname/my-logs/test.log` (use your actual username)
   - Index Name: `test-logs`
   - Parser Type: `keyvalue`
   - Click "Add Source"

4. **Wait 15 seconds** (for commit interval)

5. **Search:**
   - Navigate to Search
   - Index: `test-logs`
   - Query: `*`
   - Click Search

## Application Data Locations

LocalLogSearch stores its data in:

```
~/.local_log_search/
├── indices/           # Lucene indices
├── logs/             # Application logs
└── service.pid       # Process ID file
```

## Helper Scripts

Stop the service:
```bash
/Applications/LocalLogSearch.app/Contents/Resources/stop-service.sh
```

View application logs:
```bash
/Applications/LocalLogSearch.app/Contents/Resources/view-logs.sh
```

## Support

For more information, see:
- [README.md](README.md) - Full feature documentation
- [QUICKSTART.md](QUICKSTART.md) - Quick start guide
- [API.md](API.md) - REST API documentation
