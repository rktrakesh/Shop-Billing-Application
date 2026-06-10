package com.shopbilling.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MonthlyProfitRequest {
    @NotNull @Min(1) @Max(12)
    private Integer month;
    
    @NotNull @Min(2000)
    private Integer year;
    
    @NotNull @DecimalMin("0.00")
    private BigDecimal totalSales;
    
    @NotNull @DecimalMin("0.00")
    private BigDecimal productionCost;
    
    @DecimalMin("0.00")
    private BigDecimal otherExpenses = BigDecimal.ZERO;
    
    private String notes;
}
