# Code Signing LocalLogSearch for macOS

## Overview

macOS requires apps to be signed to run without warnings. There are two approaches:

1. **Apple Developer Signing** - Official, costs $99/year, required for distribution
2. **Ad-hoc Signing** - Free, self-signed, works on your own machines

## Option 1: Apple Developer Signing (Recommended for Distribution)

### Prerequisites

1. **Apple Developer Account** ($99/year)
   - Sign up at https://developer.apple.com

2. **Developer Certificate**
   - Open **Xcode** → **Preferences** → **Accounts**
   - Sign in with your Apple ID
   - Click **Manage Certificates**
   - Click **+** → **Developer ID Application**

### Find Your Certificate

```bash
# List available signing identities
security find-identity -v -p codesigning

# Look for something like:
# 1) ABC123... "Developer ID Application: Your Name (TEAM_ID)"
```

### Sign the App

```bash
# Set your identity
SIGNING_IDENTITY="Developer ID Application: Your Name (TEAM_ID)"

# Sign the app
codesign --force --deep --sign "$SIGNING_IDENTITY" \
  --options runtime \
  --entitlements codesign.entitlements \
  build/LocalLogSearch.app

# Verify
codesign -vvv --deep --strict build/LocalLogSearch.app
spctl -a -vv build/LocalLogSearch.app
```

### Notarize (Required for macOS 10.15+)

```bash
# Create a ZIP for notarization
ditto -c -k --keepParent build/LocalLogSearch.app LocalLogSearch.zip

# Submit for notarization (requires app-specific password)
xcrun notarytool submit LocalLogSearch.zip \
  --apple-id "your@email.com" \
  --team-id "TEAM_ID" \
  --password "app-specific-password" \
  --wait

# Staple the notarization ticket
xcrun stapler staple build/LocalLogSearch.app
```

### Sign the DMG

```bash
codesign --force --sign "$SIGNING_IDENTITY" build/LocalLogSearch-1.0.dmg

# Verify
codesign -vvv build/LocalLogSearch-1.0.dmg
```

## Option 2: Ad-hoc Signing (Free, Personal Use)

Ad-hoc signing allows the app to run on your machines without root privileges, but shows warnings on other machines unless you disable Gatekeeper.

### Sign with Ad-hoc Identity

```bash
# Sign the app with ad-hoc signature (no certificate needed)
codesign --force --deep --sign - build/LocalLogSearch.app

# Verify
codesign -vvv build/LocalLogSearch.app
```

### On Other Machines

Users will need to:

**Option A: Right-click to open (first time only)**
1. Right-click the app → **Open**
2. Click **Open** in the security dialog
3. After this, double-click works normally

**Option B: Remove quarantine attribute**
```bash
xattr -d com.apple.quarantine /Applications/LocalLogSearch.app
```

**Option C: Add to Gatekeeper (local machine only)**
```bash
sudo spctl --add /Applications/LocalLogSearch.app
sudo spctl --enable
```

## Option 3: Self-Signed Certificate (Better than Ad-hoc)

Create your own certificate that you can share with multiple machines.

### Create Self-Signed Certificate

1. Open **Keychain Access**
2. **Keychain Access** menu → **Certificate Assistant** → **Create a Certificate**
3. Settings:
   - Name: `LocalLogSearch Developer`
   - Identity Type: `Self Signed Root`
   - Certificate Type: `Code Signing`
   - ✅ Let me override defaults
4. Click **Continue** through dialogs, accepting defaults
5. Make sure it's in **System** keychain for system-wide trust

### Trust the Certificate

```bash
# Trust the certificate system-wide
sudo security add-trusted-cert -d -r trustRoot \
  -k /Library/Keychains/System.keychain \
  ~/Library/Keychains/login.keychain-db
```

### Sign with Self-Signed Certificate

```bash
# Sign the app
codesign --force --deep --sign "LocalLogSearch Developer" \
  build/LocalLogSearch.app

# Verify
codesign -vvv build/LocalLogSearch.app
```

### Export and Import Certificate on Other Machines

**On signing machine:**
```bash
# Export certificate and private key
security export -k ~/Library/Keychains/login.keychain-db \
  -t identities -f pkcs12 \
  -P "your-password" \
  -o LocalLogSearchCert.p12
```

**On target machine:**
```bash
# Import certificate
security import LocalLogSearchCert.p12 \
  -k ~/Library/Keychains/login.keychain-db \
  -P "your-password"

# Trust it
sudo security add-trusted-cert -d -r trustRoot \
  -k /Library/Keychains/System.keychain \
  LocalLogSearchCert.cer
```

## Entitlements File

Create `codesign.entitlements`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>com.apple.security.cs.allow-jit</key>
    <true/>
    <key>com.apple.security.cs.allow-unsigned-executable-memory</key>
    <true/>
    <key>com.apple.security.cs.disable-library-validation</key>
    <true/>
    <key>com.apple.security.cs.allow-dyld-environment-variables</key>
    <true/>
</dict>
</plist>
```

These entitlements are needed for Java runtime.

## Automated Signing Script

See `sign-app.sh` for automated signing based on available certificates.

## Troubleshooting

### "App is damaged and can't be opened"

```bash
# Remove quarantine attribute
xattr -dr com.apple.quarantine /Applications/LocalLogSearch.app

# Or re-sign
codesign --force --deep --sign - /Applications/LocalLogSearch.app
```

### "Cannot verify developer"

- Right-click → Open (first time)
- Or disable Gatekeeper temporarily:
  ```bash
  sudo spctl --master-disable  # Disable
  # Install app
  sudo spctl --master-enable   # Re-enable
  ```

### Check signature status

```bash
codesign -dvvv build/LocalLogSearch.app
spctl -a -vv build/LocalLogSearch.app
```

### Remove signature

```bash
codesign --remove-signature build/LocalLogSearch.app
```

## Recommendations

| Use Case | Recommended Method |
|----------|-------------------|
| Personal use only | Ad-hoc signing |
| Multiple personal Macs | Self-signed certificate |
| Team/Company use | Self-signed certificate |
| Public distribution | Apple Developer signing + notarization |
| Mac App Store | Apple Developer + App Store requirements |

## Distribution Notes

When distributing the DMG:

1. **With Apple Developer Certificate**: Users can install normally
2. **With Self-signed Certificate**: Include certificate and instructions
3. **With Ad-hoc Signing**: Include instructions for right-click → Open
4. **Unsigned**: Users need to disable Gatekeeper or use command-line workarounds

For internal/personal use, **self-signed certificate is the best balance** between security and convenience.
