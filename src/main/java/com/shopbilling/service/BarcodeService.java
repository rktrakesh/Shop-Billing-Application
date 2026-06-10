package com.shopbilling.service;

import com.shopbilling.dto.response.ProductVariantResponse;

public interface BarcodeService {
    ProductVariantResponse generateBarcode(Long variantId);
    ProductVariantResponse regenerateBarcode(Long variantId);
    byte[] downloadBarcodePng(Long variantId);
    byte[] downloadBarcodePdf(Long variantId);
    ProductVariantResponse searchByBarcode(String barcode);
}
