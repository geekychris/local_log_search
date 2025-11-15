#!/bin/bash

set -e

# Configuration
APP_NAME="LocalLogSearch"
APP_VERSION="1.0"
BUNDLE_ID="com.locallogsearch.app"
BUILD_DIR="build"
APP_DIR="${BUILD_DIR}/${APP_NAME}.app"
CONTENTS_DIR="${APP_DIR}/Contents"
MACOS_DIR="${CONTENTS_DIR}/MacOS"
RESOURCES_DIR="${CONTENTS_DIR}/Resources"
JAVA_DIR="${CONTENTS_DIR}/Java"

echo "=========================================="
echo "Building ${APP_NAME} Mac Application"
echo "=========================================="
echo ""

# Step 1: Clean previous build
echo "🧹 Cleaning previous build..."
rm -rf "${BUILD_DIR}"
mkdir -p "${MACOS_DIR}" "${RESOURCES_DIR}" "${JAVA_DIR}"

# Step 2: Build the project with Maven
echo ""
echo "🔨 Building Java project with Maven..."
mvn clean package -DskipTests

# Step 3: Copy JAR files to app bundle
echo ""
echo "📦 Packaging JAR files..."
cp log-search-service/target/log-search-service-1.0-SNAPSHOT.jar "${JAVA_DIR}/log-search-service.jar"
cp test-log-generator/target/test-log-generator-1.0-SNAPSHOT.jar "${JAVA_DIR}/test-log-generator.jar"

# Step 4: Create launcher script
echo ""
echo "📝 Creating launcher script..."
cat > "${MACOS_DIR}/${APP_NAME}" << 'LAUNCHER_EOF'
#!/bin/bash

# Get the directory where this script is located
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
CONTENTS_DIR="$(dirname "$DIR")"
JAVA_DIR="${CONTENTS_DIR}/Java"
RESOURCES_DIR="${CONTENTS_DIR}/Resources"

# Set up logging
LOG_DIR="${HOME}/.local_log_search/logs"
mkdir -p "${LOG_DIR}"
LOG_FILE="${LOG_DIR}/app.log"

# Detect Java
if [ -n "$JAVA_HOME" ]; then
    JAVA_CMD="${JAVA_HOME}/bin/java"
elif command -v java &> /dev/null; then
    JAVA_CMD="java"
else
    osascript -e 'display alert "Java Not Found" message "Java 21 or later is required to run LocalLogSearch. Please install Java and try again." as critical'
    exit 1
fi

# Verify Java version
JAVA_VERSION=$("${JAVA_CMD}" -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    osascript -e 'display alert "Java Version Too Old" message "Java 21 or later is required. Found version '"$JAVA_VERSION"'." as critical'
    exit 1
fi

# Log startup
echo "========================================" >> "${LOG_FILE}"
echo "LocalLogSearch started at $(date)" >> "${LOG_FILE}"
echo "Java: ${JAVA_CMD}" >> "${LOG_FILE}"
echo "========================================" >> "${LOG_FILE}"

# Start the service
cd "${JAVA_DIR}"
"${JAVA_CMD}" -jar log-search-service.jar >> "${LOG_FILE}" 2>&1 &
SERVICE_PID=$!

# Save PID for shutdown
echo $SERVICE_PID > "${HOME}/.local_log_search/service.pid"

# Wait a moment for service to start
sleep 3

# Open browser to the UI
open "http://localhost:8080"

# Keep the process running (needed for .app to stay in Dock)
wait $SERVICE_PID
LAUNCHER_EOF

chmod +x "${MACOS_DIR}/${APP_NAME}"

# Step 5: Create Info.plist
echo ""
echo "📄 Creating Info.plist..."
cat > "${CONTENTS_DIR}/Info.plist" << PLIST_EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleDevelopmentRegion</key>
    <string>English</string>
    <key>CFBundleExecutable</key>
    <string>${APP_NAME}</string>
    <key>CFBundleIconFile</key>
    <string>AppIcon.icns</string>
    <key>CFBundleIdentifier</key>
    <string>${BUNDLE_ID}</string>
    <key>CFBundleInfoDictionaryVersion</key>
    <string>6.0</string>
    <key>CFBundleName</key>
    <string>${APP_NAME}</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleShortVersionString</key>
    <string>${APP_VERSION}</string>
    <key>CFBundleVersion</key>
    <string>${APP_VERSION}</string>
    <key>LSMinimumSystemVersion</key>
    <string>10.15</string>
    <key>NSHighResolutionCapable</key>
    <true/>
    <key>LSUIElement</key>
    <false/>
    <key>NSHumanReadableCopyright</key>
    <string>Copyright © 2025. All rights reserved.</string>
</dict>
</plist>
PLIST_EOF

# Step 6: Create a simple icon (you can replace this with a proper .icns file)
echo ""
echo "🎨 Creating placeholder icon..."
# Create a simple text file as placeholder - users can replace with proper .icns
cat > "${RESOURCES_DIR}/AppIcon.icns" << ICON_EOF
# Replace this file with a proper .icns icon file
# You can create one using Icon Composer or online tools
ICON_EOF

# Step 7: Create helper scripts in Resources
echo ""
echo "🔧 Creating helper scripts..."

# Stop script
cat > "${RESOURCES_DIR}/stop-service.sh" << 'STOP_EOF'
#!/bin/bash
PID_FILE="${HOME}/.local_log_search/service.pid"
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if kill -0 $PID 2>/dev/null; then
        echo "Stopping LocalLogSearch (PID: $PID)..."
        kill $PID
        rm "$PID_FILE"
        echo "Service stopped."
    else
        echo "Service not running (stale PID file removed)."
        rm "$PID_FILE"
    fi
else
    echo "Service is not running."
fi
STOP_EOF
chmod +x "${RESOURCES_DIR}/stop-service.sh"

# View logs script
cat > "${RESOURCES_DIR}/view-logs.sh" << 'LOGS_EOF'
#!/bin/bash
LOG_FILE="${HOME}/.local_log_search/logs/app.log"
if [ -f "$LOG_FILE" ]; then
    tail -f "$LOG_FILE"
else
    echo "No log file found at $LOG_FILE"
fi
LOGS_EOF
chmod +x "${RESOURCES_DIR}/view-logs.sh"

# Step 8: Create README for the app bundle
cat > "${RESOURCES_DIR}/README.txt" << 'README_EOF'
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
README_EOF

# Step 9: Create a DMG installer (optional - requires hdiutil)
echo ""
echo "💿 Creating DMG installer..."
DMG_NAME="${BUILD_DIR}/${APP_NAME}-${APP_VERSION}.dmg"
DMG_TEMP_DIR="${BUILD_DIR}/dmg_temp"

mkdir -p "${DMG_TEMP_DIR}"
cp -R "${APP_DIR}" "${DMG_TEMP_DIR}/"

# Create a symbolic link to Applications folder
ln -s /Applications "${DMG_TEMP_DIR}/Applications"

# Create the DMG
hdiutil create -volname "${APP_NAME}" \
    -srcfolder "${DMG_TEMP_DIR}" \
    -ov -format UDZO \
    "${DMG_NAME}"

# Clean up temp directory
rm -rf "${DMG_TEMP_DIR}"

echo ""
echo "✅ Build complete!"
echo ""
echo "=========================================="
echo "Output files:"
echo "  Application: ${APP_DIR}"
echo "  Installer:   ${DMG_NAME}"
echo "=========================================="
echo ""
echo "Installation Instructions:"
echo "  1. Mount the DMG: open ${DMG_NAME}"
echo "  2. Drag LocalLogSearch.app to Applications folder"
echo "  3. Launch from Applications or Spotlight"
echo ""
echo "Or test directly:"
echo "  open ${APP_DIR}"
echo ""
echo "Helper commands:"
echo "  Stop service: ${RESOURCES_DIR}/stop-service.sh"
echo "  View logs:    ${RESOURCES_DIR}/view-logs.sh"
echo ""
