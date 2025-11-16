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
