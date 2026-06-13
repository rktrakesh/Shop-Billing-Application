package com.shopbilling.controller;

import com.shopbilling.dto.request.ItemReturnRequest;
import com.shopbilling.dto.response.ApiResponse;
import com.shopbilling.dto.response.ItemReturnResponse;
import com.shopbilling.service.ReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/returns")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
public class ReturnController {

    private final ReturnService returnService;

    @PostMapping
    public ResponseEntity<ApiResponse<ItemReturnResponse>> createReturn(
            @Valid @RequestBody ItemReturnRequest request) {
        ItemReturnResponse response = returnService.createReturn(request);
        return ResponseEntity.ok(ApiResponse.success("Return processed successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ItemReturnResponse>>> getAllReturns() {
        return ResponseEntity.ok(ApiResponse.success(returnService.getAllReturns()));
    }

    @GetMapping("/invoice/{invoiceId}")
    public ResponseEntity<ApiResponse<List<ItemReturnResponse>>> getReturnsByInvoice(
            @PathVariable Long invoiceId) {
        return ResponseEntity.ok(ApiResponse.success(returnService.getReturnsByInvoice(invoiceId)));
    }
}