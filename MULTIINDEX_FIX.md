# MultiIndexIterator Sort Order Fix

## Problem
The original `MultiIndexIterator` was chaining multiple sorted iterators sequentially, exhausting one before moving to the next. This violated the sort order when merging results from multiple indices.

### Example of the Problem
If you had:
- **Index 1**: [result(score=10), result(score=5)]
- **Index 2**: [result(score=8), result(score=3)]

The old implementation would return: `[10, 5, 8, 3]` (wrong order!)

The correct merged order should be: `[10, 8, 5, 3]`

## Solution
Implemented a **selection tree** approach that properly merges sorted streams:

1. **PeekableIterator**: Wraps each iterator to allow peeking at the next element without consuming it
   - Only buffers one element per iterator (O(n) space where n = number of iterators)
   - Maintains streaming behavior - no bulk materialization

2. **Selection on each next()**: 
   - Compares the peeked next element from each iterator
   - Selects the best element according to the comparator
   - Consumes only that element and advances only that iterator

3. **Configurable Comparator**:
   - `buildComparator()` creates the appropriate comparator based on sort criteria
   - Supports: score (asc/desc), timestamp (asc/desc), custom fields (asc/desc)
   - Default is score descending

## Changes Made

### MultiIndexIterator.java
- Added `Comparator<SearchResult>` parameter
- Implemented `PeekableIterator` inner class for one-element lookahead
- Changed merging logic from sequential chaining to selection tree
- Maintains O(n) space complexity (only n peeked elements)

### SearchService.java
- Added `buildComparator()` method to create comparators matching Lucene's sort order
- Updated both search methods to pass the appropriate comparator to `MultiIndexIterator`
- Ensures merge order matches the sort criteria (score, timestamp, or custom field)

## Testing
Created `MultiIndexIteratorTest` with 6 test cases:
- ✅ Merge by score descending (default)
- ✅ Merge by timestamp ascending
- ✅ Merge by timestamp descending
- ✅ Handle empty iterators
- ✅ Handle all empty iterators
- ✅ Merge three+ iterators

All tests pass, confirming proper merge behavior.

## Performance
- **Space**: O(n) where n = number of indices (only one peeked element per iterator)
- **Time per element**: O(n) to find best among n iterators
- **Streaming**: True streaming - no bulk materialization required
- **Memory efficient**: Only buffers what's needed for correct ordering
