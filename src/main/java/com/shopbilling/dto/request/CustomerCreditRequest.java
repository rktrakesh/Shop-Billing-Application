package com.shopbilling.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CustomerCreditRequest {

    @NotNull(message = "Invoice ID is required")
    private Long invoiceId;

    // Amount the customer paid right now at billing time.
    // Outstanding = invoiceTotal - amountPaid
    @NotNull(message = "Amount paid is required")
    @DecimalMin(value = "0.00", message = "Amount paid cannot be negative")
    private BigDecimal amountPaid;

    private String notes;
}