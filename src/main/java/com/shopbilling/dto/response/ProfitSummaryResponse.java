package com.shopbilling.dto.response;

import lombok.*;
import lombok.experimental.SuperBuilder;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class ProfitSummaryResponse {
    private String periodLabel;     // e.g. "12 Jun 2026", "June 2026", "2026"
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalSales;      // sum of grandTotal
    private BigDecimal productionCost;  // sum(qty * costPrice)
    private BigDecimal netProfit;       // totalSales - productionCost
    private BigDecimal marginPercent;   // netProfit / totalSales * 100
    private long invoiceCount;
}