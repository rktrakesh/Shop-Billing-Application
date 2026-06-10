package com.shopbilling.dto.response;

import com.shopbilling.enums.StockChangeType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StockMovementResponse {
    private Long id;
    private Long productVariantId;
    private String productCode;
    private String designName;
    private StockChangeType changeType;
    private Integer quantity;
    private Integer stockBefore;
    private Integer stockAfter;
    private String reason;
    private String createdBy;
    private LocalDateTime createdAt;
}
