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

DATA LOCATIONS:
- Indices: ~/.local_log_search/indices/
- Logs: ~/.local_log_search/logs/app.log
- PID file: ~/.local_log_search/service.pid

HELPER SCRIPTS:
The following scripts are included in the app bundle:
- Contents/Resources/stop-service.sh - Stop the service
- Contents/Resources/view-logs.sh - View application logs

You can create aliases for these scripts:
  alias llsearch-stop='~/Applications/LocalLogSearch.app/Contents/Resources/stop-service.sh'
  alias llsearch-logs='~/Applications/LocalLogSearch.app/Contents/Resources/view-logs.sh'

TROUBLESHOOTING:
- Check logs: ~/.local_log_search/logs/app.log
- If port 8080 is in use, stop other services or modify the configuration
- Ensure Java 21+ is installed: java -version

For more information, see the project README.md
