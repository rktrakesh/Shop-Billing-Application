package com.shopbilling.service;

import com.shopbilling.dto.request.MonthlyProfitRequest;
import com.shopbilling.dto.response.MonthlyProfitResponse;

import java.util.List;

public interface MonthlyProfitService {
    MonthlyProfitResponse createOrUpdateProfit(MonthlyProfitRequest request);
    MonthlyProfitResponse getProfitByMonthYear(int month, int year);
    List<MonthlyProfitResponse> getProfitByYear(int year);
    List<MonthlyProfitResponse> getAllProfits();
}
