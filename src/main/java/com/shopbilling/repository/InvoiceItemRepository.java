package com.shopbilling.repository;

import com.shopbilling.entity.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {
    
    @Query("SELECT ii.designName, SUM(ii.quantity) as totalQty, SUM(ii.lineTotal) as totalAmount " +
           "FROM InvoiceItem ii JOIN ii.invoice i " +
           "WHERE i.invoiceDate BETWEEN :start AND :end " +
           "GROUP BY ii.designName ORDER BY totalQty DESC")
    List<Object[]> findTopSellingDesigns(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    @Query("SELECT ii.color, SUM(ii.quantity) as totalQty " +
           "FROM InvoiceItem ii JOIN ii.invoice i " +
           "WHERE i.invoiceDate BETWEEN :start AND :end " +
           "GROUP BY ii.color ORDER BY totalQty DESC")
    List<Object[]> findTopSellingColors(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    @Query("SELECT ii.size, SUM(ii.quantity) as totalQty " +
           "FROM InvoiceItem ii JOIN ii.invoice i " +
           "WHERE i.invoiceDate BETWEEN :start AND :end " +
           "GROUP BY ii.size ORDER BY totalQty DESC")
    List<Object[]> findTopSellingSizes(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    @Query("SELECT ii.printType, SUM(ii.quantity) as totalQty " +
           "FROM InvoiceItem ii JOIN ii.invoice i " +
           "WHERE i.invoiceDate BETWEEN :start AND :end " +
           "GROUP BY ii.printType ORDER BY totalQty DESC")
    List<Object[]> findTopSellingPrintTypes(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
