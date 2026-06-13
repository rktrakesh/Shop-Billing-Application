package com.shopbilling.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ItemReturnResponse {
    private Long id;
    private Long invoiceId;
    private String invoiceNumber;
    private Long invoiceItemId;
    private String designName;
    private String color;
    private String size;
    private String productCode;
    private Integer quantity;
    private BigDecimal refundAmount;
    private String reason;
    private String returnedByUsername;
    private LocalDateTime createdAt;
}