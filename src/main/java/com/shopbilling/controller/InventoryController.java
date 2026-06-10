package com.shopbilling.controller;

import com.shopbilling.dto.request.StockAdjustmentRequest;
import com.shopbilling.dto.response.ApiResponse;
import com.shopbilling.dto.response.ProductVariantResponse;
import com.shopbilling.dto.response.StockMovementResponse;
import com.shopbilling.service.InventoryService;
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
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@Tag(name = "Inventory Management")
@SecurityRequirement(name = "bearerAuth")
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/adjust")
    @Operation(summary = "Adjust stock (add, reduce, return, manual adjustment)")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> adjustStock(@Valid @RequestBody StockAdjustmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Stock adjusted", inventoryService.adjustStock(request)));
    }

    @GetMapping("/movements/{variantId}")
    @Operation(summary = "Get stock movement history for a variant")
    public ResponseEntity<ApiResponse<List<StockMovementResponse>>> getStockHistory(@PathVariable Long variantId) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getStockHistory(variantId)));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Get all low-stock products")
    public ResponseEntity<ApiResponse<List<ProductVariantResponse>>> getLowStockProducts() {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getLowStockProducts()));
    }
}
