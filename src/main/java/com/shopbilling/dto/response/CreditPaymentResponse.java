package com.shopbilling.dto.response;

import com.shopbilling.enums.PaymentMode;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreditPaymentResponse {
    private Long id;
    private BigDecimal amount;
    private PaymentMode paymentMode;
    private String notes;
    private String recordedByUsername;
    private LocalDateTime createdAt;
}