LocalLogSearch - Mac Application
================================

INSTALLATION:
1. Copy LocalLogSearch.app to your Applications folder
2. Double-click to launch
3. The service will start and your browser will open automatically

REQUIREMENTS:
- Java 21 or later (OpenJDK or Amazon Corretto)
- macOS 10.15 or later

USAGE:
- Double-click LocalLogSearch.app to start the service
- The web UI will open at http://localhost:8080
- Close the browser - the service keeps running in the background
- To stop the service, run: ~/.local_log_search/stop-service.sh

IMPORTANT - ADDING LOG FILES:
When adding log sources in the web UI, you MUST use full absolute paths:
  ✅ Correct: /Users/yourname/logs/application.log
  ❌ Wrong: ~/logs/application.log
  ❌ Wrong: logs/application.log

The Browse button shows file preview but cannot get full paths due to 
browser security. Type the full path manually or use Terminal to find it:
  echo /path/to/your/logfile.log

See MAC_APP_GUIDE.md for detailed file access instructions.

DATA LOCATIONS:
- Indices: ~/.local_log_search/indices/
- Logs: ~/.local_log_search/logs/app.log
- PID file: ~/.local_log_search/service.pid

HELPER SCRIPTS:
The following scripts are included in the app bundle:
- Contents/Resources/stop-service.sh - Stop the service
- Contents/Resources/view-logs.sh - View application logs  
- Contents/Resources/add-log-source.sh - Interactive helper to add log sources

You can create aliases for these scripts:
  alias llsearch-stop='~/Applications/LocalLogSearch.app/Contents/Resources/stop-service.sh'
  alias llsearch-logs='~/Applications/LocalLogSearch.app/Contents/Resources/view-logs.sh'
  alias llsearch-add='~/Applications/LocalLogSearch.app/Contents/Resources/add-log-source.sh'

To easily add log sources from Terminal:
  ~/Applications/LocalLogSearch.app/Contents/Resources/add-log-source.sh
  (This script handles path resolution and API calls for you)

TROUBLESHOOTING:
- Check logs: ~/.local_log_search/logs/app.log
- If port 8080 is in use, stop other services or modify the configuration
- Ensure Java 21+ is installed: java -version
- For file access issues, see MAC_APP_GUIDE.md

For more information, see the project README.md and MAC_APP_GUIDE.md
