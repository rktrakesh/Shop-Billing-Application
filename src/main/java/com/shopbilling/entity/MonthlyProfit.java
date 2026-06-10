package com.shopbilling.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "monthly_profits", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"month", "year"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class MonthlyProfit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "month", nullable = false)
    private Integer month;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "total_sales", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalSales;

    @Column(name = "production_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal productionCost;

    @Column(name = "other_expenses", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal otherExpenses = BigDecimal.ZERO;

    @Column(name = "net_profit", nullable = false, precision = 12, scale = 2)
    private BigDecimal netProfit;

    @Column(name = "notes", length = 500)
    private String notes;
}
