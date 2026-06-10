package com.shopbilling.dto.request;

import com.shopbilling.enums.StockChangeType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StockAdjustmentRequest {
    @NotNull(message = "Product variant ID is required")
    private Long productVariantId;
    
    @NotNull(message = "Change type is required")
    private StockChangeType changeType;
    
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
    
    private String reason;
}
