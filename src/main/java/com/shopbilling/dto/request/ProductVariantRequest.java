package com.shopbilling.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductVariantRequest {
    @NotNull(message = "Product ID is required")
    private Long productId;
    
    @NotBlank(message = "Product code is required")
    private String productCode;
    
    @NotBlank(message = "Color is required")
    private String color;
    
    @NotBlank(message = "Size is required")
    private String size;
    
    @NotNull(message = "Selling price is required")
    @DecimalMin(value = "0.01", message = "Selling price must be greater than 0")
    private BigDecimal sellingPrice;
    
    @NotNull(message = "Cost price is required")
    @DecimalMin(value = "0.01", message = "Cost price must be greater than 0")
    private BigDecimal costPrice;
    
    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stock = 0;
    
    @Min(value = 0, message = "Minimum stock cannot be negative")
    private Integer minimumStock = 5;
    
    private String imageUrl;
}
