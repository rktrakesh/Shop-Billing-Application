package com.shopbilling.controller;

import com.shopbilling.dto.request.CustomerRequest;
import com.shopbilling.dto.response.ApiResponse;
import com.shopbilling.dto.response.CustomerResponse;
import com.shopbilling.dto.response.InvoiceResponse;
import com.shopbilling.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "Customer Management")
@SecurityRequirement(name = "bearerAuth")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @Operation(summary = "Create customer")
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(@Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer created", customerService.createCustomer(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update customer")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(
            @PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Customer updated", customerService.updateCustomer(id, request)));
    }

    @GetMapping
    @Operation(summary = "Get all customers")
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> getAllCustomers() {
        return ResponseEntity.ok(ApiResponse.success(customerService.getAllCustomers()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get customer by ID")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(customerService.getCustomerById(id)));
    }

    @GetMapping("/search")
    @Operation(summary = "Search customers by name or mobile")
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> searchCustomers(@RequestParam String q) {
        return ResponseEntity.ok(ApiResponse.success(customerService.searchCustomers(q)));
    }

    @GetMapping("/{id}/purchases")
    @Operation(summary = "Get customer purchase history")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getPurchaseHistory(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(customerService.getCustomerPurchaseHistory(id)));
    }
}
