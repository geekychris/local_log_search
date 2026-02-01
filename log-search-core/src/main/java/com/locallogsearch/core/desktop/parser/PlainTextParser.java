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

import org.apache.tika.Tika;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Parser for plain text files and source code.
 */
public class PlainTextParser implements FileContentParser {
    private static final Set<String> TEXT_EXTENSIONS = new HashSet<>(Arrays.asList(
        // Plain text
        "txt", "text", "log",
        // Source code
        "java", "py", "js", "ts", "jsx", "tsx", "go", "rs", "c", "cpp", "h", "hpp",
        "cs", "php", "rb", "swift", "kt", "scala", "sh", "bash", "zsh", "fish",
        // Web
        "html", "htm", "css", "scss", "sass", "less",
        // Config
        "yaml", "yml", "toml", "ini", "conf", "config", "properties",
        // Data
        "sql", "graphql", "proto",
        // Docs
        "md", "rst", "adoc", "tex"
    ));
    
    private final Tika tika = new Tika();
    
    @Override
    public boolean supports(Path filePath) {
        String fileName = filePath.getFileName().toString().toLowerCase();
        int lastDot = fileName.lastIndexOf('.');
        
        if (lastDot > 0) {
            String extension = fileName.substring(lastDot + 1);
            if (TEXT_EXTENSIONS.contains(extension)) {
                return true;
            }
        }
        
        // Fallback: use Tika to detect text files
        try {
            String mimeType = tika.detect(filePath);
            return mimeType != null && mimeType.startsWith("text/");
        } catch (IOException e) {
            return false;
        }
    }
    
    @Override
    public ParsedFileContent parse(Path filePath, long maxContentBytes) throws IOException {
        byte[] bytes;
        
        long fileSize = Files.size(filePath);
        if (fileSize <= maxContentBytes) {
            // Read entire file
            bytes = Files.readAllBytes(filePath);
        } else {
            // Read only up to maxContentBytes
            bytes = new byte[(int) maxContentBytes];
            Files.newInputStream(filePath).read(bytes);
        }
        
        String content = new String(bytes, StandardCharsets.UTF_8);
        ParsedFileContent result = new ParsedFileContent(content);
        
        // Add mime type as metadata
        String mimeType = tika.detect(filePath);
        if (mimeType != null) {
            result.addMetadata("mime_type", mimeType);
        }
        
        // Add encoding metadata
        result.addMetadata("encoding", "UTF-8");
        
        return result;
    }
    
    @Override
    public String getParserName() {
        return "PlainTextParser";
    }
    
    @Override
    public int getPriority() {
        return 10; // Higher priority for known text formats
    }
}
