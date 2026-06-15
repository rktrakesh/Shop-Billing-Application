package com.shopbilling.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemReturnRequest {

    @NotNull(message = "Invoice ID is required")
    private Long invoiceId;

    @NotNull(message = "Invoice item ID is required")
    private Long invoiceItemId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotNull(message = "Refund amount is required")
    @DecimalMin(value = "0.00", message = "Refund amount cannot be negative")
    private BigDecimal refundAmount;

    private String reason;
}