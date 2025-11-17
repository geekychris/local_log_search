# Code Signing Quick Start

## TL;DR

```bash
# Build the app
./build-mac-app.sh

# Sign it (choose option when prompted)
./sign-app.sh
```

## Three Options

### Option 1: Ad-hoc (Quickest - Personal Use Only)

**Best for:** Testing on your own Mac

```bash
./sign-app.sh
# Choose option 1
```

**On other Macs:** Users right-click → Open (first time only)

### Option 2: Self-Signed Certificate (Recommended - Multi-Mac)

**Best for:** Using on multiple personal Macs or sharing with team

**Step 1: Create Certificate (one-time)**

1. Open **Keychain Access**
2. Menu: **Keychain Access** → **Certificate Assistant** → **Create a Certificate**
3. Fill in:
   - **Name:** `LocalLogSearch Developer`
   - **Identity Type:** `Self Signed Root`
   - **Certificate Type:** `Code Signing`
   - Check **"Let me override defaults"**
4. Click **Continue** through all dialogs (accept defaults)
5. Done!

**Step 2: Sign the App**

```bash
./sign-app.sh
# Choose option 2
```

**Step 3: Share Certificate (one-time per Mac)**

Export on signing Mac:
```bash
security export -k ~/Library/Keychains/login.keychain-db \
  -t identities -f pkcs12 -P "mypassword" \
  -o LocalLogSearchCert.p12
```

Import on other Macs:
```bash
security import LocalLogSearchCert.p12 \
  -k ~/Library/Keychains/login.keychain-db -P "mypassword"

sudo security add-trusted-cert -d -r trustRoot \
  -k /Library/Keychains/System.keychain \
  LocalLogSearchCert.cer
```

### Option 3: Apple Developer (Professional Distribution)

**Best for:** Public distribution, Mac App Store

**Requirements:**
- Apple Developer Account ($99/year)
- Xcode installed

**Setup:**
1. Sign up at https://developer.apple.com
2. Open **Xcode** → **Preferences** → **Accounts**
3. Sign in with Apple ID
4. Click **Manage Certificates** → **+** → **Developer ID Application**

**Sign & Notarize:**
```bash
./sign-app.sh
# Script will detect certificate and offer notarization
```

## Verification

Check if app is signed:
```bash
codesign -vvv build/LocalLogSearch.app
```

Check Gatekeeper status:
```bash
spctl -a -vv build/LocalLogSearch.app
```

## Troubleshooting

### "App is damaged"
```bash
xattr -dr com.apple.quarantine /Applications/LocalLogSearch.app
```

### Remove signature
```bash
codesign --remove-signature build/LocalLogSearch.app
```

### List available certificates
```bash
security find-identity -v -p codesigning
```

## Recommendation by Use Case

| Scenario | Method | Why |
|----------|--------|-----|
| Just me, one Mac | Ad-hoc | Fastest, no setup |
| Me, multiple Macs | Self-signed | One-time cert setup |
| Share with team | Self-signed | Share cert file once |
| Public/wide distribution | Apple Developer | No warnings for anyone |

## More Info

- Full guide: [CODE_SIGNING.md](CODE_SIGNING.md)
- Build guide: [README.md](README.md)
