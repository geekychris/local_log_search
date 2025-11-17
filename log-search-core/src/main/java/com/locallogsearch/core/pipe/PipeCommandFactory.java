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

package com.locallogsearch.core.pipe;

import com.locallogsearch.core.pipe.commands.ChartCommand;
import com.locallogsearch.core.pipe.commands.ExportCommand;
import com.locallogsearch.core.pipe.commands.FilterCommand;
import com.locallogsearch.core.pipe.commands.StatsCommand;
import com.locallogsearch.core.pipe.commands.TimeChartCommand;
import com.locallogsearch.core.pipe.commands.TransformCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Factory to create pipe commands from parsed specs
 */
public class PipeCommandFactory {
    
    public static PipeCommand createCommand(PipeQueryParser.PipeCommandSpec spec) {
        String command = spec.getCommand().toLowerCase();
        
        switch (command) {
            case "stats":
                return createStatsCommand(spec);
            case "chart":
                return createChartCommand(spec);
            case "timechart":
                return createTimeChartCommand(spec);
            case "filter":
                return createFilterCommand(spec);
            case "transform":
                return createTransformCommand(spec);
            case "export":
                return createExportCommand(spec);
            default:
                throw new IllegalArgumentException("Unknown pipe command: " + command);
        }
    }
    
    private static StatsCommand createStatsCommand(PipeQueryParser.PipeCommandSpec spec) {
        // Parse: stats count avg(duration) by user operation
        List<String> args = spec.getArgs();
        List<String> aggregations = new ArrayList<>();
        List<String> groupByFields = new ArrayList<>();
        
        boolean parsingBy = false;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("by")) {
                parsingBy = true;
            } else if (parsingBy) {
                groupByFields.add(arg);
            } else {
                // It's an aggregation
                aggregations.add(arg);
            }
        }
        
        // Default to count if no aggregations specified
        if (aggregations.isEmpty()) {
            aggregations.add("count");
        }
        
        return new StatsCommand(aggregations, groupByFields);
    }
    
    private static ChartCommand createChartCommand(PipeQueryParser.PipeCommandSpec spec) {
        // Parse: chart count by user
        // Parse: chart type=bar sum(duration) by operation
        String chartType = spec.getParam("type", "bar");
        
        List<String> args = spec.getArgs();
        List<String> aggregations = new ArrayList<>();
        List<String> groupByFields = new ArrayList<>();
        
        boolean parsingBy = false;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("by")) {
                parsingBy = true;
            } else if (parsingBy) {
                groupByFields.add(arg);
            } else {
                aggregations.add(arg);
            }
        }
        
        if (aggregations.isEmpty()) {
            aggregations.add("count");
        }
        
        return new ChartCommand(chartType, aggregations, groupByFields);
    }
    
    private static TimeChartCommand createTimeChartCommand(PipeQueryParser.PipeCommandSpec spec) {
        // Parse: timechart span=1h count
        // Parse: timechart span=5m count by user
        String span = spec.getParam("span", "1h");
        
        List<String> args = spec.getArgs();
        List<String> aggregations = new ArrayList<>();
        String splitByField = null;
        
        boolean parsingBy = false;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("by")) {
                parsingBy = true;
            } else if (parsingBy) {
                splitByField = arg;
                break; // Only support one split field for now
            } else {
                aggregations.add(arg);
            }
        }
        
        if (aggregations.isEmpty()) {
            aggregations.add("count");
        }
        
        return new TimeChartCommand(span, aggregations, splitByField);
    }
    
    private static FilterCommand createFilterCommand(PipeQueryParser.PipeCommandSpec spec) {
        // Parse: filter count > 10
        // Parse: filter user = "alice"
        // Parse: filter duration >= 100
        List<String> args = spec.getArgs();
        
        if (args.size() < 3) {
            throw new IllegalArgumentException("Filter command requires field, operator, and value: filter <field> <operator> <value>");
        }
        
        String field = args.get(0);
        String operator = args.get(1);
        // Join remaining args as value (in case value contains spaces)
        String value = String.join(" ", args.subList(2, args.size()));
        
        // Remove quotes from value if present
        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        
        return new FilterCommand(field, operator, value);
    }
    
    private static TransformCommand createTransformCommand(PipeQueryParser.PipeCommandSpec spec) {
        // Parse: transform rename user as username
        // Parse: transform extract url regex "host=([^&]+)" as hostname  
        // Parse: transform replace message regex "\\d{3}-\\d{2}-\\d{4}" with "XXX-XX-XXXX"
        // Parse: transform merge user,operation as user_op separator="_"
        // Parse: transform eval duration_ms = duration * 1000
        // Parse: transform remove temp_field
        
        List<String> args = spec.getArgs();
        if (args.isEmpty()) {
            throw new IllegalArgumentException("Transform command requires an operation: rename, extract, replace, merge, eval, or remove");
        }
        
        String operation = args.get(0).toLowerCase();
        
        switch (operation) {
            case "rename":
                // transform rename oldfield as newfield
                if (args.size() < 4 || !args.get(2).equalsIgnoreCase("as")) {
                    throw new IllegalArgumentException("Rename syntax: transform rename <field> as <newname>");
                }
                return TransformCommand.rename(args.get(1), args.get(3));
                
            case "extract":
                // transform extract field regex "pattern" as targetfield
                int regexIdx = -1;
                int asIdx = -1;
                for (int i = 0; i < args.size(); i++) {
                    if (args.get(i).equalsIgnoreCase("regex")) regexIdx = i;
                    if (args.get(i).equalsIgnoreCase("as")) asIdx = i;
                }
                if (args.size() < 6 || regexIdx < 0 || asIdx < 0) {
                    throw new IllegalArgumentException("Extract syntax: transform extract <field> regex \"pattern\" as <newfield>");
                }
                String sourceField = args.get(1);
                String pattern = args.get(regexIdx + 1);
                String targetField = args.get(asIdx + 1);
                return TransformCommand.extract(sourceField, pattern, targetField);
                
            case "replace":
                // transform replace field regex "pattern" with "replacement"
                int regexIdx2 = -1;
                int withIdx = -1;
                for (int i = 0; i < args.size(); i++) {
                    if (args.get(i).equalsIgnoreCase("regex")) regexIdx2 = i;
                    if (args.get(i).equalsIgnoreCase("with")) withIdx = i;
                }
                if (args.size() < 6 || regexIdx2 < 0 || withIdx < 0) {
                    throw new IllegalArgumentException("Replace syntax: transform replace <field> regex \"pattern\" with \"replacement\"");
                }
                String replaceField = args.get(1);
                String replacePattern = args.get(regexIdx2 + 1);
                String replacement = args.get(withIdx + 1);
                return TransformCommand.replace(replaceField, replacePattern, replacement);
                
            case "merge":
                // transform merge field1,field2 as newfield separator="sep"
                int asIdx2 = -1;
                for (int i = 0; i < args.size(); i++) {
                    if (args.get(i).equalsIgnoreCase("as")) asIdx2 = i;
                }
                if (args.size() < 4 || asIdx2 < 0) {
                    throw new IllegalArgumentException("Merge syntax: transform merge <field1,field2,...> as <newfield> [separator=\"sep\"]");
                }
                String fieldsStr = args.get(1);
                List<String> fields = Arrays.asList(fieldsStr.split(","));
                String mergeTarget = args.get(asIdx2 + 1);
                String separator = spec.getParam("separator", " ");
                // Remove quotes from separator if present
                if (separator.startsWith("\"") && separator.endsWith("\"")) {
                    separator = separator.substring(1, separator.length() - 1);
                }
                return TransformCommand.merge(fields, mergeTarget, separator);
                
            case "eval":
                // transform eval newfield = expression
                int equalIdx = -1;
                for (int i = 0; i < args.size(); i++) {
                    if (args.get(i).equals("=")) equalIdx = i;
                }
                if (args.size() < 4 || equalIdx < 0) {
                    throw new IllegalArgumentException("Eval syntax: transform eval <newfield> = <expression>");
                }
                String evalTarget = args.get(1);
                String expression = String.join(" ", args.subList(equalIdx + 1, args.size()));
                return TransformCommand.eval(evalTarget, expression);
                
            case "remove":
                // transform remove field
                if (args.size() < 2) {
                    throw new IllegalArgumentException("Remove syntax: transform remove <field>");
                }
                return TransformCommand.remove(args.get(1));
                
            default:
                throw new IllegalArgumentException("Unknown transform operation: " + operation);
        }
    }
    
    private static ExportCommand createExportCommand(PipeQueryParser.PipeCommandSpec spec) {
        // Parse: export table=mytable fields=user,level sample=1000 append=true
        String tableName = spec.getParam("table", null);
        if (tableName == null || tableName.isEmpty()) {
            throw new IllegalArgumentException("Export command requires 'table' parameter");
        }
        
        // Parse fields (comma-separated)
        String fieldsParam = spec.getParam("fields", null);
        List<String> fields = null;
        if (fieldsParam != null && !fieldsParam.isEmpty()) {
            fields = Arrays.asList(fieldsParam.split(","));
            // Trim whitespace from field names
            fields = fields.stream().map(String::trim).toList();
        }
        
        // Parse sample size
        Integer sampleSize = null;
        String sampleParam = spec.getParam("sample", null);
        if (sampleParam != null && !sampleParam.isEmpty()) {
            try {
                sampleSize = Integer.parseInt(sampleParam);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid sample size: " + sampleParam);
            }
        }
        
        // Parse append flag
        boolean append = Boolean.parseBoolean(spec.getParam("append", "true"));
        
        return new ExportCommand(tableName, fields, sampleSize, append);
    }
}
