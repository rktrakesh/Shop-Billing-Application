package com.shopbilling.repository;

import com.shopbilling.entity.ReportHistory;
import com.shopbilling.enums.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportHistoryRepository extends JpaRepository<ReportHistory, Long> {
    List<ReportHistory> findByReportTypeOrderByGeneratedAtDesc(ReportType reportType);
    List<ReportHistory> findAllByOrderByGeneratedAtDesc();
    List<ReportHistory> findByGeneratedByIdOrderByGeneratedAtDesc(Long userId);
}
