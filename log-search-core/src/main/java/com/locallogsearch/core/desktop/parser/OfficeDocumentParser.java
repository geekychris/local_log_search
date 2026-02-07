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

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Parser for Microsoft Office documents (.docx, .xlsx) using Apache POI.
 */
public class OfficeDocumentParser implements FileContentParser {
    private static final Logger log = LoggerFactory.getLogger(OfficeDocumentParser.class);
    
    @Override
    public boolean supports(Path filePath) {
        String fileName = filePath.getFileName().toString().toLowerCase();
        return fileName.endsWith(".docx") || fileName.endsWith(".xlsx");
    }
    
    @Override
    public ParsedFileContent parse(Path filePath, long maxContentBytes) throws IOException {
        String fileName = filePath.getFileName().toString().toLowerCase();
        
        if (fileName.endsWith(".docx")) {
            return parseDocx(filePath, maxContentBytes);
        } else if (fileName.endsWith(".xlsx")) {
            return parseXlsx(filePath, maxContentBytes);
        }
        
        throw new IOException("Unsupported file type: " + fileName);
    }
    
    private ParsedFileContent parseDocx(Path filePath, long maxContentBytes) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             XWPFDocument document = new XWPFDocument(fis)) {
            
            StringBuilder content = new StringBuilder();
            
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText();
                if (text != null && !text.trim().isEmpty()) {
                    content.append(text).append("\n");
                    
                    // Check size limit
                    if (content.length() > maxContentBytes) {
                        break;
                    }
                }
            }
            
            // Truncate if needed
            String finalContent = content.toString();
            if (finalContent.length() > maxContentBytes) {
                finalContent = finalContent.substring(0, (int) maxContentBytes);
            }
            
            ParsedFileContent result = new ParsedFileContent(finalContent);
            
            // Extract metadata
            result.addMetadata("mime_type", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            
            // Extract core properties if available
            try {
                var props = document.getProperties();
                if (props != null && props.getCoreProperties() != null) {
                    var coreProps = props.getCoreProperties();
                    if (coreProps.getTitle() != null) {
                        result.addMetadata("title", coreProps.getTitle());
                    }
                    if (coreProps.getCreator() != null) {
                        result.addMetadata("author", coreProps.getCreator());
                    }
                    if (coreProps.getSubject() != null) {
                        result.addMetadata("subject", coreProps.getSubject());
                    }
                }
            } catch (Exception e) {
                log.debug("Could not extract core properties from: {}", filePath, e);
            }
            
            return result;
        } catch (Exception e) {
            log.error("Failed to parse DOCX: {}", filePath, e);
            throw new IOException("Failed to parse DOCX: " + filePath, e);
        }
    }
    
    private ParsedFileContent parseXlsx(Path filePath, long maxContentBytes) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
            
            StringBuilder content = new StringBuilder();
            DataFormatter formatter = new DataFormatter();
            
            // Extract text from all sheets
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                content.append("Sheet: ").append(sheet.getSheetName()).append("\n");
                
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        String cellValue = formatter.formatCellValue(cell);
                        if (!cellValue.isEmpty()) {
                            content.append(cellValue).append("\t");
                        }
                    }
                    content.append("\n");
                    
                    // Check size limit
                    if (content.length() > maxContentBytes) {
                        break;
                    }
                }
                
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
            
            // Extract metadata
            result.addMetadata("mime_type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            result.addMetadata("sheet_count", String.valueOf(workbook.getNumberOfSheets()));
            
            return result;
        } catch (Exception e) {
            log.error("Failed to parse XLSX: {}", filePath, e);
            throw new IOException("Failed to parse XLSX: " + filePath, e);
        }
    }
    
    @Override
    public String getParserName() {
        return "OfficeDocumentParser";
    }
    
    @Override
    public int getPriority() {
        return 20; // Higher priority than plain text
    }
}
