package com.locallogsearch.service.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Metadata about an exported table
 */
@Entity
@Table(name = "exported_tables")
public class ExportedTable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String tableName;
    
    @Column(length = 1000)
    private String description;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    @Column(nullable = false)
    private Long rowCount;
    
    @Column(length = 2000)
    private String sourceQuery;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "table_columns", joinColumns = @JoinColumn(name = "table_id"))
    @Column(name = "column_name")
    @OrderColumn(name = "column_order")
    private List<String> columns = new ArrayList<>();
    
    public ExportedTable() {
        this.createdAt = Instant.now();
        this.rowCount = 0L;
    }
    
    public ExportedTable(String tableName, List<String> columns) {
        this();
        this.tableName = tableName;
        this.columns = new ArrayList<>(columns);
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
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Long getRowCount() {
        return rowCount;
    }
    
    public void setRowCount(Long rowCount) {
        this.rowCount = rowCount;
    }
    
    public String getSourceQuery() {
        return sourceQuery;
    }
    
    public void setSourceQuery(String sourceQuery) {
        this.sourceQuery = sourceQuery;
    }
    
    public List<String> getColumns() {
        return columns;
    }
    
    public void setColumns(List<String> columns) {
        this.columns = columns;
    }
}
