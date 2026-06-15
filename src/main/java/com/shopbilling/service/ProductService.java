package com.shopbilling.service;

import com.shopbilling.dto.request.ProductRequest;
import com.shopbilling.dto.request.ProductVariantRequest;
import com.shopbilling.dto.response.ProductResponse;
import com.shopbilling.dto.response.ProductVariantResponse;

import java.util.List;

public interface ProductService {
    ProductResponse createProduct(ProductRequest request);
    ProductResponse updateProduct(Long id, ProductRequest request);
    ProductResponse getProductById(Long id);
    List<ProductResponse> getAllProducts();
    List<ProductResponse> getActiveProducts();
    void deleteProduct(Long id);
    
    ProductVariantResponse createVariant(ProductVariantRequest request);
    ProductVariantResponse updateVariant(Long id, ProductVariantRequest request);
    ProductVariantResponse getVariantById(Long id);
    ProductVariantResponse getVariantByBarcode(String barcode);
    List<ProductVariantResponse> getVariantsByProduct(Long productId);
    List<ProductVariantResponse> getAllVariants();
    List<ProductVariantResponse> getLowStockVariants();
    void deleteVariant(Long id);
    List<ProductVariantResponse> searchVariants(String query);
}
