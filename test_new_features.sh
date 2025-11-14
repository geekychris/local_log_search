#!/bin/bash

# Test script for Query Sharing and Time Range Selection features

echo "=========================================="
echo "Testing New Features Implementation"
echo "=========================================="
echo ""

# Check if service is running
echo "1. Checking if service is running..."
if curl -s http://localhost:8080/ui/index.html > /dev/null; then
    echo "   ✅ Service is running"
else
    echo "   ❌ Service is not running"
    exit 1
fi
echo ""

# Test 1: Verify Chart.js zoom plugin is loaded
echo "2. Verifying Chart.js zoom plugin..."
if curl -s http://localhost:8080/ui/index.html | grep -q "chartjs-plugin-zoom"; then
    echo "   ✅ Zoom plugin CDN link found"
else
    echo "   ❌ Zoom plugin CDN link not found"
fi
echo ""

# Test 2: Verify Share button exists
echo "3. Verifying Share Query button..."
if curl -s http://localhost:8080/ui/index.html | grep -q "📋 Share Query"; then
    echo "   ✅ Share Query button found"
else
    echo "   ❌ Share Query button not found"
fi
echo ""

# Test 3: Verify Share modal exists
echo "4. Verifying Share Query modal..."
if curl -s http://localhost:8080/ui/index.html | grep -q "shareModal"; then
    echo "   ✅ Share modal found"
else
    echo "   ❌ Share modal not found"
fi
echo ""

# Test 4: Verify generateShareLinks function
echo "5. Verifying generateShareLinks function..."
if curl -s http://localhost:8080/ui/index.html | grep -q "generateShareLinks"; then
    echo "   ✅ generateShareLinks function found"
else
    echo "   ❌ generateShareLinks function not found"
fi
echo ""

# Test 5: Verify parseUrlParameters function
echo "6. Verifying parseUrlParameters function..."
if curl -s http://localhost:8080/ui/index.html | grep -q "parseUrlParameters"; then
    echo "   ✅ parseUrlParameters function found"
else
    echo "   ❌ parseUrlParameters function not found"
fi
echo ""

# Test 6: Verify handleTimeRangeSelection function
echo "7. Verifying handleTimeRangeSelection function..."
if curl -s http://localhost:8080/ui/index.html | grep -q "handleTimeRangeSelection"; then
    echo "   ✅ handleTimeRangeSelection function found"
else
    echo "   ❌ handleTimeRangeSelection function not found"
fi
echo ""

# Test 7: Verify zoom configuration in renderTimeChart
echo "8. Verifying zoom plugin configuration..."
if curl -s http://localhost:8080/ui/index.html | grep -q "plugins.*zoom"; then
    echo "   ✅ Zoom configuration found in chart options"
else
    echo "   ❌ Zoom configuration not found"
fi
echo ""

# Test 8: Test URL with query parameters
echo "9. Testing URL parameter parsing..."
TEST_URL="http://localhost:8080/ui/index.html?q=level:ERROR&indices=app-logs"
if curl -s "$TEST_URL" > /dev/null; then
    echo "   ✅ URL with parameters is accessible"
else
    echo "   ❌ URL with parameters failed"
fi
echo ""

# Test 9: Verify modal CSS classes
echo "10. Verifying modal CSS classes..."
if curl -s http://localhost:8080/ui/index.html | grep -q "share-modal"; then
    echo "   ✅ Share modal CSS classes found"
else
    echo "   ❌ Share modal CSS classes not found"
fi
echo ""

# Test 10: Check for copy button functionality
echo "11. Verifying copy button handlers..."
if curl -s http://localhost:8080/ui/index.html | grep -q "copy-btn"; then
    echo "   ✅ Copy button classes found"
else
    echo "   ❌ Copy button classes not found"
fi
echo ""

echo "=========================================="
echo "Feature Test Summary"
echo "=========================================="
echo ""
echo "All component checks completed!"
echo ""
echo "Manual Testing Checklist:"
echo "  1. Open http://localhost:8080/ui/index.html"
echo "  2. Run a query: level:ERROR | timechart span=1h count"
echo "  3. Verify 'Share Query' button appears"
echo "  4. Click Share button and verify modal opens"
echo "  5. Test URL copy functionality"
echo "  6. Test cURL copy functionality"
echo "  7. Click and drag on timechart to zoom"
echo "  8. Verify date filters update"
echo "  9. Double-click chart to reset zoom"
echo "  10. Test a shared URL with parameters"
echo ""
echo "Documentation:"
echo "  - README.md updated with Query Sharing section"
echo "  - README.md updated with Time Range Selection section"
echo "  - FEATURES_IMPLEMENTATION.md created with full details"
echo ""
