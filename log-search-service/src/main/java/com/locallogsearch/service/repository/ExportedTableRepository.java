package com.locallogsearch.service.repository;

import com.locallogsearch.service.entity.ExportedTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExportedTableRepository extends JpaRepository<ExportedTable, Long> {
    Optional<ExportedTable> findByTableName(String tableName);
    boolean existsByTableName(String tableName);
    void deleteByTableName(String tableName);
}
