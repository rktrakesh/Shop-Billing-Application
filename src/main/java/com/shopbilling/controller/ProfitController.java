package com.shopbilling.controller;

import com.shopbilling.dto.request.MonthlyProfitRequest;
import com.shopbilling.dto.response.ApiResponse;
import com.shopbilling.dto.response.MonthlyProfitResponse;
import com.shopbilling.service.MonthlyProfitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
}
