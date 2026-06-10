package com.shopbilling.repository;

import com.shopbilling.entity.MonthlyProfit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface MonthlyProfitRepository extends JpaRepository<MonthlyProfit, Long> {
    Optional<MonthlyProfit> findByMonthAndYear(int month, int year);
    List<MonthlyProfit> findByYearOrderByMonthAsc(int year);
    
    @Query("SELECT COALESCE(SUM(mp.netProfit), 0) FROM MonthlyProfit mp WHERE mp.year = :year")
    BigDecimal sumNetProfitByYear(@Param("year") int year);
    
    @Query("SELECT COALESCE(SUM(mp.totalSales), 0) FROM MonthlyProfit mp WHERE mp.year = :year")
    BigDecimal sumTotalSalesByYear(@Param("year") int year);
}
