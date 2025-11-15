package com.locallogsearch.core.tailer;

import com.locallogsearch.core.config.LogSourceConfig;
import com.locallogsearch.core.index.IndexManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class TailerManager implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(TailerManager.class);
    
    private final IndexManager indexManager;
    private final Map<String, FileTailer> tailers;
    private final ExecutorService executorService;
    private BiConsumer<String, FileTailerState> checkpointCallback;
    
    public TailerManager(IndexManager indexManager) {
        this.indexManager = indexManager;
        this.tailers = new ConcurrentHashMap<>();
        this.executorService = Executors.newCachedThreadPool();
    }
    
    /**
     * Set a callback to be invoked when tailer state should be checkpointed.
     * Callback receives: (sourceId, tailerState)
     */
    public void setCheckpointCallback(BiConsumer<String, FileTailerState> callback) {
        this.checkpointCallback = callback;
    }
    
    public void addLogSource(LogSourceConfig config) {
        addLogSource(config, null);
    }
    
    public void addLogSource(LogSourceConfig config, FileTailerState initialState) {
        if (tailers.containsKey(config.getId())) {
            log.warn("Log source already exists: {}", config.getId());
            return;
        }
        
        FileTailer tailer = new FileTailer(config, indexManager, initialState);
        
        // Set checkpoint callback if configured
        if (checkpointCallback != null) {
            tailer.setCheckpointCallback(state -> checkpointCallback.accept(config.getId(), state));
        }
        
        tailers.put(config.getId(), tailer);
        executorService.submit(tailer);
        
        log.info("Added log source: {}", config);
    }
    
    public void removeLogSource(String id) {
        FileTailer tailer = tailers.remove(id);
        if (tailer != null) {
            tailer.stop();
            log.info("Removed log source: {}", id);
        }
    }
    
    public void updateLogSource(LogSourceConfig config) {
        removeLogSource(config.getId());
        addLogSource(config);
    }
    
    /**
     * Reindex a log source by restarting the tailer from the beginning of the file.
     * This removes the existing tailer and starts a new one with no initial state,
     * causing it to read from the start of the file.
     * 
     * @param config the log source configuration
     */
    public void reindexLogSource(LogSourceConfig config) {
        log.info("Reindexing log source: {}", config.getId());
        removeLogSource(config.getId());
        // Add with null initial state to start from beginning
        addLogSource(config, null);
    }
    
    public Map<String, FileTailer> getTailers() {
        return new ConcurrentHashMap<>(tailers);
    }
    
    @Override
    public void close() {
        log.info("Closing TailerManager");
        
        for (FileTailer tailer : tailers.values()) {
            tailer.stop();
        }
        
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
