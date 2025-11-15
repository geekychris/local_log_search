# LocalLogSearch Mac App - Quick Reference

## Adding Log Sources - The Right Way

### ✅ Method 1: Use the Helper Script (Easiest)

```bash
# Run this from Terminal - it handles everything for you
/Applications/LocalLogSearch.app/Contents/Resources/add-log-source.sh

# Or create an alias in your ~/.zshrc:
alias llsearch-add='/Applications/LocalLogSearch.app/Contents/Resources/add-log-source.sh'
```

The script will:
- Let you drag & drop files
- Handle path resolution automatically
- Validate file access
- Add the source via API

### ✅ Method 2: Use Full Paths in Web UI

1. **Find your log file's full path:**
   ```bash
   # Drag file into Terminal after typing 'echo '
   echo [drag file here]
   # Press Enter to see full path
   ```

2. **In the Web UI:**
   - File Path: `/Users/yourname/logs/app.log` (paste the full path)
   - Index Name: `app-logs`
   - Parser Type: `keyvalue`
   - Click "Add Source"

## Common Issues & Solutions

| Problem | Solution |
|---------|----------|
| Browse button doesn't show path | **Expected** - Type full path manually |
| "File does not exist" | Use absolute path: `/Users/...` not `~/...` |
| "File not readable" | `chmod 644 /path/to/file.log` |
| File won't index | Wait 15 seconds for commit, check logs |

## Path Rules

| Type | Example | Works? |
|------|---------|--------|
| Absolute | `/Users/chris/logs/app.log` | ✅ Yes |
| Tilde | `~/logs/app.log` | ❌ No |
| Relative | `./logs/app.log` | ❌ No |
| Variable | `$HOME/logs/app.log` | ❌ No |

## Essential Commands

```bash
# Stop the service
/Applications/LocalLogSearch.app/Contents/Resources/stop-service.sh

# View logs
/Applications/LocalLogSearch.app/Contents/Resources/view-logs.sh

# Add log source (interactive)
/Applications/LocalLogSearch.app/Contents/Resources/add-log-source.sh

# Check if service is running
curl http://localhost:8080/api/sources

# Add source via curl
curl -X POST http://localhost:8080/api/sources \
  -H "Content-Type: application/json" \
  -d '{
    "filePath": "/Users/yourname/logs/app.log",
    "indexName": "app-logs",
    "parserType": "keyvalue",
    "enabled": true
  }'
```

## File Locations

```
~/.local_log_search/
├── indices/              # Lucene indices
├── logs/app.log         # Application logs
└── service.pid          # Running service PID

/Applications/LocalLogSearch.app/Contents/
├── MacOS/LocalLogSearch     # App launcher
├── Java/*.jar               # Application JARs
└── Resources/
    ├── stop-service.sh      # Helper scripts
    ├── view-logs.sh
    ├── add-log-source.sh
    └── MAC_APP_GUIDE.md     # Full documentation
```

## Quick Test

```bash
# 1. Create test log
mkdir -p ~/test-logs
echo "$(date -u +"%Y-%m-%dT%H:%M:%SZ") level=INFO message=HelloWorld" > ~/test-logs/test.log

# 2. Get full path
echo ~/test-logs/test.log
# Copy the output (should be /Users/yourname/test-logs/test.log)

# 3. Add via helper script
/Applications/LocalLogSearch.app/Contents/Resources/add-log-source.sh
# Paste the path when prompted

# 4. Wait 15 seconds, then search
# Open http://localhost:8080, index: test-logs, query: *
```

## Getting Help

- Full Mac guide: `/Applications/LocalLogSearch.app/Contents/Resources/MAC_APP_GUIDE.md`
- Project README: `/Applications/LocalLogSearch.app/Contents/Resources/README.md`
- View app logs: `tail -f ~/.local_log_search/logs/app.log`

## Pro Tips

1. **Set up aliases** in `~/.zshrc`:
   ```bash
   alias llsearch-add='/Applications/LocalLogSearch.app/Contents/Resources/add-log-source.sh'
   alias llsearch-stop='/Applications/LocalLogSearch.app/Contents/Resources/stop-service.sh'
   alias llsearch-logs='tail -f ~/.local_log_search/logs/app.log'
   ```

2. **Use the Load Preview** feature in the UI after typing the full path - it validates the file and shows how it will be parsed

3. **Keep logs in your home directory** for easiest access without permission issues

4. **Use descriptive index names** like `nginx-access`, `app-errors`, `system-logs`
