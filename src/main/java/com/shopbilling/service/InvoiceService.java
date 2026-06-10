package com.shopbilling.service;

import com.shopbilling.dto.request.InvoiceRequest;
import com.shopbilling.dto.response.InvoiceResponse;

import java.util.List;

public interface InvoiceService {
    InvoiceResponse createInvoice(InvoiceRequest request);
    InvoiceResponse getInvoiceById(Long id);
    List<InvoiceResponse> getAllInvoices();
    List<InvoiceResponse> getMyInvoices();
    byte[] generateInvoicePdf(Long invoiceId);
}
