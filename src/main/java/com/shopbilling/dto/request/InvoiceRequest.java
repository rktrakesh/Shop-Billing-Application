package com.shopbilling.dto.request;

import com.shopbilling.enums.PaymentMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class InvoiceRequest {
    private Long customerId;
    private String customerName;
    private String customerMobile;

    @NotEmpty(message = "Invoice must have at least one item")
    @Valid
    private List<InvoiceItemRequest> items;

    private BigDecimal discountAmount = BigDecimal.ZERO;
    private BigDecimal taxAmount = BigDecimal.ZERO;
    private String notes;

    // Payment mode — defaults to CASH if not provided
    private PaymentMode paymentMode = PaymentMode.CASH;

    // If customer pays less than the total, the difference becomes a credit.
    // null means full payment received.
    private BigDecimal amountPaid;
}