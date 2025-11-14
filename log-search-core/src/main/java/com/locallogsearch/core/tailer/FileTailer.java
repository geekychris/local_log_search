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

public class FileTailer implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(FileTailer.class);
    private static final long POLL_INTERVAL_MS = 1000;
    
    private final LogSourceConfig config;
    private final IndexManager indexManager;
    private final LogParser parser;
    private volatile boolean running = true;
    private long filePointer = 0;
    
    public FileTailer(LogSourceConfig config, IndexManager indexManager) {
        this.config = config;
        this.indexManager = indexManager;
        this.parser = ParserFactory.createParser(config.getParserType(), config.getParserConfig());
        
        // Start from end of file if it exists
        Path path = Paths.get(config.getFilePath());
        if (Files.exists(path)) {
            try {
                this.filePointer = Files.size(path);
                log.info("Starting tailer for {} at position {}", config.getFilePath(), filePointer);
            } catch (IOException e) {
                log.warn("Could not determine file size for {}", config.getFilePath(), e);
            }
        }
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
            return;
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
    }
    
    public boolean isRunning() {
        return running;
    }
}
