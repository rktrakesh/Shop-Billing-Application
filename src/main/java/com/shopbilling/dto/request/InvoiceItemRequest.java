package com.shopbilling.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InvoiceItemRequest {
    // For barcode mode
    private String barcode;
    
    // For manual mode or overrides
    @NotBlank(message = "Design name is required")
    private String designName;
    
    private String productCode;
    private String color;
    private String size;
    private String printType;
    
    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.01", message = "Unit price must be greater than 0")
    private BigDecimal unitPrice;
    
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
    
    private BigDecimal discountAmount = BigDecimal.ZERO;
}
