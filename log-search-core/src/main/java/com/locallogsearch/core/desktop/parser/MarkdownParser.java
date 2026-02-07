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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for Markdown files.
 * Extracts headings and treats them as metadata.
 */
public class MarkdownParser implements FileContentParser {
    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);
    
    @Override
    public boolean supports(Path filePath) {
        String fileName = filePath.getFileName().toString().toLowerCase();
        return fileName.endsWith(".md") || fileName.endsWith(".markdown");
    }
    
    @Override
    public ParsedFileContent parse(Path filePath, long maxContentBytes) throws IOException {
        byte[] bytes;
        long fileSize = Files.size(filePath);
        
        if (fileSize <= maxContentBytes) {
            bytes = Files.readAllBytes(filePath);
        } else {
            bytes = new byte[(int) maxContentBytes];
            Files.newInputStream(filePath).read(bytes);
        }
        
        String content = new String(bytes, StandardCharsets.UTF_8);
        ParsedFileContent result = new ParsedFileContent(content);
        
        // Extract headings as metadata
        Matcher matcher = HEADING_PATTERN.matcher(content);
        StringBuilder headings = new StringBuilder();
        int headingCount = 0;
        
        while (matcher.find() && headingCount < 10) { // Limit to first 10 headings
            String heading = matcher.group(2);
            if (headings.length() > 0) {
                headings.append(", ");
            }
            headings.append(heading);
            headingCount++;
        }
        
        if (headings.length() > 0) {
            result.addExtractedField("headings", headings.toString());
        }
        
        result.addMetadata("mime_type", "text/markdown");
        return result;
    }
    
    @Override
    public String getParserName() {
        return "MarkdownParser";
    }
    
    @Override
    public int getPriority() {
        return 12; // Slightly higher than plain text
    }
}
