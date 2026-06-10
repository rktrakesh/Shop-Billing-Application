package com.shopbilling.controller;

import com.shopbilling.dto.response.ApiResponse;
import com.shopbilling.service.BackupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/backup")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Database Backup")
@SecurityRequirement(name = "bearerAuth")
public class BackupController {

    private final BackupService backupService;

    @PostMapping("/create")
    @Operation(summary = "Create and download a database backup (Admin only)")
    public ResponseEntity<byte[]> createBackup() {
        byte[] backup = backupService.createBackup();
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=backup_" + ts + ".sql")
                .contentType(MediaType.TEXT_PLAIN)
                .body(backup);
    }

    @GetMapping("/status")
    @Operation(summary = "Get backup directory status")
    public ResponseEntity<ApiResponse<String>> getStatus() {
        return ResponseEntity.ok(ApiResponse.success(backupService.getBackupStatus()));
    }
}
