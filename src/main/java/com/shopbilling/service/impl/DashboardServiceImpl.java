package com.shopbilling.service.impl;

import com.shopbilling.dto.response.DashboardResponse;
import com.shopbilling.entity.User;
import com.shopbilling.mapper.InvoiceMapper;
import com.shopbilling.mapper.ProductVariantMapper;
import com.shopbilling.repository.*;
import com.shopbilling.service.DashboardService;
import com.shopbilling.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final MonthlyProfitRepository profitRepository;
    private final InvoiceMapper invoiceMapper;
    private final ProductVariantMapper variantMapper;
    private final SecurityUtils securityUtils;

    @Override
    public DashboardResponse getAdminDashboard() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime weekStart = now.toLocalDate().with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime monthStart = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
        LocalDateTime yearStart = now.toLocalDate().withDayOfYear(1).atStartOfDay();

        BigDecimal todaySales = invoiceRepository.sumGrandTotalByDateRange(todayStart, now);
        BigDecimal weeklySales = invoiceRepository.sumGrandTotalByDateRange(weekStart, now);
        BigDecimal monthlySales = invoiceRepository.sumGrandTotalByDateRange(monthStart, now);
        BigDecimal yearlySales = invoiceRepository.sumGrandTotalByDateRange(yearStart, now);

        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();
        BigDecimal monthlyProfit = profitRepository.findByMonthAndYear(currentMonth, currentYear)
                .map(p -> p.getNetProfit()).orElse(BigDecimal.ZERO);
        BigDecimal yearlyProfit = profitRepository.sumNetProfitByYear(currentYear);

        return DashboardResponse.builder()
                .todaySales(todaySales)
                .weeklySales(weeklySales)
                .monthlySales(monthlySales)
                .yearlySales(yearlySales)
                .monthlyProfit(monthlyProfit)
                .yearlyProfit(yearlyProfit)
                .totalCustomers(customerRepository.count())
                .totalProducts(productRepository.count())
                .lowStockCount(variantRepository.countLowStockVariants())
                .todayInvoiceCount(invoiceRepository.countByDateRange(todayStart, now))
                .monthlyInvoiceCount(invoiceRepository.countByDateRange(monthStart, now))
                .recentInvoices(invoiceMapper.toResponseList(
                        invoiceRepository.findRecentInvoices(PageRequest.of(0, 10))))
                .lowStockProducts(variantMapper.toResponseList(variantRepository.findLowStockVariants()))
                .build();
    }

    @Override
    public DashboardResponse getManagerDashboard() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
        LocalDateTime yearStart = now.toLocalDate().withDayOfYear(1).atStartOfDay();

        BigDecimal monthlySales = invoiceRepository.sumGrandTotalByDateRange(monthStart, now);
        BigDecimal yearlySales = invoiceRepository.sumGrandTotalByDateRange(yearStart, now);

        return DashboardResponse.builder()
                .monthlySales(monthlySales)
                .yearlySales(yearlySales)
                .lowStockCount(variantRepository.countLowStockVariants())
                .monthlyInvoiceCount(invoiceRepository.countByDateRange(monthStart, now))
                .lowStockProducts(variantMapper.toResponseList(variantRepository.findLowStockVariants()))
                .recentInvoices(invoiceMapper.toResponseList(
                        invoiceRepository.findRecentInvoices(PageRequest.of(0, 10))))
                .build();
    }

    @Override
    public DashboardResponse getUserDashboard() {
        User currentUser = securityUtils.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();

        long todayInvoices = invoiceRepository.countByCreatedByAndDateRange(currentUser, todayStart, now);

        return DashboardResponse.builder()
                .todayInvoiceCount(todayInvoices)
                .build();
    }
}