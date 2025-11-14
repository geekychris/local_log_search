package com.locallogsearch.service.controller;

import com.locallogsearch.core.model.LogEntry;
import com.locallogsearch.core.parser.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/parser")
public class ParserTestController {
    private static final Logger log = LoggerFactory.getLogger(ParserTestController.class);
    
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testParser(@RequestBody ParserTestRequest request) {
        try {
            LogParser parser = ParserFactory.createParser(request.getParserType(), request.getParserConfig());
            
            // Create a LogEntry with the test log line
            LogEntry entry = new LogEntry(request.getLogEntry(), "test", "test");
            
            // Parse the entry in-place
            parser.parse(entry);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("timestamp", entry.getTimestamp() != null ? entry.getTimestamp().toEpochMilli() : null);
            result.put("fields", entry.getFields());
            result.put("rawText", request.getLogEntry());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error testing parser", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            error.put("rawText", request.getLogEntry());
            return ResponseEntity.ok(error);
        }
    }
    
    public static class ParserTestRequest {
        private String logEntry;
        private String parserType;
        private Map<String, String> parserConfig;
        
        public String getLogEntry() {
            return logEntry;
        }
        
        public void setLogEntry(String logEntry) {
            this.logEntry = logEntry;
        }
        
        public String getParserType() {
            return parserType;
        }
        
        public void setParserType(String parserType) {
            this.parserType = parserType;
        }
        
        public Map<String, String> getParserConfig() {
            return parserConfig;
        }
        
        public void setParserConfig(Map<String, String> parserConfig) {
            this.parserConfig = parserConfig;
        }
    }
}
