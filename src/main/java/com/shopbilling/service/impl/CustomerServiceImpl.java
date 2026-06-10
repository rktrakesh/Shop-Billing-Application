package com.shopbilling.service.impl;

import com.shopbilling.dto.request.CustomerRequest;
import com.shopbilling.dto.response.CustomerResponse;
import com.shopbilling.dto.response.InvoiceResponse;
import com.shopbilling.entity.Customer;
import com.shopbilling.enums.AuditAction;
import com.shopbilling.exception.ResourceNotFoundException;
import com.shopbilling.mapper.CustomerMapper;
import com.shopbilling.mapper.InvoiceMapper;
import com.shopbilling.repository.CustomerRepository;
import com.shopbilling.repository.InvoiceRepository;
import com.shopbilling.service.AuditLogService;
import com.shopbilling.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final CustomerMapper customerMapper;
    private final InvoiceMapper invoiceMapper;
    private final AuditLogService auditLogService;

    @Override
    public CustomerResponse createCustomer(CustomerRequest request) {
        Customer customer = customerMapper.toEntity(request);
        Customer saved = customerRepository.save(customer);
        auditLogService.log(AuditAction.CUSTOMER_CREATED, "Customer", saved.getId(),
                "Customer created: " + saved.getName());
        return customerMapper.toResponse(saved);
    }

    @Override
    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        Customer customer = getCustomerEntityById(id);
        customerMapper.updateFromRequest(request, customer);
        Customer saved = customerRepository.save(customer);
        auditLogService.log(AuditAction.CUSTOMER_UPDATED, "Customer", saved.getId(),
                "Customer updated: " + saved.getName());
        return customerMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long id) {
        return customerMapper.toResponse(getCustomerEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponse> getAllCustomers() {
        return customerMapper.toResponseList(customerRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponse> searchCustomers(String query) {
        return customerMapper.toResponseList(customerRepository.searchCustomers(query));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getCustomerPurchaseHistory(Long customerId) {
        getCustomerEntityById(customerId);
        return invoiceMapper.toResponseList(invoiceRepository.findByCustomerIdOrderByCreatedAtDesc(customerId));
    }

    private Customer getCustomerEntityById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));
    }
}
