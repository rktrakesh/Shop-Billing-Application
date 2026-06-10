package com.shopbilling.repository;

import com.shopbilling.entity.StockMovement;
import com.shopbilling.enums.StockChangeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findByProductVariantIdOrderByCreatedAtDesc(Long variantId);
    List<StockMovement> findByChangeType(StockChangeType changeType);
    
    @Query("SELECT sm FROM StockMovement sm WHERE sm.createdAt BETWEEN :start AND :end ORDER BY sm.createdAt DESC")
    List<StockMovement> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
