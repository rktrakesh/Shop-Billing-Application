package com.shopbilling.repository;

import com.shopbilling.entity.Invoice;
import com.shopbilling.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByCreatedByOrderByCreatedAtDesc(User user);
    List<Invoice> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.createdBy = :user AND i.invoiceDate BETWEEN :start AND :end")
    long countByCreatedByAndDateRange(@Param("user") User user, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Used to find a walk-in customer's purchase history when no Customer
    // record exists, by matching the mobile number stored directly on the invoice.
    List<Invoice> findAllByCustomerMobileOrderByInvoiceDateDesc(String customerMobile);

    @Query("SELECT i FROM Invoice i WHERE i.invoiceDate BETWEEN :start AND :end ORDER BY i.invoiceDate DESC")
    List<Invoice> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(i.grandTotal), 0) FROM Invoice i WHERE i.invoiceDate BETWEEN :start AND :end")
    BigDecimal sumGrandTotalByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.invoiceDate BETWEEN :start AND :end")
    long countByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT i FROM Invoice i ORDER BY i.createdAt DESC")
    List<Invoice> findRecentInvoices(org.springframework.data.domain.Pageable pageable);

    @Query("SELECT COALESCE(SUM(i.discountAmount), 0) FROM Invoice i WHERE i.invoiceDate BETWEEN :start AND :end")
    BigDecimal sumDiscountByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    String findTopByOrderByInvoiceNumberDesc();

    @Query("SELECT i.invoiceNumber FROM Invoice i ORDER BY i.id DESC")
    List<String> findLastInvoiceNumber(org.springframework.data.domain.Pageable pageable);
}