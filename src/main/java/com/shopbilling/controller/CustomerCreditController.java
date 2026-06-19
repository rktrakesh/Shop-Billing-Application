package com.shopbilling.controller;

import com.shopbilling.dto.request.CreditPaymentRequest;
import com.shopbilling.dto.request.CustomerCreditRequest;
import com.shopbilling.dto.response.ApiResponse;
import com.shopbilling.dto.response.CustomerCreditResponse;
import com.shopbilling.service.CustomerCreditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/credits")
@RequiredArgsConstructor
@Tag(name = "Customer Credits")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','USER')")
public class CustomerCreditController {

    private final CustomerCreditService creditService;

    @PostMapping
    @Operation(summary = "Create a credit for a partially paid invoice")
    public ResponseEntity<ApiResponse<CustomerCreditResponse>> createCredit(
            @Valid @RequestBody CustomerCreditRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Credit created", creditService.createCredit(request)));
    }

    @GetMapping
    @Operation(summary = "Get all credits (all roles)")
    public ResponseEntity<ApiResponse<List<CustomerCreditResponse>>> getAllCredits() {
        return ResponseEntity.ok(ApiResponse.success(creditService.getAllCredits()));
    }

    @GetMapping("/pending")
    @Operation(summary = "Get only pending/partial (outstanding) credits")
    public ResponseEntity<ApiResponse<List<CustomerCreditResponse>>> getPendingCredits() {
        return ResponseEntity.ok(ApiResponse.success(creditService.getPendingCredits()));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get all credits for a specific customer")
    public ResponseEntity<ApiResponse<List<CustomerCreditResponse>>> getCreditsByCustomer(
            @PathVariable Long customerId) {
        return ResponseEntity.ok(ApiResponse.success(creditService.getCreditsByCustomer(customerId)));
    }

    @GetMapping("/invoice/{invoiceId}")
    @Operation(summary = "Get credit for a specific invoice (if any)")
    public ResponseEntity<ApiResponse<CustomerCreditResponse>> getCreditByInvoice(
            @PathVariable Long invoiceId) {
        return ResponseEntity.ok(ApiResponse.success(creditService.getCreditByInvoice(invoiceId)));
    }

    @PostMapping("/{creditId}/payment")
    @Operation(summary = "Record a repayment (partial or full) against a credit")
    public ResponseEntity<ApiResponse<CustomerCreditResponse>> recordPayment(
            @PathVariable Long creditId,
            @Valid @RequestBody CreditPaymentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Payment recorded", creditService.recordPayment(creditId, request)));
    }

    @GetMapping("/summary")
    @Operation(summary = "Dashboard summary — pending credit count and total outstanding amount")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSummary() {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "pendingCount", creditService.countPendingCredits(),
                "totalOutstanding", creditService.totalOutstandingAmount()
        )));
    }

    @GetMapping("/customer/{customerId}/check")
    @Operation(summary = "Check if a customer has outstanding credit (for billing warning)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkCustomerCredit(
            @PathVariable Long customerId) {
        boolean hasCredit = creditService.customerHasOutstandingCredit(customerId);
        BigDecimal outstanding = creditService.getCustomerOutstandingAmount(customerId);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "hasOutstandingCredit", hasCredit,
                "outstandingAmount", outstanding
        )));
    }
}