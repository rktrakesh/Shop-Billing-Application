package com.shopbilling.service;

import com.shopbilling.dto.request.CustomerRequest;
import com.shopbilling.dto.response.CustomerResponse;
import com.shopbilling.dto.response.InvoiceResponse;

import java.util.List;

public interface CustomerService {
    CustomerResponse createCustomer(CustomerRequest request);
    CustomerResponse updateCustomer(Long id, CustomerRequest request);
    CustomerResponse getCustomerById(Long id);
    List<CustomerResponse> getAllCustomers();
    List<CustomerResponse> searchCustomers(String query);
    List<InvoiceResponse> getCustomerPurchaseHistory(Long customerId);
}
