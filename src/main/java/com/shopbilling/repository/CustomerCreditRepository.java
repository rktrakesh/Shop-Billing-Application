package com.shopbilling.repository;

import com.shopbilling.entity.CustomerCredit;
import com.shopbilling.enums.CreditStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerCreditRepository extends JpaRepository<CustomerCredit, Long> {

    List<CustomerCredit> findAllByOrderByCreatedAtDesc();

    List<CustomerCredit> findByStatusInOrderByCreatedAtDesc(List<CreditStatus> statuses);

    List<CustomerCredit> findByCustomer_IdOrderByCreatedAtDesc(Long customerId);

    Optional<CustomerCredit> findByInvoice_Id(Long invoiceId);

    // For the warning popup: does this customer have any pending/partial credits?
    @Query("SELECT COUNT(c) FROM CustomerCredit c WHERE c.customer.id = :customerId AND c.status IN ('PENDING', 'PARTIAL')")
    long countPendingByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT COALESCE(SUM(c.outstandingAmount), 0) FROM CustomerCredit c WHERE c.customer.id = :customerId AND c.status IN ('PENDING', 'PARTIAL')")
    BigDecimal sumOutstandingByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT COALESCE(SUM(c.outstandingAmount), 0) FROM CustomerCredit c WHERE c.status IN ('PENDING', 'PARTIAL')")
    BigDecimal sumTotalOutstanding();

    long countByStatusIn(List<CreditStatus> statuses);
}