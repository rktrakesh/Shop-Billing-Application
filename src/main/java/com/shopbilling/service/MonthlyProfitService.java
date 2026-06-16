package com.shopbilling.service;

import com.shopbilling.dto.request.MonthlyProfitRequest;
import com.shopbilling.dto.response.MonthlyProfitResponse;
import com.shopbilling.dto.response.ProfitSummaryResponse;

import java.time.LocalDate;
import java.util.List;

public interface MonthlyProfitService {
    MonthlyProfitResponse createOrUpdateProfit(MonthlyProfitRequest request);
    MonthlyProfitResponse getProfitByMonthYear(int month, int year);
    List<MonthlyProfitResponse> getProfitByYear(int year);
    List<MonthlyProfitResponse> getAllProfits();
    ProfitSummaryResponse getDailyProfit(LocalDate date);
    ProfitSummaryResponse getMonthlyProfit(int year, int month);
    ProfitSummaryResponse getYearlyProfit(int year);
    byte[] generateProfitPdf(ProfitSummaryResponse summary);
    List<ProfitSummaryResponse> getLastNMonthsSummary(int months);
}
