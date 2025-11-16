package com.locallogsearch.service.repository;

import com.locallogsearch.service.entity.ExportedRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExportedRowRepository extends JpaRepository<ExportedRow, Long> {
    List<ExportedRow> findByTableName(String tableName);
    Page<ExportedRow> findByTableName(String tableName, Pageable pageable);
    long countByTableName(String tableName);
    void deleteByTableName(String tableName);
}
