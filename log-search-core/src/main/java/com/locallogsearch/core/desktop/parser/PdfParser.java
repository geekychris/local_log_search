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

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Calendar;

/**
 * Parser for PDF files using Apache PDFBox.
 */
public class PdfParser implements FileContentParser {
    private static final Logger log = LoggerFactory.getLogger(PdfParser.class);
    
    @Override
    public boolean supports(Path filePath) {
        String fileName = filePath.getFileName().toString().toLowerCase();
        return fileName.endsWith(".pdf");
    }
    
    @Override
    public ParsedFileContent parse(Path filePath, long maxContentBytes) throws IOException {
        try (PDDocument document = org.apache.pdfbox.Loader.loadPDF(filePath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            
            // Truncate if needed
            if (text.length() > maxContentBytes) {
                text = text.substring(0, (int) maxContentBytes);
            }
            
            ParsedFileContent result = new ParsedFileContent(text);
            
            // Extract PDF metadata
            PDDocumentInformation info = document.getDocumentInformation();
            if (info != null) {
                if (info.getTitle() != null) {
                    result.addMetadata("title", info.getTitle());
                }
                if (info.getAuthor() != null) {
                    result.addMetadata("author", info.getAuthor());
                }
                if (info.getSubject() != null) {
                    result.addMetadata("subject", info.getSubject());
                }
                if (info.getKeywords() != null) {
                    result.addMetadata("keywords", info.getKeywords());
                }
                if (info.getCreator() != null) {
                    result.addMetadata("creator", info.getCreator());
                }
                if (info.getProducer() != null) {
                    result.addMetadata("producer", info.getProducer());
                }
                
                Calendar creationDate = info.getCreationDate();
                if (creationDate != null) {
                    result.addMetadata("creation_date", creationDate.toInstant().toString());
                }
                
                Calendar modDate = info.getModificationDate();
                if (modDate != null) {
                    result.addMetadata("modification_date", modDate.toInstant().toString());
                }
            }
            
            // Add page count
            result.addMetadata("page_count", String.valueOf(document.getNumberOfPages()));
            result.addMetadata("mime_type", "application/pdf");
            
            return result;
        } catch (Exception e) {
            log.error("Failed to parse PDF: {}", filePath, e);
            throw new IOException("Failed to parse PDF: " + filePath, e);
        }
    }
    
    @Override
    public String getParserName() {
        return "PdfParser";
    }
    
    @Override
    public int getPriority() {
        return 20; // Higher priority than plain text
    }
}
