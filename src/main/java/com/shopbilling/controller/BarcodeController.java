package com.shopbilling.controller;

import com.shopbilling.dto.response.ApiResponse;
import com.shopbilling.dto.response.ProductVariantResponse;
import com.shopbilling.service.BarcodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/barcodes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@Tag(name = "Barcode Management")
@SecurityRequirement(name = "bearerAuth")
public class BarcodeController {

    private final BarcodeService barcodeService;

    @PostMapping("/generate/{variantId}")
    @Operation(summary = "Generate barcode for a product variant")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> generateBarcode(@PathVariable Long variantId) {
        return ResponseEntity.ok(ApiResponse.success("Barcode generated", barcodeService.generateBarcode(variantId)));
    }

    @PostMapping("/regenerate/{variantId}")
    @Operation(summary = "Regenerate barcode for a product variant")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> regenerateBarcode(@PathVariable Long variantId) {
        return ResponseEntity.ok(ApiResponse.success("Barcode regenerated", barcodeService.regenerateBarcode(variantId)));
    }

    @GetMapping("/download/png/{variantId}")
    @Operation(summary = "Download barcode as PNG image")
    public ResponseEntity<byte[]> downloadBarcodePng(@PathVariable Long variantId) {
        byte[] image = barcodeService.downloadBarcodePng(variantId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=barcode_" + variantId + ".png")
                .contentType(MediaType.IMAGE_PNG)
                .body(image);
    }

    @GetMapping("/download/pdf/{variantId}")
    @Operation(summary = "Download barcode as printable PDF sticker")
    public ResponseEntity<byte[]> downloadBarcodePdf(@PathVariable Long variantId) {
        byte[] pdf = barcodeService.downloadBarcodePdf(variantId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=barcode_" + variantId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/search/{barcode}")
    @Operation(summary = "Search product variant by barcode")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> searchByBarcode(@PathVariable String barcode) {
        return ResponseEntity.ok(ApiResponse.success(barcodeService.searchByBarcode(barcode)));
    }
}
