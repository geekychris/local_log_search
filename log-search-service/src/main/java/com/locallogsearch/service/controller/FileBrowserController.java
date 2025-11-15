package com.locallogsearch.service.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/files")
public class FileBrowserController {
    private static final Logger log = LoggerFactory.getLogger(FileBrowserController.class);
    
    /**
     * List files and directories in a given path.
     */
    @GetMapping("/browse")
    public ResponseEntity<Map<String, Object>> browseDirectory(@RequestParam(required = false) String path) {
        try {
            // Default to user home if no path provided
            Path dirPath = path != null && !path.isEmpty() 
                ? Paths.get(path) 
                : Paths.get(System.getProperty("user.home"));
            
            if (!Files.exists(dirPath)) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "Path does not exist: " + dirPath)
                );
            }
            
            if (!Files.isDirectory(dirPath)) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "Path is not a directory: " + dirPath)
                );
            }
            
            List<Map<String, Object>> entries = new ArrayList<>();
            
            // Add parent directory entry if not at root
            Path parent = dirPath.getParent();
            if (parent != null) {
                Map<String, Object> parentEntry = new HashMap<>();
                parentEntry.put("name", "..");
                parentEntry.put("path", parent.toString());
                parentEntry.put("isDirectory", true);
                parentEntry.put("isParent", true);
                entries.add(parentEntry);
            }
            
            // List directory contents
            try (Stream<Path> stream = Files.list(dirPath)) {
                List<Path> paths = stream
                    .filter(p -> {
                        try {
                            // Filter out hidden files on Unix-like systems
                            return !p.getFileName().toString().startsWith(".");
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .sorted((p1, p2) -> {
                        // Directories first, then by name
                        boolean isDir1 = Files.isDirectory(p1);
                        boolean isDir2 = Files.isDirectory(p2);
                        if (isDir1 && !isDir2) return -1;
                        if (!isDir1 && isDir2) return 1;
                        return p1.getFileName().toString().compareToIgnoreCase(
                            p2.getFileName().toString()
                        );
                    })
                    .collect(Collectors.toList());
                
                for (Path p : paths) {
                    try {
                        Map<String, Object> entry = new HashMap<>();
                        entry.put("name", p.getFileName().toString());
                        entry.put("path", p.toString());
                        entry.put("isDirectory", Files.isDirectory(p));
                        entry.put("isParent", false);
                        
                        if (!Files.isDirectory(p)) {
                            entry.put("size", Files.size(p));
                            entry.put("sizeFormatted", formatSize(Files.size(p)));
                            
                            // Check if it looks like a log file
                            String name = p.getFileName().toString().toLowerCase();
                            boolean isLogFile = name.endsWith(".log") || 
                                               name.endsWith(".txt") ||
                                               name.contains("log");
                            entry.put("isLogFile", isLogFile);
                        }
                        
                        entries.add(entry);
                    } catch (IOException e) {
                        log.warn("Could not read file info: {}", p, e);
                    }
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("currentPath", dirPath.toString());
            response.put("entries", entries);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error browsing directory", e);
            return ResponseEntity.internalServerError().body(
                Map.of("error", "Failed to browse directory: " + e.getMessage())
            );
        }
    }
    
    /**
     * Get common/favorite directories.
     */
    @GetMapping("/quick-paths")
    public ResponseEntity<List<Map<String, String>>> getQuickPaths() {
        List<Map<String, String>> paths = new ArrayList<>();
        
        String userHome = System.getProperty("user.home");
        
        paths.add(Map.of("name", "Home", "path", userHome));
        paths.add(Map.of("name", "Logs (/var/log)", "path", "/var/log"));
        paths.add(Map.of("name", "Tmp (/tmp)", "path", "/tmp"));
        paths.add(Map.of("name", "Root (/)", "path", "/"));
        
        // Add common log locations if they exist
        addIfExists(paths, "System Logs", "/var/log");
        addIfExists(paths, "User Logs", userHome + "/logs");
        addIfExists(paths, "Desktop", userHome + "/Desktop");
        addIfExists(paths, "Documents", userHome + "/Documents");
        
        return ResponseEntity.ok(paths);
    }
    
    private void addIfExists(List<Map<String, String>> paths, String name, String path) {
        if (Files.exists(Paths.get(path))) {
            // Avoid duplicates
            if (paths.stream().noneMatch(p -> p.get("path").equals(path))) {
                paths.add(Map.of("name", name, "path", path));
            }
        }
    }
    
    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
