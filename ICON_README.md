# App Icon

This directory contains the LocalLogSearch app icon featuring a magnifying glass design.

## Files

- `icon.svg` - Source SVG icon (1024x1024, scalable)
- `AppIcon.icns` - macOS icon bundle with all required sizes
- `generate-icon.sh` - Script to regenerate the icon from SVG

## Icon Design

The icon features:
- Large magnifying glass on a blue gradient background
- White magnifying glass with glass reflection effects
- Rounded square background matching macOS style
- Professional drop shadow

## Regenerating the Icon

If you want to modify the icon:

1. Edit `icon.svg` with any SVG editor (or text editor)
2. Run the generation script:
   ```bash
   ./generate-icon.sh
   ```
3. Rebuild the app:
   ```bash
   ./build-mac-app.sh
   ```

## Requirements

The icon generation script requires:
- `sips` (built-in on macOS)
- `iconutil` (built-in on macOS)
- `rsvg-convert` (recommended, install with: `brew install librsvg`)
  - Alternative: will attempt to use `qlmanage` or `sips` directly

## Icon Sizes

The `.icns` file includes all standard macOS icon sizes:
- 16x16, 32x32, 64x64, 128x128, 256x256, 512x512, 1024x1024
- Plus @2x retina versions

## Usage

The icon is automatically copied to the app bundle when you run `./build-mac-app.sh`.
The build script looks for `AppIcon.icns` in the project root and copies it to:
```
build/LocalLogSearch.app/Contents/Resources/AppIcon.icns
```
