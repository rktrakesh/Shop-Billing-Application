package com.shopbilling.controller;

import com.shopbilling.dto.request.ShopSettingsRequest;
import com.shopbilling.dto.response.ApiResponse;
import com.shopbilling.dto.response.ShopSettingsResponse;
import com.shopbilling.service.ShopSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
@Tag(name = "Shop Settings")
@SecurityRequirement(name = "bearerAuth")
public class ShopSettingsController {

    private final ShopSettingsService settingsService;

    @GetMapping
    @Operation(summary = "Get current shop settings")
    public ResponseEntity<ApiResponse<ShopSettingsResponse>> getSettings() {
        return ResponseEntity.ok(ApiResponse.success(settingsService.getSettings()));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update shop settings (Admin only)")
    public ResponseEntity<ApiResponse<ShopSettingsResponse>> updateSettings(@Valid @RequestBody ShopSettingsRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Settings updated", settingsService.updateSettings(request)));
    }
}
