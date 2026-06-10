package com.shopbilling.service;

import com.shopbilling.dto.request.ReportRequest;
import com.shopbilling.dto.response.ReportHistoryResponse;

import java.util.List;

public interface ReportService {
    byte[] generateDailyReport();
    byte[] generateMonthlyReport();
    byte[] generateYearlyReport();
    byte[] generateCustomReport(ReportRequest request);
    List<ReportHistoryResponse> getReportHistory();
    byte[] downloadReport(Long reportId);
    void deleteReport(Long reportId);
}
