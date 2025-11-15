package com.locallogsearch.service.model;

/**
 * Represents the persistent state of a file tailer.
 */
public class TailerState {
    private String filePath;
    private long filePointer;
    private long lastModifiedTime;
    private long fileSize;
    private String fileKey;  // Unique identifier for the file (inode-like)
    private long lastCheckpointTime;
    
    public TailerState() {
    }
    
    public TailerState(String filePath, long filePointer, long lastModifiedTime, 
                       long fileSize, String fileKey) {
        this.filePath = filePath;
        this.filePointer = filePointer;
        this.lastModifiedTime = lastModifiedTime;
        this.fileSize = fileSize;
        this.fileKey = fileKey;
        this.lastCheckpointTime = System.currentTimeMillis();
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public long getFilePointer() {
        return filePointer;
    }
    
    public void setFilePointer(long filePointer) {
        this.filePointer = filePointer;
    }
    
    public long getLastModifiedTime() {
        return lastModifiedTime;
    }
    
    public void setLastModifiedTime(long lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getFileKey() {
        return fileKey;
    }
    
    public void setFileKey(String fileKey) {
        this.fileKey = fileKey;
    }
    
    public long getLastCheckpointTime() {
        return lastCheckpointTime;
    }
    
    public void setLastCheckpointTime(long lastCheckpointTime) {
        this.lastCheckpointTime = lastCheckpointTime;
    }
    
    @Override
    public String toString() {
        return "TailerState{" +
                "filePath='" + filePath + '\'' +
                ", filePointer=" + filePointer +
                ", lastModifiedTime=" + lastModifiedTime +
                ", fileSize=" + fileSize +
                ", fileKey='" + fileKey + '\'' +
                ", lastCheckpointTime=" + lastCheckpointTime +
                '}';
    }
}
