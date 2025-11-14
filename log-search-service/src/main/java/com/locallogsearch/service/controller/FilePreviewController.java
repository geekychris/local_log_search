package com.locallogsearch.service.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/file")
public class FilePreviewController {
    private static final Logger log = LoggerFactory.getLogger(FilePreviewController.class);
    private static final int MAX_LINES = 200;
    private static final int DEFAULT_LINES = 50;
    
    @PostMapping("/preview")
    public ResponseEntity<Map<String, Object>> previewFile(@RequestBody FilePreviewRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Path path = Paths.get(request.getFilePath());
            
            if (!Files.exists(path)) {
                response.put("success", false);
                response.put("error", "File does not exist: " + request.getFilePath());
                return ResponseEntity.ok(response);
            }
            
            if (!Files.isReadable(path)) {
                response.put("success", false);
                response.put("error", "File is not readable: " + request.getFilePath());
                return ResponseEntity.ok(response);
            }
            
            int maxLines = request.getMaxLines() != null && request.getMaxLines() > 0 
                ? Math.min(request.getMaxLines(), MAX_LINES) 
                : DEFAULT_LINES;
            
            List<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
                String line;
                int count = 0;
                while ((line = reader.readLine()) != null && count < maxLines) {
                    if (!line.trim().isEmpty()) {
                        lines.add(line);
                        count++;
                    }
                }
            }
            
            response.put("success", true);
            response.put("lines", lines);
            response.put("filePath", request.getFilePath());
            
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            log.error("Error reading file: " + request.getFilePath(), e);
            response.put("success", false);
            response.put("error", "Error reading file: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
    
    public static class FilePreviewRequest {
        private String filePath;
        private Integer maxLines;
        
        public String getFilePath() {
            return filePath;
        }
        
        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }
        
        public Integer getMaxLines() {
            return maxLines;
        }
        
        public void setMaxLines(Integer maxLines) {
            this.maxLines = maxLines;
        }
    }
}
