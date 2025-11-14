package com.locallogsearch.core.parser;

import com.locallogsearch.core.model.LogEntry;
import io.krakens.grok.api.Grok;
import io.krakens.grok.api.GrokCompiler;
import io.krakens.grok.api.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

public class GrokParser implements LogParser {
    private static final Logger log = LoggerFactory.getLogger(GrokParser.class);
    
    private Grok grok;
    private String timestampField;
    private String timestampFormat;
    
    @Override
    public void configure(Map<String, String> config) {
        if (config == null || !config.containsKey("pattern")) {
            throw new IllegalArgumentException("Grok parser requires 'pattern' configuration");
        }
        
        try {
            GrokCompiler grokCompiler = GrokCompiler.newInstance();
            grokCompiler.registerDefaultPatterns();
            
            String pattern = config.get("pattern");
            this.grok = grokCompiler.compile(pattern);
            
            this.timestampField = config.get("timestamp.field");
            this.timestampFormat = config.get("timestamp.format");
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to compile Grok pattern", e);
        }
    }
    
    @Override
    public void parse(LogEntry entry) {
        String text = entry.getRawText();
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        
        Match match = grok.match(text);
        Map<String, Object> capture = match.capture();
        
        for (Map.Entry<String, Object> field : capture.entrySet()) {
            String key = field.getKey();
            Object value = field.getValue();
            
            if (value != null) {
                entry.addField(key, value.toString());
                
                // Check if this is the timestamp field
                if (timestampField != null && timestampField.equals(key)) {
                    Instant timestamp = parseTimestamp(value.toString());
                    if (timestamp != null) {
                        entry.setTimestamp(timestamp);
                    }
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
                return Instant.parse(timestampStr);
            }
        } catch (DateTimeParseException e) {
            log.debug("Could not parse timestamp: {}", timestampStr);
            return null;
        }
    }
}
