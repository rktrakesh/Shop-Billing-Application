package com.shopbilling.repository;

import com.shopbilling.entity.AuditLog;
import com.shopbilling.enums.AuditAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByPerformedByOrderByCreatedAtDesc(String username);
    List<AuditLog> findByActionOrderByCreatedAtDesc(AuditAction action);
    List<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);
}
