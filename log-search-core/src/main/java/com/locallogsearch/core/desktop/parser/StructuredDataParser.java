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

package com.locallogsearch.core.desktop.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;

/**
 * Parser for structured data files (JSON, XML, CSV).
 * Extracts both raw content and structured fields.
 */
public class StructuredDataParser implements FileContentParser {
    private static final Logger log = LoggerFactory.getLogger(StructuredDataParser.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public boolean supports(Path filePath) {
        String fileName = filePath.getFileName().toString().toLowerCase();
        return fileName.endsWith(".json") || 
               fileName.endsWith(".xml") || 
               fileName.endsWith(".csv");
    }
    
    @Override
    public ParsedFileContent parse(Path filePath, long maxContentBytes) throws IOException {
        String fileName = filePath.getFileName().toString().toLowerCase();
        
        if (fileName.endsWith(".json")) {
            return parseJson(filePath, maxContentBytes);
        } else if (fileName.endsWith(".xml")) {
            return parseXml(filePath, maxContentBytes);
        } else if (fileName.endsWith(".csv")) {
            return parseCsv(filePath, maxContentBytes);
        }
        
        throw new IOException("Unsupported structured data type: " + fileName);
    }
    
    private ParsedFileContent parseJson(Path filePath, long maxContentBytes) throws IOException {
        try {
            // Read file content - just index as searchable text without extracting fields
            String content = readFileContent(filePath, maxContentBytes);
            ParsedFileContent result = new ParsedFileContent(content);
            
            // Only add mime type metadata, don't extract individual JSON fields
            // This prevents facet explosion from structured data
            result.addMetadata("mime_type", "application/json");
            return result;
        } catch (Exception e) {
            log.error("Failed to parse JSON: {}", filePath, e);
            throw new IOException("Failed to parse JSON: " + filePath, e);
        }
    }
    
    private void extractJsonFields(JsonNode node, String prefix, ParsedFileContent result, int depth, int maxDepth) {
        if (depth >= maxDepth || node == null) {
            return;
        }
        
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String fieldName = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
                JsonNode value = entry.getValue();
                
                if (value.isValueNode()) {
                    result.addExtractedField(fieldName, value.asText());
                } else {
                    extractJsonFields(value, fieldName, result, depth + 1, maxDepth);
                }
            }
        } else if (node.isArray() && node.size() > 0) {
            // Extract fields from first array element as representative
            extractJsonFields(node.get(0), prefix, result, depth + 1, maxDepth);
        }
    }
    
    private ParsedFileContent parseXml(Path filePath, long maxContentBytes) throws IOException {
        // For XML, just index as text content for now
        // More sophisticated XML parsing could be added later
        String content = readFileContent(filePath, maxContentBytes);
        ParsedFileContent result = new ParsedFileContent(content);
        result.addMetadata("mime_type", "application/xml");
        return result;
    }
    
    private ParsedFileContent parseCsv(Path filePath, long maxContentBytes) throws IOException {
        StringBuilder content = new StringBuilder();
        
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line;
            boolean firstLine = true;
            String[] headers = null;
            
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    headers = line.split(",");
                    firstLine = false;
                }
                
                content.append(line).append("\n");
                
                if (content.length() > maxContentBytes) {
                    break;
                }
            }
            
            // Truncate if needed
            String finalContent = content.toString();
            if (finalContent.length() > maxContentBytes) {
                finalContent = finalContent.substring(0, (int) maxContentBytes);
            }
            
            ParsedFileContent result = new ParsedFileContent(finalContent);
            result.addMetadata("mime_type", "text/csv");
            
            // Add header info if available
            if (headers != null && headers.length > 0) {
                result.addMetadata("column_count", String.valueOf(headers.length));
                result.addExtractedField("headers", String.join(", ", headers));
            }
            
            return result;
        } catch (Exception e) {
            log.error("Failed to parse CSV: {}", filePath, e);
            throw new IOException("Failed to parse CSV: " + filePath, e);
        }
    }
    
    private String readFileContent(Path filePath, long maxContentBytes) throws IOException {
        byte[] bytes;
        long fileSize = Files.size(filePath);
        
        if (fileSize <= maxContentBytes) {
            bytes = Files.readAllBytes(filePath);
        } else {
            bytes = new byte[(int) maxContentBytes];
            Files.newInputStream(filePath).read(bytes);
        }
        
        return new String(bytes, StandardCharsets.UTF_8);
    }
    
    @Override
    public String getParserName() {
        return "StructuredDataParser";
    }
    
    @Override
    public int getPriority() {
        return 15; // Medium-high priority
    }
}
