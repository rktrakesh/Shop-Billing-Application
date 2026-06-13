package com.shopbilling.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ItemReturnRequest {

    @NotNull
    private Long invoiceId;

    @NotNull
    private Long invoiceItemId;

    @NotNull
    @Min(1)
    private Integer quantity;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal refundAmount;

    private String reason;
}