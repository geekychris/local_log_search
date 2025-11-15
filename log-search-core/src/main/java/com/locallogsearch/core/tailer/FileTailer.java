package com.locallogsearch.core.tailer;

import com.locallogsearch.core.config.LogSourceConfig;
import com.locallogsearch.core.index.IndexManager;
import com.locallogsearch.core.model.LogEntry;
import com.locallogsearch.core.parser.LogParser;
import com.locallogsearch.core.parser.ParserFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Consumer;

public class FileTailer implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(FileTailer.class);
    private static final long POLL_INTERVAL_MS = 1000;
    private static final long CHECKPOINT_INTERVAL_MS = 10000; // Save state every 10 seconds
    
    private final LogSourceConfig config;
    private final IndexManager indexManager;
    private final LogParser parser;
    private volatile boolean running = true;
    private long filePointer = 0;
    private String currentFileKey = null; // Unique file identifier
    private long lastCheckpointTime = 0;
    private Consumer<FileTailerState> checkpointCallback;
    
    public FileTailer(LogSourceConfig config, IndexManager indexManager) {
        this(config, indexManager, null);
    }
    
    public FileTailer(LogSourceConfig config, IndexManager indexManager, FileTailerState initialState) {
        this.config = config;
        this.indexManager = indexManager;
        this.parser = ParserFactory.createParser(config.getParserType(), config.getParserConfig());
        
        // Restore from checkpoint if available
        if (initialState != null) {
            this.filePointer = initialState.getFilePointer();
            this.currentFileKey = initialState.getFileKey();
            log.info("Restored tailer for {} from checkpoint at position {} with fileKey {}", 
                config.getFilePath(), filePointer, currentFileKey);
        } else {
            // Start from end of file if it exists
            Path path = Paths.get(config.getFilePath());
            if (Files.exists(path)) {
                try {
                    this.filePointer = Files.size(path);
                    this.currentFileKey = getFileKey(path);
                    log.info("Starting tailer for {} at position {} with fileKey {}", 
                        config.getFilePath(), filePointer, currentFileKey);
                } catch (IOException e) {
                    log.warn("Could not determine file info for {}", config.getFilePath(), e);
                }
            }
        }
    }
    
    /**
     * Set a callback to be invoked when state should be checkpointed.
     */
    public void setCheckpointCallback(Consumer<FileTailerState> callback) {
        this.checkpointCallback = callback;
    }
    
    @Override
    public void run() {
        log.info("FileTailer started for: {}", config.getFilePath());
        
        while (running && config.isEnabled()) {
            try {
                tailFile();
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                log.info("FileTailer interrupted for: {}", config.getFilePath());
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error tailing file: {}", config.getFilePath(), e);
                try {
                    Thread.sleep(POLL_INTERVAL_MS * 5);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        log.info("FileTailer stopped for: {}", config.getFilePath());
    }
    
    private void tailFile() throws IOException {
        Path path = Paths.get(config.getFilePath());
        
        if (!Files.exists(path)) {
            log.debug("File does not exist: {}", config.getFilePath());
            return;
        }
        
        // Check for file rotation (file was deleted and recreated)
        String fileKey = getFileKey(path);
        if (currentFileKey != null && !currentFileKey.equals(fileKey)) {
            log.info("File rotation detected for {}. Old key: {}, New key: {}. Resetting to start of file.",
                config.getFilePath(), currentFileKey, fileKey);
            filePointer = 0;
            currentFileKey = fileKey;
        } else if (currentFileKey == null) {
            currentFileKey = fileKey;
        }
        
        long fileSize = Files.size(path);
        
        // Check if file was truncated
        if (fileSize < filePointer) {
            log.info("File was truncated, resetting position: {}", config.getFilePath());
            filePointer = 0;
        }
        
        // Check if there's new content
        if (fileSize == filePointer) {
            return;
        }
        
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "r")) {
            file.seek(filePointer);
            
            String line;
            while ((line = file.readLine()) != null) {
                processLine(line);
            }
            
            filePointer = file.getFilePointer();
        }
        
        // Checkpoint state periodically
        long now = System.currentTimeMillis();
        if (checkpointCallback != null && (now - lastCheckpointTime) >= CHECKPOINT_INTERVAL_MS) {
            checkpointState(path);
            lastCheckpointTime = now;
        }
    }
    
    /**
     * Get a unique identifier for the file based on creation time and size.
     * This helps detect when a file is deleted and recreated.
     */
    private String getFileKey(Path path) throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
        // Use creation time + file size as a pseudo-inode
        return attrs.creationTime().toMillis() + ":" + attrs.size();
    }
    
    /**
     * Save the current tailer state via checkpoint callback.
     */
    private void checkpointState(Path path) {
        try {
            long lastModifiedTime = Files.getLastModifiedTime(path).toMillis();
            long fileSize = Files.size(path);
            
            FileTailerState state = new FileTailerState(
                config.getFilePath(),
                filePointer,
                lastModifiedTime,
                fileSize,
                currentFileKey
            );
            
            checkpointCallback.accept(state);
            log.debug("Checkpointed state for {}: position={}", config.getFilePath(), filePointer);
        } catch (IOException e) {
            log.error("Failed to checkpoint state for {}", config.getFilePath(), e);
        }
    }
    
    private void processLine(String line) {
        try {
            LogEntry entry = new LogEntry(line, config.getFilePath(), config.getIndexName());
            parser.parse(entry);
            indexManager.indexLogEntry(entry);
        } catch (Exception e) {
            log.error("Failed to process log line: {}", line, e);
        }
    }
    
    public void stop() {
        this.running = false;
        
        // Final checkpoint on shutdown
        if (checkpointCallback != null) {
            try {
                Path path = Paths.get(config.getFilePath());
                if (Files.exists(path)) {
                    checkpointState(path);
                }
            } catch (Exception e) {
                log.error("Failed to perform final checkpoint for {}", config.getFilePath(), e);
            }
        }
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public String getSourceId() {
        return config.getId();
    }
}
