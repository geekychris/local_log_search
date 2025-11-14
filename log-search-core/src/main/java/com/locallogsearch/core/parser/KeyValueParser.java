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

public class KeyValueParser implements LogParser {
    private static final Logger log = LoggerFactory.getLogger(KeyValueParser.class);
    
    // Pattern to match key=value pairs
    private static final Pattern KV_PATTERN = Pattern.compile("(\\w+)=([^\\s]+)");
    
    // Common timestamp patterns
    private static final Pattern[] TIMESTAMP_PATTERNS = {
        Pattern.compile("\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:\\d{2})?"),
        Pattern.compile("\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}"),
        Pattern.compile("\\d{2}/\\w{3}/\\d{4}:\\d{2}:\\d{2}:\\d{2}"),
    };
    
    private String delimiter = " ";
    
    @Override
    public void configure(Map<String, String> config) {
        if (config != null && config.containsKey("delimiter")) {
            this.delimiter = config.get("delimiter");
        }
    }
    
    @Override
    public void parse(LogEntry entry) {
        String text = entry.getRawText();
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        
        // Extract timestamp if present
        Instant timestamp = extractTimestamp(text);
        if (timestamp != null) {
            entry.setTimestamp(timestamp);
        }
        
        // Extract key=value pairs
        Matcher matcher = KV_PATTERN.matcher(text);
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2);
            entry.addField(key, value);
        }
    }
    
    private Instant extractTimestamp(String text) {
        for (Pattern pattern : TIMESTAMP_PATTERNS) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String timestampStr = matcher.group();
                try {
                    // Try ISO format first
                    return Instant.parse(timestampStr);
                } catch (DateTimeParseException e) {
                    // Try other common formats
                    try {
                        return DateTimeFormatter.ISO_DATE_TIME.parse(timestampStr, Instant::from);
                    } catch (DateTimeParseException e2) {
                        log.debug("Could not parse timestamp: {}", timestampStr);
                    }
                }
            }
        }
        return null;
    }
}
