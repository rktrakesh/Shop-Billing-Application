package com.shopbilling.service;

import com.shopbilling.dto.request.CreditPaymentRequest;
import com.shopbilling.dto.request.CustomerCreditRequest;
import com.shopbilling.dto.response.CustomerCreditResponse;

import java.math.BigDecimal;
import java.util.List;

public interface CustomerCreditService {

    // Create a credit when an invoice is partially paid
    CustomerCreditResponse createCredit(CustomerCreditRequest request);

    // Record a repayment (partial or full) against an existing credit
    CustomerCreditResponse recordPayment(Long creditId, CreditPaymentRequest request);

    // Get all credits (pending + partial + cleared)
    List<CustomerCreditResponse> getAllCredits();

    // Get only pending/partial (outstanding) credits
    List<CustomerCreditResponse> getPendingCredits();

    // Get all credits for a specific customer
    List<CustomerCreditResponse> getCreditsByCustomer(Long customerId);

    // Get credit for a specific invoice (if any)
    CustomerCreditResponse getCreditByInvoice(Long invoiceId);

    // Summary counts/amounts for dashboard
    long countPendingCredits();
    BigDecimal totalOutstandingAmount();

    // Check if a customer has outstanding credits (for the warning popup)
    boolean customerHasOutstandingCredit(Long customerId);
    BigDecimal getCustomerOutstandingAmount(Long customerId);
}