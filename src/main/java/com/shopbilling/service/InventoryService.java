package com.shopbilling.service;

import com.shopbilling.dto.request.StockAdjustmentRequest;
import com.shopbilling.dto.response.ProductVariantResponse;
import com.shopbilling.dto.response.StockMovementResponse;

import java.util.List;

public interface InventoryService {
    ProductVariantResponse adjustStock(StockAdjustmentRequest request);
    List<StockMovementResponse> getStockHistory(Long variantId);
    List<ProductVariantResponse> getLowStockProducts();
}
