#!/bin/bash

set -e

echo "🎨 Generating macOS app icon..."
echo ""

# Check if we have the required tools
if ! command -v sips &> /dev/null; then
    echo "❌ Error: sips command not found (should be available on macOS)"
    exit 1
fi

if ! command -v iconutil &> /dev/null; then
    echo "❌ Error: iconutil command not found (should be available on macOS)"
    exit 1
fi

# Check for SVG conversion tool
SVG_CONVERTER=""
if command -v rsvg-convert &> /dev/null; then
    SVG_CONVERTER="rsvg-convert"
    echo "✅ Using rsvg-convert for SVG conversion"
elif command -v qlmanage &> /dev/null; then
    SVG_CONVERTER="qlmanage"
    echo "✅ Using qlmanage for SVG conversion"
else
    echo "⚠️  Warning: No SVG converter found. Attempting with sips..."
    echo "   If this fails, install librsvg: brew install librsvg"
    SVG_CONVERTER="sips"
fi

# Create temporary directory for iconset
ICONSET_DIR="AppIcon.iconset"
rm -rf "$ICONSET_DIR"
mkdir -p "$ICONSET_DIR"

echo ""
echo "📐 Converting SVG to PNG at various sizes..."

# Function to convert SVG to PNG
convert_svg() {
    local size=$1
    local output=$2
    
    if [ "$SVG_CONVERTER" = "rsvg-convert" ]; then
        rsvg-convert -w $size -h $size icon.svg -o "$output"
    elif [ "$SVG_CONVERTER" = "qlmanage" ]; then
        # qlmanage can preview but not easily convert, so we'll use a workaround
        # First convert SVG to large PNG with sips, then resize
        if [ ! -f "icon_temp.png" ]; then
            # Convert SVG to PNG using Quick Look
            qlmanage -t -s 1024 -o . icon.svg > /dev/null 2>&1
            mv icon.svg.png icon_temp.png 2>/dev/null || {
                echo "❌ Failed to convert SVG. Installing librsvg recommended: brew install librsvg"
                exit 1
            }
        fi
        sips -z $size $size icon_temp.png --out "$output" > /dev/null 2>&1
    else
        # Try with sips directly (may not work with SVG)
        sips -z $size $size icon.svg --out "$output" > /dev/null 2>&1 || {
            echo "❌ Failed to convert SVG. Please install librsvg: brew install librsvg"
            exit 1
        }
    fi
    
    echo "  ✓ Generated ${size}x${size}"
}

# Generate all required icon sizes for macOS
# Standard sizes
convert_svg 16 "$ICONSET_DIR/icon_16x16.png"
convert_svg 32 "$ICONSET_DIR/icon_16x16@2x.png"
convert_svg 32 "$ICONSET_DIR/icon_32x32.png"
convert_svg 64 "$ICONSET_DIR/icon_32x32@2x.png"
convert_svg 128 "$ICONSET_DIR/icon_128x128.png"
convert_svg 256 "$ICONSET_DIR/icon_128x128@2x.png"
convert_svg 256 "$ICONSET_DIR/icon_256x256.png"
convert_svg 512 "$ICONSET_DIR/icon_256x256@2x.png"
convert_svg 512 "$ICONSET_DIR/icon_512x512.png"
convert_svg 1024 "$ICONSET_DIR/icon_512x512@2x.png"

# Clean up temporary file if created
rm -f icon_temp.png

echo ""
echo "🔨 Creating .icns file..."
iconutil -c icns "$ICONSET_DIR" -o AppIcon.icns

# Clean up iconset directory
rm -rf "$ICONSET_DIR"

echo ""
echo "✅ Icon generated: AppIcon.icns"
echo ""
echo "📋 Next steps:"
echo "  1. The icon has been created as AppIcon.icns"
echo "  2. Run ./build-mac-app.sh to rebuild the app with the new icon"
echo "  3. The build script will automatically copy AppIcon.icns to the app bundle"
echo ""
