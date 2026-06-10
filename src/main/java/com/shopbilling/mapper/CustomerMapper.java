package com.shopbilling.mapper;

import com.shopbilling.dto.request.CustomerRequest;
import com.shopbilling.dto.response.CustomerResponse;
import com.shopbilling.entity.Customer;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CustomerMapper {
    
    @Mapping(target = "totalPurchases", expression = "java(customer.getInvoices() != null ? customer.getInvoices().size() : 0)")
    CustomerResponse toResponse(Customer customer);
    
    List<CustomerResponse> toResponseList(List<Customer> customers);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "invoices", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Customer toEntity(CustomerRequest request);
    
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "invoices", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateFromRequest(CustomerRequest request, @MappingTarget Customer customer);
}
