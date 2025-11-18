/*
 * MIT License
 *
 * Copyright (c) 2025 Chris Collins
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.locallogsearch.core.pipe.commands;

import com.locallogsearch.core.pipe.PipeCommand;
import com.locallogsearch.core.pipe.PipeResult;
import com.locallogsearch.core.search.SearchResult;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Filter command: filters results based on field conditions
 * Examples:
 *   | filter count > 10
 *   | filter user = "alice"
 *   | filter duration >= 100
 *   | filter status != "ok"
 *   | filter message regex "error|exception"
 *   | filter user regex "^(alice|bob)$"
 */
public class FilterCommand implements PipeCommand {
    
    private final String field;
    private final String operator;
    private final String value;
    private final Pattern regexPattern;
    
    public FilterCommand(String field, String operator, String value) {
        this.field = field;
        this.operator = operator;
        this.value = value;
        
        // Compile regex pattern if operator is regex
        if ("regex".equalsIgnoreCase(operator) || "match".equalsIgnoreCase(operator)) {
            try {
                this.regexPattern = Pattern.compile(value);
            } catch (PatternSyntaxException e) {
                throw new IllegalArgumentException("Invalid regex pattern: " + value, e);
            }
        } else {
            this.regexPattern = null;
        }
    }
    
    @Override
    public PipeResult execute(Iterator<SearchResult> input, int totalHits) {
        // Filter logs directly when used as first pipe command
        List<SearchResult> filtered = new ArrayList<>();
        Predicate<Map<String, String>> predicate = createPredicate();
        
        while (input.hasNext()) {
            SearchResult result = input.next();
            if (predicate.test(result.getFields())) {
                filtered.add(result);
            }
        }
        
        return new PipeResult.LogsResult(filtered);
    }
    
    /**
     * Execute filter on a pipe result (supports both LogsResult and TableResult)
     */
    public PipeResult executeOnResult(PipeResult inputResult) {
        if (inputResult instanceof PipeResult.LogsResult) {
            return filterLogs((PipeResult.LogsResult) inputResult);
        } else if (inputResult instanceof PipeResult.TableResult) {
            return filterTable((PipeResult.TableResult) inputResult);
        } else {
            // Can't filter chart or timechart results
            return inputResult;
        }
    }
    
    private PipeResult filterLogs(PipeResult.LogsResult logsResult) {
        List<SearchResult> filtered = new ArrayList<>();
        Predicate<Map<String, String>> predicate = createPredicate();
        
        for (SearchResult result : logsResult.getResults()) {
            if (predicate.test(result.getFields())) {
                filtered.add(result);
            }
        }
        
        return new PipeResult.LogsResult(filtered);
    }
    
    private PipeResult filterTable(PipeResult.TableResult tableResult) {
        List<Map<String, Object>> filtered = new ArrayList<>();
        
        for (Map<String, Object> row : tableResult.getRows()) {
            if (matchesCondition(row)) {
                filtered.add(row);
            }
        }
        
        return new PipeResult.TableResult(tableResult.getColumns(), filtered, tableResult.getSourceHits());
    }
    
    private Predicate<Map<String, String>> createPredicate() {
        return fields -> {
            if (fields == null) {
                return false;
            }
            String fieldValue = fields.get(field);
            if (fieldValue == null) {
                return false;
            }
            return matchesOperator(fieldValue, operator, value);
        };
    }
    
    private boolean matchesCondition(Map<String, Object> row) {
        Object fieldValue = row.get(field);
        if (fieldValue == null) {
            return false;
        }
        
        return matchesOperator(fieldValue.toString(), operator, value);
    }
    
    private boolean matchesOperator(String fieldValue, String operator, String expectedValue) {
        switch (operator.toLowerCase()) {
            case "=":
            case "==":
                return fieldValue.equals(expectedValue);
            case "!=":
                return !fieldValue.equals(expectedValue);
            case ">":
                return compareNumeric(fieldValue, expectedValue) > 0;
            case ">=":
                return compareNumeric(fieldValue, expectedValue) >= 0;
            case "<":
                return compareNumeric(fieldValue, expectedValue) < 0;
            case "<=":
                return compareNumeric(fieldValue, expectedValue) <= 0;
            case "contains":
                return fieldValue.contains(expectedValue);
            case "startswith":
                return fieldValue.startsWith(expectedValue);
            case "endswith":
                return fieldValue.endsWith(expectedValue);
            case "regex":
            case "match":
                return regexPattern != null && regexPattern.matcher(fieldValue).find();
            case "!regex":
            case "notmatch":
                return regexPattern == null || !regexPattern.matcher(fieldValue).find();
            default:
                return false;
        }
    }
    
    private int compareNumeric(String value1, String value2) {
        try {
            double num1 = Double.parseDouble(value1);
            double num2 = Double.parseDouble(value2);
            return Double.compare(num1, num2);
        } catch (NumberFormatException e) {
            // Fall back to string comparison
            return value1.compareTo(value2);
        }
    }
    
    @Override
    public String getName() {
        return "filter";
    }
    
    @Override
    public String getAIDocumentation() {
        return """
## Filter Command
Filters log entries or aggregated results based on field conditions. Use this to narrow down results after the initial search or after aggregations.

Syntax: `| filter <field> <operator> <value>`

Comparison operators:
- `| filter amount > 100` - Keep only entries where amount is greater than 100
- `| filter count >= 50` - Keep rows where count is 50 or more
- `| filter duration < 1000` - Filter to durations under 1000ms
- `| filter status = "success"` or `status == "success"` - Exact match
- `| filter level != "DEBUG"` - Exclude debug entries

String operators:
- `| filter message contains "timeout"` - Message field contains the word "timeout"
- `| filter user startswith "admin"` - User field starts with "admin"
- `| filter endpoint endswith "/api"` - Endpoint ends with "/api"

Regex operators:
- `| filter message regex "error|exception|failure"` - Match any of these patterns
- `| filter user regex "^(alice|bob|charlie)$"` - Match specific users
- `| filter hostname !regex "dev.*"` - Exclude hostnames starting with "dev"

Common use cases:
- After search: `error | filter amount > 200` - Find expensive errors
- After stats: `* | stats count by user | filter count > 100` - Find heavy users
- Multi-stage: `* | filter duration > 1000 | stats avg(duration) by service` - Analyze slow requests""";
    }
}
