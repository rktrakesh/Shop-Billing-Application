package com.shopbilling.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductVariantResponse {
    private Long id;
    private Long productId;
    private String designName;
    private String productCode;
    private String color;
    private String size;
    private String barcode;
    private BigDecimal sellingPrice;
    private BigDecimal costPrice; // Only visible to ADMIN
    private Integer stock;
    private Integer minimumStock;
    private String imageUrl;
    private String barcodeImagePath;
    private boolean active;
    private boolean lowStock;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
