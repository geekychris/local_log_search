package com.locallogsearch.service.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * A single row of exported data
 */
@Entity
@Table(name = "exported_rows", indexes = {
    @Index(name = "idx_table_name", columnList = "tableName")
})
public class ExportedRow {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String tableName;
    
    @Column
    private Instant timestamp;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "row_data", joinColumns = @JoinColumn(name = "row_id"))
    @MapKeyColumn(name = "field_name")
    @Column(name = "field_value", length = 4000)
    private Map<String, String> data = new HashMap<>();
    
    public ExportedRow() {
    }
    
    public ExportedRow(String tableName, Map<String, String> data, Instant timestamp) {
        this.tableName = tableName;
        this.data = new HashMap<>(data);
        this.timestamp = timestamp;
    }
    
    // Getters and Setters
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTableName() {
        return tableName;
    }
    
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    public Map<String, String> getData() {
        return data;
    }
    
    public void setData(Map<String, String> data) {
        this.data = data;
    }
}
