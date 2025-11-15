package com.locallogsearch.core.tailer;

/**
 * Represents the runtime state of a file tailer (used for checkpointing).
 */
public class FileTailerState {
    private final String filePath;
    private final long filePointer;
    private final long lastModifiedTime;
    private final long fileSize;
    private final String fileKey;
    
    public FileTailerState(String filePath, long filePointer, long lastModifiedTime,
                           long fileSize, String fileKey) {
        this.filePath = filePath;
        this.filePointer = filePointer;
        this.lastModifiedTime = lastModifiedTime;
        this.fileSize = fileSize;
        this.fileKey = fileKey;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public long getFilePointer() {
        return filePointer;
    }
    
    public long getLastModifiedTime() {
        return lastModifiedTime;
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    public String getFileKey() {
        return fileKey;
    }
}
