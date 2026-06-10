package com.shopbilling.controller;

import com.shopbilling.dto.response.ApiResponse;
import com.shopbilling.dto.response.DashboardResponse;
import com.shopbilling.enums.Role;
import com.shopbilling.service.DashboardService;
import com.shopbilling.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @Operation(summary = "Get role-based dashboard data")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard() {
        DashboardResponse response;
        if (securityUtils.isAdmin()) {
            response = dashboardService.getAdminDashboard();
        } else if (securityUtils.isManager()) {
            response = dashboardService.getManagerDashboard();
        } else {
            response = dashboardService.getUserDashboard();
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
