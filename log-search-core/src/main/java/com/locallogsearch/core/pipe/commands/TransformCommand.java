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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Transform command: manipulates fields in logs or table results
 * 
 * Operations:
 * - rename: Rename a field
 * - extract: Extract value using regex groups
 * - replace: Replace text using regex
 * - merge: Combine multiple fields
 * - eval: Compute new field from expression
 * - remove: Remove a field
 * 
 * Examples:
 *   | transform rename user as username
 *   | transform extract url regex "host=([^&]+)" as hostname
 *   | transform replace message regex "\\d{3}-\\d{2}-\\d{4}" with "XXX-XX-XXXX"
 *   | transform merge user,operation as user_op separator="_"
 *   | transform eval duration_ms = duration * 1000
 *   | transform remove temp_field
 */
public class TransformCommand implements PipeCommand {
    
    public enum Operation {
        RENAME,    // rename field as newname
        EXTRACT,   // extract field regex "pattern" as newfield
        REPLACE,   // replace field regex "pattern" with "replacement"
        MERGE,     // merge field1,field2 as newfield separator="sep"
        EVAL,      // eval newfield = expression
        REMOVE     // remove field
    }
    
    private final Operation operation;
    private final String field;
    private final String targetField;
    private final String value;
    private final Pattern regexPattern;
    private final List<String> sourceFields;
    private final String separator;
    
    // For RENAME: field = old name, targetField = new name
    public static TransformCommand rename(String oldName, String newName) {
        return new TransformCommand(Operation.RENAME, oldName, newName, null, null, null, null);
    }
    
    // For EXTRACT: field = source field, value = regex pattern, targetField = new field name
    public static TransformCommand extract(String field, String regexPattern, String targetField) {
        return new TransformCommand(Operation.EXTRACT, field, targetField, regexPattern, null, null, null);
    }
    
    // For REPLACE: field = target field, value = regex pattern, targetField = replacement
    public static TransformCommand replace(String field, String regexPattern, String replacement) {
        return new TransformCommand(Operation.REPLACE, field, replacement, regexPattern, null, null, null);
    }
    
    // For MERGE: sourceFields = fields to merge, targetField = new field name, separator = join string
    public static TransformCommand merge(List<String> sourceFields, String targetField, String separator) {
        return new TransformCommand(Operation.MERGE, null, targetField, null, null, sourceFields, separator);
    }
    
    // For EVAL: targetField = new field, value = expression
    public static TransformCommand eval(String targetField, String expression) {
        return new TransformCommand(Operation.EVAL, null, targetField, expression, null, null, null);
    }
    
    // For REMOVE: field = field to remove
    public static TransformCommand remove(String field) {
        return new TransformCommand(Operation.REMOVE, field, null, null, null, null, null);
    }
    
    private TransformCommand(Operation operation, String field, String targetField, String value, 
                            Pattern regexPattern, List<String> sourceFields, String separator) {
        this.operation = operation;
        this.field = field;
        this.targetField = targetField;
        this.value = value;
        this.sourceFields = sourceFields;
        this.separator = separator != null ? separator : "";
        
        // Compile regex pattern if needed
        if ((operation == Operation.EXTRACT || operation == Operation.REPLACE) && value != null) {
            try {
                this.regexPattern = Pattern.compile(value);
            } catch (PatternSyntaxException e) {
                throw new IllegalArgumentException("Invalid regex pattern: " + value, e);
            }
        } else {
            this.regexPattern = regexPattern;
        }
    }
    
    @Override
    public PipeResult execute(Iterator<SearchResult> input, int totalHits) {
        List<SearchResult> transformed = new ArrayList<>();
        
        while (input.hasNext()) {
            SearchResult result = input.next();
            SearchResult transformedResult = transformSearchResult(result);
            transformed.add(transformedResult);
        }
        
        return new PipeResult.LogsResult(transformed);
    }
    
    /**
     * Execute transform on a pipe result (supports both LogsResult and TableResult)
     */
    public PipeResult executeOnResult(PipeResult inputResult) {
        if (inputResult instanceof PipeResult.LogsResult) {
            return transformLogs((PipeResult.LogsResult) inputResult);
        } else if (inputResult instanceof PipeResult.TableResult) {
            return transformTable((PipeResult.TableResult) inputResult);
        } else {
            // Can't transform chart or timechart results
            return inputResult;
        }
    }
    
    private PipeResult transformLogs(PipeResult.LogsResult logsResult) {
        List<SearchResult> transformed = new ArrayList<>();
        
        for (SearchResult result : logsResult.getResults()) {
            SearchResult transformedResult = transformSearchResult(result);
            transformed.add(transformedResult);
        }
        
        return new PipeResult.LogsResult(transformed);
    }
    
    private PipeResult transformTable(PipeResult.TableResult tableResult) {
        List<String> columns = new ArrayList<>(tableResult.getColumns());
        List<Map<String, Object>> transformedRows = new ArrayList<>();
        
        // Update columns based on operation
        if (operation == Operation.RENAME) {
            int idx = columns.indexOf(field);
            if (idx >= 0) {
                columns.set(idx, targetField);
            }
        } else if (operation == Operation.REMOVE) {
            columns.remove(field);
        } else if (operation == Operation.EXTRACT || operation == Operation.MERGE || operation == Operation.EVAL) {
            if (!columns.contains(targetField)) {
                columns.add(targetField);
            }
        }
        
        // Transform each row
        for (Map<String, Object> row : tableResult.getRows()) {
            Map<String, Object> transformedRow = transformTableRow(row);
            transformedRows.add(transformedRow);
        }
        
        return new PipeResult.TableResult(columns, transformedRows, tableResult.getSourceHits());
    }
    
    private SearchResult transformSearchResult(SearchResult result) {
        Map<String, String> fields = new HashMap<>(result.getFields());
        
        switch (operation) {
            case RENAME:
                if (fields.containsKey(field)) {
                    fields.put(targetField, fields.get(field));
                    fields.remove(field);
                }
                break;
                
            case EXTRACT:
                String sourceValue = fields.get(field);
                if (sourceValue != null && regexPattern != null) {
                    Matcher matcher = regexPattern.matcher(sourceValue);
                    if (matcher.find()) {
                        // Use first group if available, otherwise whole match
                        String extracted = matcher.groupCount() > 0 ? matcher.group(1) : matcher.group(0);
                        fields.put(targetField, extracted);
                    }
                }
                break;
                
            case REPLACE:
                String valueToReplace = fields.get(field);
                if (valueToReplace != null && regexPattern != null) {
                    String replaced = regexPattern.matcher(valueToReplace).replaceAll(targetField);
                    fields.put(field, replaced);
                }
                break;
                
            case MERGE:
                if (sourceFields != null && !sourceFields.isEmpty()) {
                    StringBuilder merged = new StringBuilder();
                    for (int i = 0; i < sourceFields.size(); i++) {
                        String val = fields.get(sourceFields.get(i));
                        if (val != null) {
                            if (merged.length() > 0) {
                                merged.append(separator);
                            }
                            merged.append(val);
                        }
                    }
                    fields.put(targetField, merged.toString());
                }
                break;
                
            case EVAL:
                // Simple expression evaluation for basic arithmetic
                String evalResult = evaluateExpression(value, fields);
                if (evalResult != null) {
                    fields.put(targetField, evalResult);
                }
                break;
                
            case REMOVE:
                fields.remove(field);
                break;
        }
        
        // Create new SearchResult with transformed fields
        SearchResult transformed = new SearchResult();
        transformed.setIndexName(result.getIndexName());
        transformed.setScore(result.getScore());
        transformed.setRawText(result.getRawText());
        transformed.setSource(result.getSource());
        transformed.setTimestamp(result.getTimestamp());
        transformed.setFields(fields);
        
        return transformed;
    }
    
    private Map<String, Object> transformTableRow(Map<String, Object> row) {
        Map<String, Object> transformed = new LinkedHashMap<>(row);
        
        switch (operation) {
            case RENAME:
                if (transformed.containsKey(field)) {
                    transformed.put(targetField, transformed.get(field));
                    transformed.remove(field);
                }
                break;
                
            case EXTRACT:
                Object sourceValue = transformed.get(field);
                if (sourceValue != null && regexPattern != null) {
                    Matcher matcher = regexPattern.matcher(sourceValue.toString());
                    if (matcher.find()) {
                        String extracted = matcher.groupCount() > 0 ? matcher.group(1) : matcher.group(0);
                        transformed.put(targetField, extracted);
                    }
                }
                break;
                
            case REPLACE:
                Object valueToReplace = transformed.get(field);
                if (valueToReplace != null && regexPattern != null) {
                    String replaced = regexPattern.matcher(valueToReplace.toString()).replaceAll(targetField);
                    transformed.put(field, replaced);
                }
                break;
                
            case MERGE:
                if (sourceFields != null && !sourceFields.isEmpty()) {
                    StringBuilder merged = new StringBuilder();
                    for (int i = 0; i < sourceFields.size(); i++) {
                        Object val = transformed.get(sourceFields.get(i));
                        if (val != null) {
                            if (merged.length() > 0) {
                                merged.append(separator);
                            }
                            merged.append(val.toString());
                        }
                    }
                    transformed.put(targetField, merged.toString());
                }
                break;
                
            case EVAL:
                // Simple expression evaluation for table rows
                Map<String, String> stringFields = new HashMap<>();
                for (Map.Entry<String, Object> entry : transformed.entrySet()) {
                    stringFields.put(entry.getKey(), entry.getValue().toString());
                }
                String evalResult = evaluateExpression(value, stringFields);
                if (evalResult != null) {
                    transformed.put(targetField, evalResult);
                }
                break;
                
            case REMOVE:
                transformed.remove(field);
                break;
        }
        
        return transformed;
    }
    
    /**
     * Simple expression evaluator for basic arithmetic and string operations
     * Supports: field1 + field2, field * 1000, etc.
     */
    private String evaluateExpression(String expression, Map<String, String> fields) {
        if (expression == null) {
            return null;
        }
        
        String result = expression;
        
        // Replace field references with their values
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        
        // Try to evaluate as arithmetic if it looks numeric
        try {
            // Simple arithmetic evaluation (only handles basic cases)
            if (result.matches(".*[+\\-*/].*")) {
                // This is a simplified evaluator - for production, use a proper expression parser
                double value = evaluateArithmetic(result);
                return String.valueOf(value);
            }
        } catch (Exception e) {
            // If evaluation fails, return the string result
        }
        
        return result;
    }
    
    /**
     * Very basic arithmetic evaluator (handles single operator only)
     */
    private double evaluateArithmetic(String expression) {
        expression = expression.trim();
        
        // Handle single operators
        if (expression.contains("+")) {
            String[] parts = expression.split("\\+", 2);
            return Double.parseDouble(parts[0].trim()) + Double.parseDouble(parts[1].trim());
        } else if (expression.contains("-") && !expression.startsWith("-")) {
            String[] parts = expression.split("-", 2);
            return Double.parseDouble(parts[0].trim()) - Double.parseDouble(parts[1].trim());
        } else if (expression.contains("*")) {
            String[] parts = expression.split("\\*", 2);
            return Double.parseDouble(parts[0].trim()) * Double.parseDouble(parts[1].trim());
        } else if (expression.contains("/")) {
            String[] parts = expression.split("/", 2);
            return Double.parseDouble(parts[0].trim()) / Double.parseDouble(parts[1].trim());
        }
        
        return Double.parseDouble(expression);
    }
    
    @Override
    public String getName() {
        return "transform";
    }
}
