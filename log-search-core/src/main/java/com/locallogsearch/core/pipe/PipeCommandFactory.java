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
import com.locallogsearch.core.pipe.commands.StatsCommand;
import com.locallogsearch.core.pipe.commands.TimeChartCommand;

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
