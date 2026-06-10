package com.shopbilling.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MonthlyProfitResponse {
    private Long id;
    private Integer month;
    private Integer year;
    private String monthName;
    private BigDecimal totalSales;
    private BigDecimal productionCost;
    private BigDecimal otherExpenses;
    private BigDecimal netProfit;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
