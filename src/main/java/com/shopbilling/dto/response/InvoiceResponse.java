package com.shopbilling.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class InvoiceResponse {
    private Long id;
    private String invoiceNumber;
    private LocalDateTime invoiceDate;
    private Long customerId;
    private String customerName;
    private String customerMobile;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal grandTotal;
    private String createdByUsername;
    private String notes;
    private List<InvoiceItemResponse> items;
    private LocalDateTime createdAt;
}
