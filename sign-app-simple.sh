#!/bin/bash

# Simple code signing script - ad-hoc signing (no setup required)

set -e

APP_PATH="${1:-build/LocalLogSearch.app}"
DMG_PATH="${2:-build/LocalLogSearch-1.0.dmg}"

echo "=========================================="
echo "LocalLogSearch - Simple Code Signing"
echo "=========================================="
echo ""

# Check if app exists
if [ ! -d "$APP_PATH" ]; then
    echo "❌ Error: App not found at $APP_PATH"
    echo "Build the app first: ./build-mac-app.sh"
    exit 1
fi

echo "Using ad-hoc signing (no setup required)"
echo ""

# Sign the app
echo "🔐 Signing app bundle..."
codesign --force --deep --sign - "$APP_PATH"
echo "✅ App signed"
echo ""

# Verify
echo "🔍 Verifying signature..."
codesign -vvv "$APP_PATH" 2>&1 | head -3
echo ""

# Sign DMG if it exists
if [ -f "$DMG_PATH" ]; then
    echo "🔐 Signing DMG..."
    codesign --force --sign - "$DMG_PATH"
    echo "✅ DMG signed"
    echo ""
fi

echo "✅ Done!"
echo ""
echo "=========================================="
echo "Installation Notes"
echo "=========================================="
echo ""
echo "On YOUR Mac:"
echo "  - App runs immediately without warnings"
echo ""
echo "On OTHER Macs:"
echo "  - First time: Right-click → Open (not double-click)"
echo "  - Click 'Open' in the security dialog"
echo "  - After that, normal double-click works"
echo ""
echo "Or remove quarantine attribute:"
echo "  xattr -d com.apple.quarantine /Applications/LocalLogSearch.app"
echo ""
echo "For better distribution options, see SIGNING_README.md"
echo ""
