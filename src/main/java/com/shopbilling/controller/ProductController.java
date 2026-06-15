package com.shopbilling.controller;

import com.shopbilling.dto.request.ProductRequest;
import com.shopbilling.dto.request.ProductVariantRequest;
import com.shopbilling.dto.response.ApiResponse;
import com.shopbilling.dto.response.ProductResponse;
import com.shopbilling.dto.response.ProductVariantResponse;
import com.shopbilling.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Product Management")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private final ProductService productService;

    // ── Products ──────────────────────────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Create product")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created", productService.createProduct(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Update product")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Product updated", productService.updateProduct(id, request)));
    }

    @GetMapping
    @Operation(summary = "Get all active products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getActiveProducts() {
        return ResponseEntity.ok(ApiResponse.success(productService.getActiveProducts()));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Get all products including inactive")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProducts() {
        return ResponseEntity.ok(ApiResponse.success(productService.getAllProducts()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductById(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Deactivate product")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deactivated", null));
    }

    // ── Variants ──────────────────────────────────────────────────────────
    @PostMapping("/variants")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Create product variant")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> createVariant(@Valid @RequestBody ProductVariantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Variant created", productService.createVariant(request)));
    }

    @PutMapping("/variants/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Update product variant")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> updateVariant(
            @PathVariable Long id, @Valid @RequestBody ProductVariantRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Variant updated", productService.updateVariant(id, request)));
    }

    @GetMapping("/variants/{id}")
    @Operation(summary = "Get variant by ID")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> getVariantById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getVariantById(id)));
    }

    @GetMapping("/variants/barcode/{barcode}")
    @Operation(summary = "Find product variant by barcode (used during billing)")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> getVariantByBarcode(@PathVariable String barcode) {
        return ResponseEntity.ok(ApiResponse.success(productService.getVariantByBarcode(barcode)));
    }

    @GetMapping("/{productId}/variants")
    @Operation(summary = "Get all variants for a product")
    public ResponseEntity<ApiResponse<List<ProductVariantResponse>>> getVariantsByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(productService.getVariantsByProduct(productId)));
    }

    @GetMapping("/variants")
    @Operation(summary = "Get all variants (active and inactive) across all products (avoids N+1 per-product calls)")
    public ResponseEntity<ApiResponse<List<ProductVariantResponse>>> getAllVariants() {
        return ResponseEntity.ok(ApiResponse.success(productService.getAllVariants()));
    }

    @GetMapping("/variants/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Get low-stock variants")
    public ResponseEntity<ApiResponse<List<ProductVariantResponse>>> getLowStockVariants() {
        return ResponseEntity.ok(ApiResponse.success(productService.getLowStockVariants()));
    }

    @GetMapping("/variants/search")
    @Operation(summary = "Search variants by code or colour")
    public ResponseEntity<ApiResponse<List<ProductVariantResponse>>> searchVariants(@RequestParam String q) {
        return ResponseEntity.ok(ApiResponse.success(productService.searchVariants(q)));
    }

    @DeleteMapping("/variants/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Deactivate variant")
    public ResponseEntity<ApiResponse<Void>> deleteVariant(@PathVariable Long id) {
        productService.deleteVariant(id);
        return ResponseEntity.ok(ApiResponse.success("Variant deactivated", null));
    }
}
