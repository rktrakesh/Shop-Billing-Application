package com.shopbilling.controller;

import com.shopbilling.dto.response.ApiResponse;
import com.shopbilling.dto.response.AuditLogResponse;
import com.shopbilling.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Audit Logs")
@SecurityRequirement(name = "bearerAuth")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @Operation(summary = "Get all audit logs")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getAllLogs() {
        return ResponseEntity.ok(ApiResponse.success(auditLogService.getAllLogs()));
    }

    @GetMapping("/recent")
    @Operation(summary = "Get the most recent N audit log entries")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getRecentLogs(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(ApiResponse.success(auditLogService.getRecentLogs(limit)));
    }
}
