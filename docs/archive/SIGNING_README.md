# Signing Your Mac App

## Why Sign?

Without signing, macOS will:
- Require root/admin privileges to install the DMG on other Macs
- Show "unidentified developer" warnings
- Potentially block the app from running (Gatekeeper)

## Quick Start

```bash
# After building with ./build-mac-app.sh
./sign-app.sh
```

The script will:
1. Detect available certificates
2. Guide you through options
3. Sign the app and DMG
4. Verify the signature

## Your Current Setup

You have **Apple Development** certificates:
- `Apple Development: Chris Collins`
- `Apple Development: chris@hitorro.com`

These work for personal use but **are not ideal for distributing DMGs to other Macs**.

## Recommendations

### Option A: Use Ad-hoc Signing (Simplest)

**For:** Personal use, testing

```bash
./sign-app.sh
# Choose option 1 when prompted
```

**Result:** 
- Works on your Mac immediately
- On other Macs: right-click → Open (first time)
- No certificate setup needed

### Option B: Create Self-Signed Certificate (Recommended)

**For:** Multiple Macs, sharing with team

**One-time setup:**
1. Open **Keychain Access** app
2. Menu bar: **Keychain Access** → **Certificate Assistant** → **Create a Certificate**
3. Settings:
   - Name: `LocalLogSearch Developer`
   - Identity Type: `Self Signed Root`
   - Certificate Type: `Code Signing`
   - Check: **Let me override defaults**
4. Click **Continue** through dialogs
5. Done!

**Then sign:**
```bash
./sign-app.sh
# Choose option 2
```

**To use on other Macs** (one-time):
```bash
# Export certificate (on signing Mac)
security export -k ~/Library/Keychains/login.keychain-db \
  -t identities -f pkcs12 -P "password123" \
  -o LocalLogSearchCert.p12

# Copy LocalLogSearchCert.p12 to other Mac, then import:
security import LocalLogSearchCert.p12 \
  -k ~/Library/Keychains/login.keychain-db -P "password123"

# Trust it system-wide
sudo security add-trusted-cert -d -r trustRoot \
  -k /Library/Keychains/System.keychain \
  ~/Desktop/LocalLogSearchCert.cer
```

### Option C: Get Developer ID Certificate (Professional)

**For:** Wide distribution, public apps

**Requirements:**
- Apple Developer Program membership ($99/year)
- Must upgrade from free account

**Steps:**
1. Join Apple Developer Program at https://developer.apple.com/programs/
2. Wait for approval (~24 hours)
3. Open **Xcode** → **Preferences** → **Accounts**
4. Select your Apple ID
5. Click **Manage Certificates**
6. Click **+** → **Developer ID Application**
7. Run `./sign-app.sh` - it will detect the new certificate

**Benefits:**
- Apps run without warnings on any Mac
- Can notarize for full distribution
- Professional appearance

## Quick Commands

```bash
# Check what certificates you have
security find-identity -v -p codesigning

# Sign the app manually (ad-hoc)
codesign --force --deep --sign - build/LocalLogSearch.app

# Verify signature
codesign -vvv build/LocalLogSearch.app

# Check if Gatekeeper will accept it
spctl -a -vv build/LocalLogSearch.app

# Remove quarantine (on downloaded DMG)
xattr -d com.apple.quarantine /Applications/LocalLogSearch.app
```

## Troubleshooting

**"App is damaged and can't be opened"**
```bash
# Remove quarantine attribute
xattr -dr com.apple.quarantine /Applications/LocalLogSearch.app

# Or re-sign
codesign --force --deep --sign - /Applications/LocalLogSearch.app
```

**DMG requires root to install**
- The DMG isn't signed, or
- It's signed but the certificate isn't trusted on target Mac
- Solution: Sign the DMG and app together with `./sign-app.sh`

**"Cannot verify developer"**
- First launch: Right-click → Open (not double-click)
- Or: System Settings → Privacy & Security → Open Anyway

## Summary

| Method | Setup Time | Best For | Distribution |
|--------|-----------|----------|--------------|
| Ad-hoc | 0 min | Personal testing | Right-click to open |
| Self-signed | 5 min | Team/multi-Mac | Share cert once |
| Developer ID | $99/year | Public/professional | No warnings |

**My recommendation for you:** Start with **ad-hoc** for testing, then create a **self-signed certificate** if you need to use it on multiple Macs.

## Full Documentation

- **Quick Guide:** [SIGNING_QUICK_START.md](SIGNING_QUICK_START.md)
- **Complete Reference:** [CODE_SIGNING.md](CODE_SIGNING.md)
- **Script:** `./sign-app.sh` (automated signing)
