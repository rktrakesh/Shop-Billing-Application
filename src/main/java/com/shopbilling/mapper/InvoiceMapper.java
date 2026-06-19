package com.shopbilling.mapper;

import com.shopbilling.dto.response.InvoiceItemResponse;
import com.shopbilling.dto.response.InvoiceResponse;
import com.shopbilling.entity.Invoice;
import com.shopbilling.entity.InvoiceItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface InvoiceMapper {

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "createdByUsername", source = "createdBy.username")
    @Mapping(target = "hasCredit", ignore = true)
    @Mapping(target = "outstandingAmount", ignore = true)
    InvoiceResponse toResponse(Invoice invoice);

    List<InvoiceResponse> toResponseList(List<Invoice> invoices);

    @Mapping(target = "productVariantId", source = "productVariant.id")
    InvoiceItemResponse toItemResponse(InvoiceItem item);
}
