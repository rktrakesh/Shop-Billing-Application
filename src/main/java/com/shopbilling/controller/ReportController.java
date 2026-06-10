package com.shopbilling.controller;

import com.shopbilling.dto.request.ReportRequest;
import com.shopbilling.dto.response.ApiResponse;
import com.shopbilling.dto.response.ReportHistoryResponse;
import com.shopbilling.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@Tag(name = "Reports")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/daily")
    @Operation(summary = "Generate and download today's sales report")
    public ResponseEntity<byte[]> dailyReport() {
        byte[] pdf = reportService.generateDailyReport();
        return pdfResponse(pdf, "daily_report_" + LocalDate.now() + ".pdf");
    }

    @GetMapping("/monthly")
    @Operation(summary = "Generate and download this month's sales report")
    public ResponseEntity<byte[]> monthlyReport() {
        byte[] pdf = reportService.generateMonthlyReport();
        return pdfResponse(pdf, "monthly_report_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")) + ".pdf");
    }

    @GetMapping("/yearly")
    @Operation(summary = "Generate and download this year's sales report")
    public ResponseEntity<byte[]> yearlyReport() {
        byte[] pdf = reportService.generateYearlyReport();
        return pdfResponse(pdf, "yearly_report_" + LocalDate.now().getYear() + ".pdf");
    }

    @PostMapping("/custom")
    @Operation(summary = "Generate and download a custom date-range report")
    public ResponseEntity<byte[]> customReport(@Valid @RequestBody ReportRequest request) {
        byte[] pdf = reportService.generateCustomReport(request);
        return pdfResponse(pdf, "custom_report_" + request.getStartDate() + "_" + request.getEndDate() + ".pdf");
    }

    @GetMapping("/history")
    @Operation(summary = "View report generation history")
    public ResponseEntity<ApiResponse<List<ReportHistoryResponse>>> getHistory() {
        return ResponseEntity.ok(ApiResponse.success(reportService.getReportHistory()));
    }

    @GetMapping("/history/{id}/download")
    @Operation(summary = "Re-download a previously generated report")
    public ResponseEntity<byte[]> downloadOldReport(@PathVariable Long id) {
        byte[] pdf = reportService.downloadReport(id);
        return pdfResponse(pdf, "report_" + id + ".pdf");
    }

    @DeleteMapping("/history/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a report from history (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteReport(@PathVariable Long id) {
        reportService.deleteReport(id);
        return ResponseEntity.ok(ApiResponse.success("Report deleted", null));
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String fileName) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
