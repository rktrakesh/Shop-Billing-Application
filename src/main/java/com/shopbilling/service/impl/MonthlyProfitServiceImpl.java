package com.shopbilling.service.impl;

import com.shopbilling.dto.request.MonthlyProfitRequest;
import com.shopbilling.dto.response.MonthlyProfitResponse;
import com.shopbilling.entity.MonthlyProfit;
import com.shopbilling.mapper.MonthlyProfitMapper;
import com.shopbilling.repository.MonthlyProfitRepository;
import com.shopbilling.service.MonthlyProfitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MonthlyProfitServiceImpl implements MonthlyProfitService {

    private final MonthlyProfitRepository profitRepository;
    private final MonthlyProfitMapper profitMapper;

    @Override
    public MonthlyProfitResponse createOrUpdateProfit(MonthlyProfitRequest request) {
        MonthlyProfit profit = profitRepository.findByMonthAndYear(request.getMonth(), request.getYear())
                .orElse(new MonthlyProfit());

        profit.setMonth(request.getMonth());
        profit.setYear(request.getYear());
        profit.setTotalSales(request.getTotalSales());
        profit.setProductionCost(request.getProductionCost());
        BigDecimal otherExpenses = request.getOtherExpenses() != null ? request.getOtherExpenses() : BigDecimal.ZERO;
        profit.setOtherExpenses(otherExpenses);
        profit.setNetProfit(request.getTotalSales().subtract(request.getProductionCost()).subtract(otherExpenses));
        profit.setNotes(request.getNotes());

        MonthlyProfit saved = profitRepository.save(profit);
        log.info("Monthly profit saved for {}/{}: net profit = {}", request.getMonth(), request.getYear(), saved.getNetProfit());
        return profitMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public MonthlyProfitResponse getProfitByMonthYear(int month, int year) {
        return profitRepository.findByMonthAndYear(month, year)
                .map(profitMapper::toResponse)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonthlyProfitResponse> getProfitByYear(int year) {
        return profitMapper.toResponseList(profitRepository.findByYearOrderByMonthAsc(year));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonthlyProfitResponse> getAllProfits() {
        return profitMapper.toResponseList(profitRepository.findAll());
    }
}
