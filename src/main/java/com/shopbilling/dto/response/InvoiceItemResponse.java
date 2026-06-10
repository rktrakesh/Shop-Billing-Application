package com.shopbilling.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class InvoiceItemResponse {
    private Long id;
    private Long productVariantId;
    private String barcode;
    private String designName;
    private String productCode;
    private String color;
    private String size;
    private String printType;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal discountAmount;
    private BigDecimal lineTotal;
}
