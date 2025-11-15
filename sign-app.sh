#!/bin/bash

# Automated code signing script for LocalLogSearch Mac app
# Detects available signing identities and signs the app accordingly

set -e

APP_PATH="${1:-build/LocalLogSearch.app}"
DMG_PATH="${2:-build/LocalLogSearch-1.0.dmg}"
ENTITLEMENTS="codesign.entitlements"

echo "=========================================="
echo "LocalLogSearch Code Signing"
echo "=========================================="
echo ""

# Check if app exists
if [ ! -d "$APP_PATH" ]; then
    echo "❌ Error: App not found at $APP_PATH"
    echo "Build the app first: ./build-mac-app.sh"
    exit 1
fi

echo "Detecting available signing identities..."
echo ""

# Get list of signing identities
DEVELOPER_ID=$(security find-identity -v -p codesigning 2>/dev/null | grep "Developer ID Application" || true)
APPLE_DEV=$(security find-identity -v -p codesigning 2>/dev/null | grep "Apple Development" || true)

if [ -n "$DEVELOPER_ID" ]; then
    IDENTITIES="$DEVELOPER_ID"
    CERT_TYPE="Developer ID Application"
elif [ -n "$APPLE_DEV" ]; then
    IDENTITIES="$APPLE_DEV"
    CERT_TYPE="Apple Development"
else
    IDENTITIES=""
fi

if [ -z "$IDENTITIES" ]; then
    echo "ℹ️  No Apple Developer certificate found."
    echo ""
    echo "Available signing options:"
    echo "  1) Ad-hoc signing (free, works on your machines)"
    echo "  2) Create self-signed certificate (better for multiple machines)"
    echo "  3) Get Apple Developer certificate (\$99/year, best for distribution)"
    echo ""
    read -p "Choose option [1/2/3]: " CHOICE
    
    case $CHOICE in
        1)
            echo ""
            echo "📝 Using ad-hoc signing (free)..."
            SIGNING_IDENTITY="-"
            USE_ENTITLEMENTS=false
            ;;
        2)
            echo ""
            echo "Creating self-signed certificate..."
            echo ""
            echo "Opening Keychain Access for you..."
            
            # Open Keychain Access
            open -a "Keychain Access"
            
            echo ""
            echo "Follow these steps in Keychain Access:"
            echo "  1. Menu bar: Keychain Access → Certificate Assistant → Create a Certificate"
            echo "  2. Name: LocalLogSearch Developer"
            echo "  3. Identity Type: Self Signed Root"
            echo "  4. Certificate Type: Code Signing"
            echo "  5. ✓ Check 'Let me override defaults'"
            echo "  6. Click Continue through all dialogs (accept defaults)"
            echo ""
            read -p "Press Enter when you've created the certificate..."
            
            # Check if certificate exists
            if security find-identity -v -p codesigning | grep -q "LocalLogSearch Developer"; then
                SIGNING_IDENTITY="LocalLogSearch Developer"
                USE_ENTITLEMENTS=true
                echo "✅ Certificate found!"
            else
                echo "❌ Certificate not found. Using ad-hoc signing instead."
                echo "   (You can create the certificate later and re-run this script)"
                SIGNING_IDENTITY="-"
                USE_ENTITLEMENTS=false
            fi
            ;;
        3)
            echo ""
            echo "To get an Apple Developer certificate:"
            echo "  1. Sign up at https://developer.apple.com (\$99/year)"
            echo "  2. Install Xcode"
            echo "  3. Xcode → Preferences → Accounts → Manage Certificates"
            echo "  4. Add Developer ID Application certificate"
            echo "  5. Run this script again"
            echo ""
            exit 0
            ;;
        *)
            echo "Invalid choice. Using ad-hoc signing."
            SIGNING_IDENTITY="-"
            USE_ENTITLEMENTS=false
            ;;
    esac
else
    echo "✅ Found $CERT_TYPE certificate(s):"
    echo "$IDENTITIES"
    echo ""
    
    if [ "$CERT_TYPE" = "Apple Development" ]; then
        echo "⚠️  Note: 'Apple Development' certificates work but are not ideal for distribution."
        echo "   For distributing to other Macs, consider creating a 'Developer ID Application'"
        echo "   certificate or using a self-signed certificate (option 2)."
        echo ""
    fi
    
    # Extract the first certificate identity
    SIGNING_IDENTITY=$(echo "$IDENTITIES" | head -1 | sed 's/.*"\(.*\)".*/\1/')
    USE_ENTITLEMENTS=true
    
    read -p "Use this certificate? [Y/n]: " USE_CERT
    if [[ "$USE_CERT" =~ ^[Nn]$ ]]; then
        echo "Using ad-hoc signing instead."
        SIGNING_IDENTITY="-"
        USE_ENTITLEMENTS=false
    fi
fi

echo ""
echo "Signing configuration:"
echo "  App path: $APP_PATH"
echo "  Identity: $SIGNING_IDENTITY"
echo "  Entitlements: $USE_ENTITLEMENTS"
echo ""

# Sign the app
echo "🔐 Signing app bundle..."

if [ "$USE_ENTITLEMENTS" = true ] && [ -f "$ENTITLEMENTS" ]; then
    codesign --force --deep \
        --sign "$SIGNING_IDENTITY" \
        --options runtime \
        --entitlements "$ENTITLEMENTS" \
        "$APP_PATH"
else
    codesign --force --deep \
        --sign "$SIGNING_IDENTITY" \
        "$APP_PATH"
fi

echo "✅ App signed successfully"
echo ""

# Verify signature
echo "🔍 Verifying signature..."
codesign -vvv "$APP_PATH" 2>&1 | head -3
echo ""

# Check Gatekeeper assessment
echo "🔍 Checking Gatekeeper status..."
if spctl -a -vv "$APP_PATH" 2>&1 | grep -q "accepted"; then
    echo "✅ App will run without warnings"
else
    if [ "$SIGNING_IDENTITY" = "-" ]; then
        echo "⚠️  Ad-hoc signed - users will need to right-click → Open on first launch"
    else
        echo "⚠️  App may require user approval on first launch"
    fi
fi
echo ""

# Sign DMG if it exists
if [ -f "$DMG_PATH" ]; then
    echo "🔐 Signing DMG..."
    codesign --force --sign "$SIGNING_IDENTITY" "$DMG_PATH"
    echo "✅ DMG signed successfully"
    echo ""
fi

# Notarization prompt for Apple Developer certificates
if [ "$SIGNING_IDENTITY" != "-" ] && [ "$SIGNING_IDENTITY" != "LocalLogSearch Developer" ]; then
    echo "=========================================="
    echo "Optional: Notarization"
    echo "=========================================="
    echo ""
    echo "For distribution to other Macs running macOS 10.15+, you should notarize the app."
    echo ""
    read -p "Notarize now? [y/N]: " NOTARIZE
    
    if [[ "$NOTARIZE" =~ ^[Yy]$ ]]; then
        echo ""
        read -p "Apple ID email: " APPLE_ID
        read -p "Team ID: " TEAM_ID
        echo ""
        echo "Creating app-specific password:"
        echo "  1. Go to https://appleid.apple.com"
        echo "  2. Sign in"
        echo "  3. Security section → App-Specific Passwords"
        echo "  4. Generate a new password"
        echo ""
        read -s -p "App-specific password: " APP_PASSWORD
        echo ""
        echo ""
        
        # Create ZIP for notarization
        echo "📦 Creating ZIP for notarization..."
        ditto -c -k --keepParent "$APP_PATH" "LocalLogSearch.zip"
        
        # Submit for notarization
        echo "📤 Submitting for notarization (this may take a few minutes)..."
        xcrun notarytool submit LocalLogSearch.zip \
            --apple-id "$APPLE_ID" \
            --team-id "$TEAM_ID" \
            --password "$APP_PASSWORD" \
            --wait
        
        if [ $? -eq 0 ]; then
            echo "✅ Notarization successful"
            
            # Staple the ticket
            echo "📎 Stapling notarization ticket..."
            xcrun stapler staple "$APP_PATH"
            
            if [ -f "$DMG_PATH" ]; then
                # Recreate DMG with notarized app
                echo "📦 Recreating DMG with notarized app..."
                rm -f "$DMG_PATH"
                hdiutil create -volname "LocalLogSearch" \
                    -srcfolder "$APP_PATH" \
                    -ov -format UDZO \
                    "$DMG_PATH"
                
                # Sign the new DMG
                codesign --force --sign "$SIGNING_IDENTITY" "$DMG_PATH"
                echo "✅ Notarized DMG created"
            fi
        else
            echo "❌ Notarization failed. Check the logs for details."
        fi
        
        # Cleanup
        rm -f LocalLogSearch.zip
    fi
fi

echo ""
echo "=========================================="
echo "✅ Code Signing Complete"
echo "=========================================="
echo ""

if [ "$SIGNING_IDENTITY" = "-" ]; then
    echo "⚠️  Ad-hoc Signing Instructions:"
    echo ""
    echo "On other Macs, users will need to:"
    echo "  1. Right-click LocalLogSearch.app → Open"
    echo "  2. Click 'Open' in the security dialog"
    echo "  3. After first launch, normal double-click will work"
    echo ""
    echo "Or remove quarantine attribute:"
    echo "  xattr -d com.apple.quarantine /Applications/LocalLogSearch.app"
    echo ""
elif [ "$SIGNING_IDENTITY" = "LocalLogSearch Developer" ]; then
    echo "🔐 Self-Signed Certificate Instructions:"
    echo ""
    echo "To use on other Macs, export and import the certificate:"
    echo ""
    echo "On this Mac:"
    echo "  security export -k ~/Library/Keychains/login.keychain-db \\"
    echo "    -t identities -f pkcs12 -P 'password' \\"
    echo "    -o LocalLogSearchCert.p12"
    echo ""
    echo "On target Mac:"
    echo "  security import LocalLogSearchCert.p12 \\"
    echo "    -k ~/Library/Keychains/login.keychain-db -P 'password'"
    echo "  sudo security add-trusted-cert -d -r trustRoot \\"
    echo "    -k /Library/Keychains/System.keychain \\"
    echo "    LocalLogSearchCert.cer"
    echo ""
else
    echo "✅ Signed with Apple Developer certificate"
    echo ""
    if spctl -a -vv "$APP_PATH" 2>&1 | grep -q "accepted"; then
        echo "✅ App can be distributed and will run on other Macs without warnings"
    else
        echo "⚠️  Consider notarizing for distribution to macOS 10.15+"
    fi
    echo ""
fi

echo "For more information, see CODE_SIGNING.md"
echo ""
