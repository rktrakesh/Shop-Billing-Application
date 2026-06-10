package com.shopbilling.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductResponse {
    private Long id;
    private String designName;
    private String description;
    private String category;
    private String printType;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ProductVariantResponse> variants;
}
