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
import com.locallogsearch.core.desktop.parser.FileContentParserRegistry;
import com.locallogsearch.core.index.IndexManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

/**
 * Manages multiple desktop indexers and coordinates indexing operations.
 */
public class DesktopIndexerManager implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(DesktopIndexerManager.class);
    
    private final IndexManager indexManager;
    private final FileContentParserRegistry parserRegistry;
    private final Map<String, DesktopIndexer> indexers;
    private final ExecutorService executorService;
    private final Map<String, Future<IndexingResult>> activeIndexingTasks;
    private BiConsumer<String, Map<String, FileIndexState>> stateCallback;
    
    public DesktopIndexerManager(IndexManager indexManager, 
                                 FileContentParserRegistry parserRegistry,
                                 int threadPoolSize) {
        this.indexManager = indexManager;
        this.parserRegistry = parserRegistry;
        this.indexers = new ConcurrentHashMap<>();
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
        this.activeIndexingTasks = new ConcurrentHashMap<>();
    }
    
    /**
     * Set a callback to be invoked when indexing completes to persist file states.
     * Callback receives: (sourceId, fileStates)
     */
    public void setStateCallback(BiConsumer<String, Map<String, FileIndexState>> callback) {
        this.stateCallback = callback;
    }
    
    /**
     * Register a desktop source.
     */
    public void addDesktopSource(DesktopSourceConfig config, Map<String, FileIndexState> initialStates) {
        if (indexers.containsKey(config.getId())) {
            log.warn("Desktop source already exists: {}", config.getId());
            return;
        }
        
        DesktopIndexer indexer = new DesktopIndexer(config, indexManager, parserRegistry);
        
        // Load initial states if available
        if (initialStates != null) {
            indexer.loadFileStates(initialStates);
        }
        
        // Set progress callback to persist states
        indexer.setProgressCallback(result -> {
            if (stateCallback != null) {
                stateCallback.accept(config.getId(), indexer.getFileStates());
            }
        });
        
        indexers.put(config.getId(), indexer);
        log.info("Added desktop source: {}", config);
    }
    
    /**
     * Remove a desktop source.
     */
    public void removeDesktopSource(String sourceId) {
        // Cancel any active indexing
        Future<IndexingResult> task = activeIndexingTasks.remove(sourceId);
        if (task != null && !task.isDone()) {
            task.cancel(true);
            log.info("Cancelled indexing for: {}", sourceId);
        }
        
        DesktopIndexer indexer = indexers.remove(sourceId);
        if (indexer != null) {
            log.info("Removed desktop source: {}", sourceId);
        }
    }
    
    /**
     * Trigger indexing for a desktop source.
     * 
     * @param sourceId the source ID
     * @return future that completes when indexing is done
     */
    public Future<IndexingResult> triggerIndexing(String sourceId) {
        DesktopIndexer indexer = indexers.get(sourceId);
        if (indexer == null) {
            throw new IllegalArgumentException("Desktop source not found: " + sourceId);
        }
        
        // Check if already indexing
        Future<IndexingResult> existingTask = activeIndexingTasks.get(sourceId);
        if (existingTask != null && !existingTask.isDone()) {
            log.warn("Indexing already in progress for: {}", sourceId);
            return existingTask;
        }
        
        // Submit indexing task
        Future<IndexingResult> task = executorService.submit(() -> {
            try {
                return indexer.indexDirectory();
            } finally {
                activeIndexingTasks.remove(sourceId);
            }
        });
        
        activeIndexingTasks.put(sourceId, task);
        log.info("Started indexing for: {}", sourceId);
        
        return task;
    }
    
    /**
     * Trigger smart re-indexing for a desktop source.
     * 
     * @param sourceId the source ID
     * @return future that completes when re-indexing is done
     */
    public Future<IndexingResult> triggerReindexing(String sourceId) {
        DesktopIndexer indexer = indexers.get(sourceId);
        if (indexer == null) {
            throw new IllegalArgumentException("Desktop source not found: " + sourceId);
        }
        
        // Check if already indexing
        Future<IndexingResult> existingTask = activeIndexingTasks.get(sourceId);
        if (existingTask != null && !existingTask.isDone()) {
            log.warn("Indexing already in progress for: {}", sourceId);
            return existingTask;
        }
        
        // Submit re-indexing task
        Future<IndexingResult> task = executorService.submit(() -> {
            try {
                return indexer.reindexDirectory();
            } finally {
                activeIndexingTasks.remove(sourceId);
            }
        });
        
        activeIndexingTasks.put(sourceId, task);
        log.info("Started re-indexing for: {}", sourceId);
        
        return task;
    }
    
    /**
     * Check if a source is currently indexing.
     */
    public boolean isIndexing(String sourceId) {
        DesktopIndexer indexer = indexers.get(sourceId);
        return indexer != null && indexer.isIndexing();
    }
    
    /**
     * Get the indexer for a source.
     */
    public DesktopIndexer getIndexer(String sourceId) {
        return indexers.get(sourceId);
    }
    
    @Override
    public void close() {
        log.info("Closing DesktopIndexerManager");
        
        // Cancel all active tasks
        for (Map.Entry<String, Future<IndexingResult>> entry : activeIndexingTasks.entrySet()) {
            if (!entry.getValue().isDone()) {
                entry.getValue().cancel(true);
                log.info("Cancelled indexing for: {}", entry.getKey());
            }
        }
        
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
