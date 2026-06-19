package com.shopbilling.dto.response;

import com.shopbilling.enums.CreditStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CustomerCreditResponse {
    private Long id;
    private Long invoiceId;
    private String invoiceNumber;
    private Long customerId;
    private String customerName;
    private String customerMobile;
    private BigDecimal totalAmount;
    private BigDecimal amountPaid;
    private BigDecimal outstandingAmount;
    private CreditStatus status;
    private String notes;
    private String createdByUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CreditPaymentResponse> payments;
}