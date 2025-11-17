# Transform and Regex Filter Features

## Overview

Enhanced the pipeline system with powerful data transformation and regex filtering capabilities, enabling sophisticated log processing workflows that can extract, clean, normalize, and analyze data in ways that aren't possible with Lucene queries alone.

## New Capabilities

### 1. Regex Support in Filter Command

**Enhanced FilterCommand** with regex pattern matching operators.

**New Operators:**
- `regex` or `match` - Match field value against regex pattern
- `!regex` or `notmatch` - Negative regex match (exclude matches)

**Examples:**
```bash
# Filter messages containing error patterns
* | filter message regex "error|exception|timeout"

# Filter for specific users using regex
* | filter user regex "^(alice|bob|charlie)$"

# Exclude debug/trace messages
* | filter message !regex "debug|trace"

# Filter aggregated results
* | stats count by operation | filter operation regex "^process.*"
```

### 2. Transform Command

**New TransformCommand** for field manipulation with 6 powerful operations.

#### Operations

**RENAME** - Change field names
```bash
* | transform rename user as username
* | stats count by level | transform rename level as severity
```

**EXTRACT** - Extract values using regex capture groups
```bash
# Extract hostname from URL parameter
* | transform extract url regex "host=([^&]+)" as hostname

# Extract status code from message
* | transform extract message regex "status=(\d+)" as status_code

# Extract error code from brackets
* | transform extract message regex "\[ERROR-(\d+)\]" as error_code
```

**REPLACE** - Replace text using regex patterns
```bash
# Redact Social Security Numbers
* | transform replace message regex "\d{3}-\d{2}-\d{4}" with "XXX-XX-XXXX"

# Redact email addresses
* | transform replace message regex "\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b" with "[EMAIL]"

# Mask credit card numbers
* | transform replace message regex "\d{4}-\d{4}-\d{4}-\d{4}" with "****-****-****-****"
```

**MERGE** - Combine multiple fields
```bash
# Create combined user-operation field
* | transform merge user,operation as user_op separator="_"

# Combine host and port
* | transform merge host,port as endpoint separator=":"

# Create full name from parts
* | transform merge first_name,last_name as full_name separator=" "
```

**EVAL** - Calculate new fields
```bash
# Convert milliseconds to seconds
* | transform eval duration_sec = duration / 1000

# Calculate throughput
* | transform eval rate = requests / duration

# Simple arithmetic
* | transform eval total = count * price
```

**REMOVE** - Delete fields
```bash
# Remove temporary field
* | transform remove temp_field

# Clean up sensitive data
* | transform remove password | transform remove api_key
```

## Use Cases

### Use Case 1: Extract and Analyze Hidden Data

Extract structured data from unstructured log messages:

```bash
# Extract HTTP status codes from messages and analyze distribution
* | transform extract message regex "status=(\d+)" as status_code | filter status_code regex "^[45]" | stats count by status_code | chart type=pie count by status_code
```

**Flow:**
1. Extract status codes from message text
2. Filter for 4xx and 5xx errors
3. Count by status code
4. Visualize in pie chart

### Use Case 2: Data Cleaning and Privacy

Remove sensitive information before analysis or export:

```bash
# Redact PII and export cleaned logs
* | transform replace message regex "\d{3}-\d{2}-\d{4}" with "[SSN]" | transform replace message regex "\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b" with "[EMAIL]" | transform replace message regex "\d{4}-\d{4}-\d{4}-\d{4}" with "[CC]" | export table=cleaned_logs
```

**Flow:**
1. Mask SSN patterns
2. Mask email addresses
3. Mask credit card numbers
4. Export to database

### Use Case 3: Multi-Host Analysis

Extract and filter by deployment environment:

```bash
# Analyze errors by production environment
* | transform extract url regex "host=([^&]+)" as hostname | filter hostname regex "prod.*" | filter level = "ERROR" | stats count avg(duration) by hostname | filter count > 10
```

**Flow:**
1. Extract hostname from URL parameters
2. Filter for production hosts only
3. Filter for errors
4. Aggregate statistics by hostname
5. Filter for high-error hosts

### Use Case 4: Field Normalization

Standardize field formats before aggregation:

```bash
# Normalize user identifiers and analyze
* | transform extract user regex "^([^@]+)" as username | transform eval username_lower = username | stats count by username_lower | sort -count | head 10
```

**Flow:**
1. Extract username from email
2. Create lowercase version
3. Count by normalized username
4. Sort and limit results

### Use Case 5: Complex Log Parsing

Parse structured data embedded in log messages:

```bash
# Parse and analyze request parameters
* | transform extract message regex "method=(\w+)" as http_method | transform extract message regex "path=([^\s]+)" as url_path | transform extract message regex "duration=(\d+)" as request_duration | filter http_method = "POST" | filter request_duration > 1000 | stats count avg(request_duration) by url_path
```

**Flow:**
1. Extract HTTP method from message
2. Extract URL path from message
3. Extract duration from message
4. Filter for POST requests
5. Filter for slow requests (>1000ms)
6. Aggregate by URL path

## Query Examples in UI

Added comprehensive examples to the Query Examples page:

### Filter with Regex Section
- Basic regex filtering
- User pattern matching
- Multi-stage pipelines with regex

### Transform Section
- Rename operations
- Extract with regex groups
- Replace for data redaction
- Merge fields
- Eval calculations
- Complex transform pipelines

### Tips Section
Updated with:
- All filter operators including regex
- All transform operations with syntax
- Practical usage guidelines

## Technical Implementation

### FilterCommand Enhancements

**File:** `FilterCommand.java`

**Changes:**
- Added `Pattern regexPattern` field
- Compile regex pattern in constructor for efficiency
- Added `regex` and `match` operators
- Added `!regex` and `notmatch` for negative matches
- Case-insensitive operator matching

**Regex Compilation:**
```java
if ("regex".equalsIgnoreCase(operator) || "match".equalsIgnoreCase(operator)) {
    try {
        this.regexPattern = Pattern.compile(value);
    } catch (PatternSyntaxException e) {
        throw new IllegalArgumentException("Invalid regex pattern: " + value, e);
    }
}
```

### TransformCommand Implementation

**File:** `TransformCommand.java`

**Architecture:**
- Enum-based operation types for type safety
- Static factory methods for each operation
- Works on both `LogsResult` and `TableResult`
- Supports regex with `Pattern` compilation
- Simple expression evaluator for basic arithmetic

**Key Features:**
- **Memory Efficient:** Transforms results one at a time
- **Type Safe:** Enum-based operation selection
- **Flexible:** Works on logs and tables
- **Regex Optimized:** Pre-compiles patterns

### PipeCommandFactory Updates

**File:** `PipeCommandFactory.java`

**Added:**
- `createTransformCommand()` method
- Parsing for all 6 transform operations
- Support for complex syntax with `as`, `with`, `separator` keywords
- Quoted value handling

**Parse Examples:**
- `transform rename user as username`
- `transform extract url regex "pattern" as field`
- `transform merge user,op as combined separator="_"`

### SearchService Integration

**File:** `SearchService.java`

**Changes:**
- Added `TransformCommand` routing in `executeChainedCommand()`
- Transform works seamlessly in multi-stage pipelines
- Maintains streaming where possible

## Pipeline Transformations

Updated transformation matrix:

```
LogsResult → [filter] → LogsResult
LogsResult → [transform] → LogsResult
LogsResult → [stats] → TableResult

TableResult → [filter] → TableResult
TableResult → [transform] → TableResult
TableResult → [chart] → ChartResult
```

## Documentation Updates

### Updated Files

1. **`docs/PIPELINE_GUIDE.md`**
   - Added Transform command section (section 3)
   - Enhanced Filter command with regex operators
   - Added 2 new use case examples (5 & 6)
   - Updated all section numbers

2. **`log-search-service/src/main/resources/static/ui/index.html`**
   - Added "Filter" section with 5 examples
   - Added "Transform" section with 6 examples
   - Updated tips section with operators and operations
   - All examples are clickable to try immediately

3. **`TRANSFORM_AND_REGEX_FEATURES.md`** (this file)
   - Comprehensive feature documentation
   - Use cases and examples
   - Technical implementation details

## Example Queries

### Basic Transform Pipeline
```bash
* | transform rename user as username | stats count by username
```

### Regex Extract and Aggregate
```bash
* | transform extract message regex "code=(\d+)" as status_code | stats count by status_code
```

### Data Redaction
```bash
* | transform replace message regex "\d{3}-\d{2}-\d{4}" with "[SSN]"
```

### Multi-Stage Complex Pipeline
```bash
* | transform extract url regex "host=([^&]+)" as hostname | filter hostname regex "prod.*" | transform eval duration_sec = duration / 1000 | filter duration_sec > 1 | stats count avg(duration_sec) by hostname | filter count > 10 | chart type=bar avg(duration_sec) by hostname
```

## Benefits

### 1. **Work Around Lucene Limitations**
- Extract data that wasn't indexed
- Filter on computed values
- Pattern match beyond Lucene's capabilities

### 2. **Data Privacy and Compliance**
- Redact PII before export
- Mask sensitive data in real-time
- Clean logs for analysis

### 3. **Field Normalization**
- Standardize formats
- Combine related fields
- Extract structured data from text

### 4. **Advanced Analysis**
- Multi-step data transformation
- Complex filtering with regex
- Calculate derived metrics

### 5. **Flexible Data Processing**
- Chain multiple transforms
- Mix with stats and filters
- Build sophisticated pipelines

## Performance Considerations

1. **Regex Compilation:** Patterns are compiled once and reused
2. **Streaming:** Transform processes results one at a time when possible
3. **Memory:** Table transforms materialize results (use filters early)
4. **Expression Eval:** Simple evaluator for basic arithmetic only

## Future Enhancements

Potential additions:
- **Advanced Expression Parser:** Support complex mathematical expressions
- **Conditional Transforms:** If-then-else logic
- **Lookup Tables:** Enrich data from external sources
- **String Functions:** upper(), lower(), trim(), substring()
- **Date Functions:** Parse and format timestamps
- **JSON Parsing:** Extract from JSON strings

## Testing

Build successful:
```bash
mvn clean package -DskipTests
```

All components compile and integrate properly.

## Summary

This enhancement adds powerful data transformation and regex filtering capabilities that enable:

✅ **Regex filtering** on any field  
✅ **6 transform operations** (rename, extract, replace, merge, eval, remove)  
✅ **Multi-stage pipelines** with transforms  
✅ **Data extraction** from unstructured text  
✅ **PII redaction** and data cleaning  
✅ **Field normalization** and combination  
✅ **UI examples** for immediate use  
✅ **Comprehensive documentation**  

Users can now build sophisticated data processing pipelines that extract, clean, normalize, and analyze log data in ways far beyond basic Lucene queries!
