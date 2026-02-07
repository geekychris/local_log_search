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

package com.locallogsearch.core.desktop;

import com.locallogsearch.core.config.DesktopSourceConfig;
import com.locallogsearch.core.desktop.parser.FileContentParser;
import com.locallogsearch.core.desktop.parser.FileContentParserRegistry;
import com.locallogsearch.core.desktop.parser.ParsedFileContent;
import com.locallogsearch.core.index.IndexManager;
import com.locallogsearch.core.model.DocumentEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Indexes desktop files recursively from a root directory.
 */
public class DesktopIndexer {
    private static final Logger log = LoggerFactory.getLogger(DesktopIndexer.class);
    
    private final DesktopSourceConfig config;
    private final IndexManager indexManager;
    private final FileContentParserRegistry parserRegistry;
    private final Map<String, FileIndexState> fileStates;
    private final AtomicBoolean isIndexing;
    private Consumer<IndexingResult> progressCallback;
    
    public DesktopIndexer(DesktopSourceConfig config, 
                         IndexManager indexManager,
                         FileContentParserRegistry parserRegistry) {
        this.config = config;
        this.indexManager = indexManager;
        this.parserRegistry = parserRegistry;
        this.fileStates = new HashMap<>();
        this.isIndexing = new AtomicBoolean(false);
    }
    
    /**
     * Set a callback to report indexing progress.
     */
    public void setProgressCallback(Consumer<IndexingResult> callback) {
        this.progressCallback = callback;
    }
    
    /**
     * Load file states from a previous indexing run.
     */
    public void loadFileStates(Map<String, FileIndexState> states) {
        this.fileStates.clear();
        this.fileStates.putAll(states);
    }
    
    /**
     * Get current file states.
     */
    public Map<String, FileIndexState> getFileStates() {
        return new HashMap<>(fileStates);
    }
    
    /**
     * Perform a full index of the directory.
     */
    public IndexingResult indexDirectory() {
        return indexDirectory(false);
    }
    
    /**
     * Perform smart re-indexing - only index changed files.
     */
    public IndexingResult reindexDirectory() {
        return indexDirectory(true);
    }
    
    /**
     * Index the directory.
     * 
     * @param smartReindex if true, skip files that haven't changed
     * @return indexing result
     */
    private IndexingResult indexDirectory(boolean smartReindex) {
        if (!isIndexing.compareAndSet(false, true)) {
            throw new IllegalStateException("Indexing already in progress for: " + config.getId());
        }
        
        try {
            log.info("Starting {} for: {} ({})", 
                smartReindex ? "re-indexing" : "indexing", 
                config.getId(), 
                config.getRootPath());
            
            IndexingResult result = new IndexingResult();
            Path rootPath = Paths.get(config.getRootPath());
            
            if (!Files.exists(rootPath)) {
                result.addError("Root path does not exist: " + config.getRootPath());
                result.finish();
                return result;
            }
            
            // Walk the file tree
            Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    result.incrementFilesScanned();
                    
                    try {
                        // Check if file matches patterns
                        if (!shouldIndexFile(file)) {
                            result.incrementFilesSkipped();
                            return FileVisitResult.CONTINUE;
                        }
                        
                        // Check file size limit
                        if (attrs.size() > config.getMaxFileSizeBytes()) {
                            log.debug("Skipping file (too large): {} ({} bytes)", file, attrs.size());
                            result.incrementFilesSkipped();
                            return FileVisitResult.CONTINUE;
                        }
                        
                        // Check if file needs re-indexing
                        if (smartReindex) {
                            String filePathStr = file.toString();
                            FileIndexState state = fileStates.get(filePathStr);
                            
                            if (state != null && !state.hasChanged(attrs.lastModifiedTime().toMillis(), attrs.size())) {
                                log.debug("Skipping unchanged file: {}", file);
                                result.incrementFilesSkipped();
                                return FileVisitResult.CONTINUE;
                            }
                        }
                        
                        // Index the file
                        indexFile(file, attrs, result);
                        
                    } catch (Exception e) {
                        log.error("Error indexing file: {}", file, e);
                        result.incrementFilesErrored();
                        result.addError("Error indexing " + file + ": " + e.getMessage());
                    }
                    
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    // Check if directory should be excluded
                    if (shouldExcludeDirectory(dir)) {
                        log.debug("Skipping excluded directory: {}", dir);
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    log.warn("Failed to visit file: {}", file, exc);
                    result.incrementFilesErrored();
                    result.addError("Failed to visit " + file + ": " + exc.getMessage());
                    return FileVisitResult.CONTINUE;
                }
            });
            
            // Commit the index
            indexManager.commit(config.getIndexName());
            
            result.finish();
            log.info("Indexing completed: {}", result);
            
            if (progressCallback != null) {
                progressCallback.accept(result);
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("Indexing failed for: {}", config.getId(), e);
            IndexingResult result = new IndexingResult();
            result.addError("Indexing failed: " + e.getMessage());
            result.finish();
            return result;
        } finally {
            isIndexing.set(false);
        }
    }
    
    private void indexFile(Path file, BasicFileAttributes attrs, IndexingResult result) throws IOException {
        // Find appropriate parser
        FileContentParser parser = parserRegistry.findParser(file);
        if (parser == null) {
            log.warn("No parser found for file: {}", file);
            result.incrementFilesSkipped();
            return;
        }
        
        log.debug("Parsing {} with {}", file, parser.getParserName());
        
        // Parse file content
        ParsedFileContent parsed = parser.parse(file, config.getMaxIndexedContentBytes());
        
        // Create document entry
        DocumentEntry entry = new DocumentEntry(file.toString(), config.getIndexName());
        entry.setFileName(file.getFileName().toString());
        entry.setFileSize(attrs.size());
        entry.setCreatedDate(attrs.creationTime().toInstant());
        entry.setModifiedDate(attrs.lastModifiedTime().toInstant());
        entry.setContent(parsed.getTextContent());
        entry.setMetadata(parsed.getMetadata());
        entry.setExtractedFields(parsed.getExtractedFields());
        
        // Index the document
        indexManager.indexDocumentEntry(entry);
        
        // Update file state
        FileIndexState state = new FileIndexState(
            file.toString(),
            attrs.lastModifiedTime().toMillis(),
            attrs.size(),
            Instant.now()
        );
        fileStates.put(file.toString(), state);
        
        result.incrementFilesIndexed();
        result.addBytesIndexed(attrs.size());
    }
    
    private boolean shouldIndexFile(Path file) {
        String fileName = file.getFileName().toString();
        Path relativePath = Paths.get(config.getRootPath()).relativize(file);
        
        // Check include patterns (if specified)
        if (!config.getIncludePatterns().isEmpty()) {
            boolean matches = false;
            for (String pattern : config.getIncludePatterns()) {
                PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
                if (matcher.matches(relativePath) || matcher.matches(file.getFileName())) {
                    matches = true;
                    break;
                }
            }
            if (!matches) {
                return false;
            }
        }
        
        // Check exclude patterns
        for (String pattern : config.getExcludePatterns()) {
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
            if (matcher.matches(relativePath) || matcher.matches(file.getFileName())) {
                return false;
            }
        }
        
        return true;
    }
    
    private boolean shouldExcludeDirectory(Path dir) {
        Path rootPath = Paths.get(config.getRootPath());
        
        // Don't exclude the root itself
        if (dir.equals(rootPath)) {
            return false;
        }
        
        Path relativePath = rootPath.relativize(dir);
        
        // Check exclude patterns
        for (String pattern : config.getExcludePatterns()) {
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
            if (matcher.matches(relativePath)) {
                return true;
            }
        }
        
        return false;
    }
    
    public boolean isIndexing() {
        return isIndexing.get();
    }
    
    public String getSourceId() {
        return config.getId();
    }
}
