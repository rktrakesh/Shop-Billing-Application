package com.shopbilling.repository;

import com.shopbilling.entity.ItemReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface ItemReturnRepository extends JpaRepository<ItemReturn, Long> {

    List<ItemReturn> findAllByInvoice_IdOrderByCreatedAtDesc(Long invoiceId);

    List<ItemReturn> findAllByOrderByCreatedAtDesc();

    @Query("SELECT COALESCE(SUM(r.quantity), 0) FROM ItemReturn r WHERE r.invoiceItem.id = :invoiceItemId")
    Integer sumReturnedQuantityForInvoiceItem(@Param("invoiceItemId") Long invoiceItemId);

    @Query("SELECT COALESCE(SUM(r.refundAmount), 0) FROM ItemReturn r WHERE r.createdAt BETWEEN :start AND :end")
    BigDecimal sumRefundsByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    List<ItemReturn> findAllByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);
}