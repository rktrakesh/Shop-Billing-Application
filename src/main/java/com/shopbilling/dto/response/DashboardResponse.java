package com.shopbilling.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardResponse {
    private BigDecimal todaySales;
    private BigDecimal weeklySales;
    private BigDecimal monthlySales;
    private BigDecimal yearlySales;
    private BigDecimal monthlyProfit;
    private BigDecimal yearlyProfit;
    private Long totalCustomers;
    private Long totalProducts;
    private Long lowStockCount;
    private Long todayInvoiceCount;
    private Long monthlyInvoiceCount;
    private Long pendingCreditCount;
    private BigDecimal totalOutstandingCredit;
    private List<InvoiceResponse> recentInvoices;
    private List<ProductVariantResponse> lowStockProducts;
}