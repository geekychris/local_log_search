# Local Log Search - Recent Features Implemented

## Overview
This document describes the pagination, sorting, and facets features that were recently implemented.

## 1. Pagination Support

### Backend (SearchService)
- **SearchRequest** class created with:
  - `page` (default: 0)
  - `pageSize` (default: 50)
- **SearchResponse** enhanced with:
  - `page`, `pageSize`, `totalPages`, `totalHits`
  - `getTotalPages()` method calculates: `(totalHits + pageSize - 1) / pageSize`
  
### REST API
- Updated `/api/search` endpoint to accept:
  - `page`: Zero-based page number
  - `pageSize`: Number of results per page (default: 50)
  
### UI
- Added pagination controls with Previous/Next buttons
- Shows current page and total pages
- Automatically hides when only one page
- Smooth scroll to top on page navigation

**Example:**
```bash
curl -X POST http://localhost:8080/api/search \
  -H "Content-Type: application/json" \
  -d '{"indices": ["app-logs"], "query": "level:ERROR", "page": 0, "pageSize": 50}'
```

## 2. Sorting Support

### Backend
- **SearchRequest** fields:
  - `sortField`: Field name to sort by (or "timestamp"/"score")
  - `sortDescending`: Boolean for sort direction (default: false)
- **SearchService** `sortResults()` method handles:
  - Sort by **timestamp** (chronological)
  - Sort by **score** (relevance)
  - Sort by any **field value** (lexicographic)
  - Both ascending and descending order

### REST API
- Parameters:
  - `sortField`: "timestamp", "score", or any field name (e.g., "level", "user")
  - `sortDescending`: true/false

### UI
- Dropdown to select sort field (Relevance, Timestamp, Level, User, Operation, Status)
- Dropdown to select sort order (Ascending/Descending)
- Auto-triggers new search when changed

**Example:**
```bash
curl -X POST http://localhost:8080/api/search \
  -H "Content-Type: application/json" \
  -d '{
    "indices": ["app-logs"], 
    "query": "level:ERROR", 
    "sortField": "timestamp",
    "sortDescending": false
  }'
```

## 3. Fixed Facets Calculation

### Problem
Facets were being calculated only from the returned page results (e.g., 50 documents), not from all matching documents. This caused:
- Facet counts to be misleading
- Facet totals not matching actual total hits

### Solution
- Modified `SearchService.search()` to fetch up to 10,000 matching documents
- Calculate facets from ALL matching documents (not just the page)
- Use `calculateFacets()` helper method that processes all results
- `mergeFacets()` combines facets across multiple indices
- Only return the requested page of results to the client

### Backend
- Facets now calculated from complete result set (up to 10,000 docs)
- Returned in response as `Map<String, Map<String, Integer>>`
- Each field shows: value → count across all matching documents

### UI
- Displays top 10 values per field
- Shows total count for each field in header
- Clickable to add constraints to query
- Now accurately reflects distribution across all results

**Example Response:**
```json
{
  "results": [...],
  "totalHits": 143,
  "page": 0,
  "pageSize": 50,
  "totalPages": 3,
  "facets": {
    "level": {"ERROR": 143},
    "user": {"admin": 35, "john.doe": 42, ...},
    "operation": {"sendNotification": 20, ...}
  }
}
```

## 4. API Changes

### New SearchRequest Parameters
```java
{
  "indices": ["app-logs", "access-logs"],  // Required
  "query": "level:ERROR",                   // Required
  "page": 0,                                // Optional, default: 0
  "pageSize": 50,                           // Optional, default: 50
  "sortField": "timestamp",                 // Optional, default: null (score)
  "sortDescending": true,                   // Optional, default: false
  "includeFacets": true                     // Optional, default: true
}
```

### SearchResponse Structure
```java
{
  "results": [...],           // Array of SearchResult
  "totalHits": 1632,          // Total matching documents
  "page": 0,                  // Current page (0-based)
  "pageSize": 50,             // Results per page
  "totalPages": 33,           // Total pages
  "facets": {                 // Facets calculated from all results
    "field1": {"value1": count1, ...},
    ...
  }
}
```

## 5. UI Features

### Pagination Controls
- Previous/Next buttons
- Page indicator (e.g., "Page 1 of 33")
- Buttons disable at boundaries
- Auto-hides for single-page results

### Sort Controls
- Field selector dropdown
- Order selector (Asc/Desc)
- Changes trigger new search at page 0
- Default: Sort by timestamp descending

### Facets Display
- Shows top 10 values per field
- Displays total count in header
- Clickable to add constraints
- Only shown when results exist

## 6. Testing

All features tested and verified:
- ✅ Pagination: Multiple pages navigate correctly
- ✅ Sorting: By timestamp, score, and field values
- ✅ Facets: Sum to total hits across all documents
- ✅ UI: Controls work smoothly with responsive updates

### Test Commands
```bash
# Pagination
curl -X POST http://localhost:8080/api/search -H "Content-Type: application/json" \
  -d '{"indices": ["app-logs"], "query": "level:ERROR", "page": 1, "pageSize": 20}'

# Sorting
curl -X POST http://localhost:8080/api/search -H "Content-Type: application/json" \
  -d '{"indices": ["app-logs"], "query": "level:ERROR", "sortField": "user", "sortDescending": false}'

# Facets
curl -X POST http://localhost:8080/api/search -H "Content-Type: application/json" \
  -d '{"indices": ["app-logs"], "query": "level:ERROR", "includeFacets": true}'
```

## 7. Performance Considerations

- Facets calculated from up to 10,000 documents (configurable)
- For larger result sets, consider:
  - Sampling for facets
  - Caching facet calculations
  - Using Lucene's built-in faceting (future enhancement)
- Sorting by field uses in-memory sort (fine for <10k results)
- Pagination is efficient (no need to load all results)

## 8. Future Enhancements (TODO)

### Aggregation Support for Charts
- Add `/api/aggregations` endpoint
- Support operations:
  - `count` by field
  - `sum` of numeric fields
  - `avg` of numeric fields
  - Time-based histograms
- Return data suitable for chart rendering
- UI: Add charts (line, bar, pie) using Chart.js or similar

### Example Aggregation Query:
```json
{
  "indices": ["app-logs"],
  "query": "level:ERROR",
  "aggregations": [
    {
      "type": "count",
      "field": "user",
      "size": 10
    },
    {
      "type": "histogram",
      "field": "timestamp",
      "interval": "1h"
    }
  ]
}
```

## Access

- **Web UI**: http://localhost:8080/ui/index.html
- **REST API**: http://localhost:8080/api/search
- **API Sources**: http://localhost:8080/api/sources

## Architecture

```
┌─────────────┐
│   Browser   │
│  (UI HTML)  │
└─────┬───────┘
      │ HTTP POST /api/search
      ├─ page, pageSize
      ├─ sortField, sortDescending
      └─ includeFacets
      ↓
┌─────────────────────────┐
│  SearchController       │
│  (Spring REST)          │
└─────┬───────────────────┘
      │ SearchRequest
      ↓
┌─────────────────────────┐
│  SearchService          │
│  - search(request)      │
│  - calculateFacets()    │
│  - sortResults()        │
│  - paginate()           │
└─────┬───────────────────┘
      │
      ↓
┌─────────────────────────┐
│  Lucene IndexReader     │
│  (Multiple indices)     │
└─────────────────────────┘
```

## Code Structure

### Core Classes
- `SearchRequest.java` - Request parameters
- `SearchResponse.java` - Response with pagination/facets
- `SearchService.java` - Main search logic
- `SearchController.java` - REST endpoint
- `index.html` - UI with pagination/sorting/facets

### Key Methods
- `SearchService.search(SearchRequest)` - Main entry point
- `SearchService.calculateFacets(List<Document>)` - Facet calculation
- `SearchService.sortResults(List<SearchResult>, ...)` - Sorting
- `SearchService.mergeFacets(Map, Map)` - Multi-index facets
