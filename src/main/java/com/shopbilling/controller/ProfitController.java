package com.shopbilling.controller;

import com.shopbilling.dto.request.MonthlyProfitRequest;
import com.shopbilling.dto.response.ApiResponse;
import com.shopbilling.dto.response.MonthlyProfitResponse;
import com.shopbilling.dto.response.ProfitSummaryResponse;
import com.shopbilling.service.MonthlyProfitService;
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
import java.util.List;

@RestController
@RequestMapping("/api/profit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Profit Management")
@SecurityRequirement(name = "bearerAuth")
public class ProfitController {

    private final MonthlyProfitService profitService;

    @PostMapping
    @Operation(summary = "Create or update a monthly profit entry")
    public ResponseEntity<ApiResponse<MonthlyProfitResponse>> createOrUpdate(@Valid @RequestBody MonthlyProfitRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Profit entry saved", profitService.createOrUpdateProfit(request)));
    }

    @GetMapping("/{year}/{month}")
    @Operation(summary = "Get profit for a specific month/year")
    public ResponseEntity<ApiResponse<MonthlyProfitResponse>> getByMonthYear(
            @PathVariable int year, @PathVariable int month) {
        return ResponseEntity.ok(ApiResponse.success(profitService.getProfitByMonthYear(month, year)));
    }

    @GetMapping("/year/{year}")
    @Operation(summary = "Get all monthly profits for a year")
    public ResponseEntity<ApiResponse<List<MonthlyProfitResponse>>> getByYear(@PathVariable int year) {
        return ResponseEntity.ok(ApiResponse.success(profitService.getProfitByYear(year)));
    }

    @GetMapping
    @Operation(summary = "Get all profit entries")
    public ResponseEntity<ApiResponse<List<MonthlyProfitResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(profitService.getAllProfits()));
    }

    @GetMapping("/summary/daily")
    @Operation(summary = "Get profit for a specific date")
    public ResponseEntity<ApiResponse<ProfitSummaryResponse>> dailySummary(
            @RequestParam(required = false) String date) {
        LocalDate d = date != null ? LocalDate.parse(date) : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.success(profitService.getDailyProfit(d)));
    }

    @GetMapping("/summary/monthly")
    @Operation(summary = "Get monthly profit for a specific year and month")
    public ResponseEntity<ApiResponse<ProfitSummaryResponse>> monthlySummary(
            @RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(ApiResponse.success(profitService.getMonthlyProfit(year, month)));
    }

    @GetMapping("/summary/yearly")
    @Operation(summary = "Get yearly profit for a specific year")
    public ResponseEntity<ApiResponse<ProfitSummaryResponse>> yearlySummary(
            @RequestParam int year) {
        return ResponseEntity.ok(ApiResponse.success(profitService.getYearlyProfit(year)));
    }

    @GetMapping("/summary/download")
    @Operation(summary = "Download profit summary as PDF for a specified period")
    public ResponseEntity<byte[]> downloadSummary(
            @RequestParam String period,  // "daily" | "monthly" | "yearly"
            @RequestParam(required = false) String date,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        ProfitSummaryResponse summary = switch (period) {
            case "monthly" -> profitService.getMonthlyProfit(year, month);
            case "yearly" -> profitService.getYearlyProfit(year);
            default -> profitService.getDailyProfit(date != null ? LocalDate.parse(date) : LocalDate.now());
        };
        byte[] pdf = profitService.generateProfitPdf(summary);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=profit_" + period + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

}
