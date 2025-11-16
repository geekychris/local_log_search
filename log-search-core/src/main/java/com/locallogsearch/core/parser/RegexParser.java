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

package com.locallogsearch.core.parser;

import com.locallogsearch.core.model.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexParser implements LogParser {
    private static final Logger log = LoggerFactory.getLogger(RegexParser.class);
    
    private Pattern pattern;
    private Map<Integer, String> groupToFieldMap;
    private Integer timestampGroup;
    private String timestampFormat;
    
    @Override
    public void configure(Map<String, String> config) {
        if (config == null || !config.containsKey("pattern")) {
            throw new IllegalArgumentException("Regex parser requires 'pattern' configuration");
        }
        
        String patternStr = config.get("pattern");
        this.pattern = Pattern.compile(patternStr);
        
        // Parse field mappings: field.1=fieldname, field.2=otherfieldname
        groupToFieldMap = new java.util.HashMap<>();
        for (Map.Entry<String, String> entry : config.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("field.")) {
                try {
                    int groupNum = Integer.parseInt(key.substring(6));
                    groupToFieldMap.put(groupNum, entry.getValue());
                } catch (NumberFormatException e) {
                    log.warn("Invalid field mapping: {}", key);
                }
            }
        }
        
        // Check for timestamp group
        if (config.containsKey("timestamp.group")) {
            this.timestampGroup = Integer.parseInt(config.get("timestamp.group"));
            this.timestampFormat = config.get("timestamp.format");
        }
    }
    
    @Override
    public void parse(LogEntry entry) {
        String text = entry.getRawText();
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            // Extract mapped fields
            for (Map.Entry<Integer, String> mapping : groupToFieldMap.entrySet()) {
                int groupNum = mapping.getKey();
                String fieldName = mapping.getValue();
                
                if (groupNum <= matcher.groupCount()) {
                    String value = matcher.group(groupNum);
                    if (value != null) {
                        entry.addField(fieldName, value);
                    }
                }
            }
            
            // Extract timestamp if configured
            if (timestampGroup != null && timestampGroup <= matcher.groupCount()) {
                String timestampStr = matcher.group(timestampGroup);
                Instant timestamp = parseTimestamp(timestampStr);
                if (timestamp != null) {
                    entry.setTimestamp(timestamp);
                }
            }
        }
    }
    
    private Instant parseTimestamp(String timestampStr) {
        if (timestampStr == null) {
            return null;
        }
        
        try {
            if (timestampFormat != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timestampFormat);
                return Instant.from(formatter.parse(timestampStr));
            } else {
                // Try ISO format
                return Instant.parse(timestampStr);
            }
        } catch (DateTimeParseException e) {
            log.debug("Could not parse timestamp: {}", timestampStr);
            return null;
        }
    }
}
