package com.shopbilling.service;

import com.shopbilling.dto.request.ItemReturnRequest;
import com.shopbilling.dto.response.ItemReturnResponse;

import java.util.List;

public interface ReturnService {
    ItemReturnResponse createReturn(ItemReturnRequest request);
    List<ItemReturnResponse> getAllReturns();
    List<ItemReturnResponse> getReturnsByInvoice(Long invoiceId);
    Integer getReturnedQuantity(Long invoiceItemId);
}