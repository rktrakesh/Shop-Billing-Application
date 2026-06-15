package com.shopbilling.controller;

import com.shopbilling.dto.request.ItemReturnRequest;
import com.shopbilling.dto.response.ApiResponse;
import com.shopbilling.dto.response.ItemReturnResponse;
import com.shopbilling.service.ReturnService;
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
@RequestMapping("/api/returns")
@RequiredArgsConstructor
@Tag(name = "Returns")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','USER')")
public class ReturnController {

    private final ReturnService returnService;

    @PostMapping
    @Operation(summary = "Process an item return (restocks the variant and logs the refund)")
    public ResponseEntity<ApiResponse<ItemReturnResponse>> createReturn(@Valid @RequestBody ItemReturnRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Return processed successfully", returnService.createReturn(request)));
    }

    @GetMapping
    @Operation(summary = "Get all returns")
    public ResponseEntity<ApiResponse<List<ItemReturnResponse>>> getAllReturns() {
        return ResponseEntity.ok(ApiResponse.success(returnService.getAllReturns()));
    }

    @GetMapping("/invoice/{invoiceId}")
    @Operation(summary = "Get returns for a specific invoice")
    public ResponseEntity<ApiResponse<List<ItemReturnResponse>>> getReturnsByInvoice(
            @PathVariable Long invoiceId) {
        return ResponseEntity.ok(ApiResponse.success(returnService.getReturnsByInvoice(invoiceId)));
    }
}