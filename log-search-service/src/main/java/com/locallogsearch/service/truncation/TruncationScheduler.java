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

package com.locallogsearch.service.truncation;

import com.locallogsearch.core.index.IndexManager;
import com.locallogsearch.core.truncation.TruncationConfig;
import com.locallogsearch.service.repository.TruncationConfigRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Service that manages automatic truncation of indices based on configured policies.
 */
@Service
public class TruncationScheduler {
    private static final Logger log = LoggerFactory.getLogger(TruncationScheduler.class);
    
    private final IndexManager indexManager;
    private final TruncationConfigRepository repository;
    private final Map<String, TruncationConfig> truncationConfigs;
    private final Map<String, ScheduledFuture<?>> scheduledTasks;
    private final ScheduledExecutorService scheduler;
    
    public TruncationScheduler(IndexManager indexManager, TruncationConfigRepository repository) {
        this.indexManager = indexManager;
        this.repository = repository;
        this.truncationConfigs = new ConcurrentHashMap<>();
        this.scheduledTasks = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(2);
    }
    
    /**
     * Initialize and restore saved truncation configurations.
     */
    @PostConstruct
    public void initialize() {
        log.info("Initializing TruncationScheduler");
        Map<String, TruncationConfig> savedConfigs = repository.loadAll();
        
        for (TruncationConfig config : savedConfigs.values()) {
            if (config.isAutoTruncateEnabled()) {
                scheduleAutoTruncation(config);
            }
            truncationConfigs.put(config.getIndexName(), config);
        }
        
        log.info("TruncationScheduler initialized with {} configurations, {} scheduled", 
            truncationConfigs.size(), scheduledTasks.size());
    }
    
    /**
     * Shutdown the scheduler gracefully.
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down TruncationScheduler");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Set or update a truncation configuration for an index.
     */
    public void setTruncationConfig(TruncationConfig config) {
        log.info("Setting truncation config: {}", config);
        
        // Remove existing scheduled task if any
        cancelScheduledTruncation(config.getIndexName());
        
        // Store the new configuration
        truncationConfigs.put(config.getIndexName(), config);
        
        // Schedule if auto-truncation is enabled
        if (config.isAutoTruncateEnabled()) {
            scheduleAutoTruncation(config);
        }
        
        // Persist to disk
        repository.saveAll(truncationConfigs);
    }
    
    /**
     * Remove truncation configuration for an index.
     */
    public void removeTruncationConfig(String indexName) {
        log.info("Removing truncation config for index: {}", indexName);
        cancelScheduledTruncation(indexName);
        truncationConfigs.remove(indexName);
        repository.saveAll(truncationConfigs);
    }
    
    /**
     * Get truncation configuration for an index.
     */
    public TruncationConfig getTruncationConfig(String indexName) {
        return truncationConfigs.get(indexName);
    }
    
    /**
     * Get all truncation configurations.
     */
    public Map<String, TruncationConfig> getAllConfigs() {
        return new ConcurrentHashMap<>(truncationConfigs);
    }
    
    /**
     * Manually trigger truncation for an index.
     */
    public int executeTruncation(String indexName) throws IOException {
        TruncationConfig config = truncationConfigs.get(indexName);
        if (config == null) {
            log.warn("No truncation config found for index: {}", indexName);
            return 0;
        }
        
        return indexManager.truncateIndex(config);
    }
    
    /**
     * Schedule automatic truncation for an index.
     */
    private void scheduleAutoTruncation(TruncationConfig config) {
        if (config.getTruncationInterval() == null) {
            log.warn("Cannot schedule truncation without interval: {}", config.getIndexName());
            return;
        }
        
        long intervalSeconds = config.getTruncationInterval().getSeconds();
        
        Runnable truncationTask = () -> {
            try {
                log.info("Running scheduled truncation for index: {}", config.getIndexName());
                int deletedCount = indexManager.truncateIndex(config);
                log.info("Scheduled truncation completed for {}: deleted {} documents", 
                    config.getIndexName(), deletedCount);
            } catch (Exception e) {
                log.error("Error during scheduled truncation for index: {}", config.getIndexName(), e);
            }
        };
        
        ScheduledFuture<?> scheduledTask = scheduler.scheduleAtFixedRate(
            truncationTask,
            intervalSeconds, // Initial delay
            intervalSeconds, // Period
            TimeUnit.SECONDS
        );
        
        scheduledTasks.put(config.getIndexName(), scheduledTask);
        log.info("Scheduled auto-truncation for {} every {} seconds", 
            config.getIndexName(), intervalSeconds);
    }
    
    /**
     * Cancel scheduled truncation for an index.
     */
    private void cancelScheduledTruncation(String indexName) {
        ScheduledFuture<?> task = scheduledTasks.remove(indexName);
        if (task != null) {
            task.cancel(false);
            log.info("Cancelled scheduled truncation for index: {}", indexName);
        }
    }
}
