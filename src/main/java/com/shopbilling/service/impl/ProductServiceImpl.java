package com.shopbilling.service.impl;

import com.shopbilling.dto.request.ProductRequest;
import com.shopbilling.dto.request.ProductVariantRequest;
import com.shopbilling.dto.response.ProductResponse;
import com.shopbilling.dto.response.ProductVariantResponse;
import com.shopbilling.entity.Product;
import com.shopbilling.entity.ProductVariant;
import com.shopbilling.enums.AuditAction;
import com.shopbilling.exception.DuplicateResourceException;
import com.shopbilling.exception.ResourceNotFoundException;
import com.shopbilling.mapper.ProductMapper;
import com.shopbilling.mapper.ProductVariantMapper;
import com.shopbilling.repository.ProductRepository;
import com.shopbilling.repository.ProductVariantRepository;
import com.shopbilling.service.AuditLogService;
import com.shopbilling.service.ProductService;
import com.shopbilling.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductMapper productMapper;
    private final ProductVariantMapper variantMapper;
    private final AuditLogService auditLogService;
    private final SecurityUtils securityUtils;

    @Override
    public ProductResponse createProduct(ProductRequest request) {
        Product product = productMapper.toEntity(request);
        Product saved = productRepository.save(product);
        auditLogService.log(AuditAction.PRODUCT_CREATED, "Product", saved.getId(),
                "Product created: " + saved.getDesignName());
        log.info("Product created: {}", saved.getDesignName());
        return productMapper.toResponse(saved);
    }

    @Override
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = getProductEntityById(id);
        productMapper.updateFromRequest(request, product);
        Product saved = productRepository.save(product);
        auditLogService.log(AuditAction.PRODUCT_UPDATED, "Product", saved.getId(),
                "Product updated: " + saved.getDesignName());
        return productMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        return productMapper.toResponse(getProductEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productMapper.toResponseList(productRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getActiveProducts() {
        return productMapper.toResponseList(productRepository.findByActiveTrue());
    }

    @Override
    public void deleteProduct(Long id) {
        Product product = getProductEntityById(id);
        product.setActive(false);
        productRepository.save(product);
        log.info("Product deactivated: {}", product.getDesignName());
    }

    @Override
    public ProductVariantResponse createVariant(ProductVariantRequest request) {
        Product product = getProductEntityById(request.getProductId());
        
        if (variantRepository.existsByProductCode(request.getProductCode())) {
            throw new DuplicateResourceException("Product code already exists: " + request.getProductCode());
        }

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .productCode(request.getProductCode())
                .color(request.getColor())
                .size(request.getSize())
                .sellingPrice(request.getSellingPrice())
                .costPrice(request.getCostPrice())
                .stock(request.getStock() != null ? request.getStock() : 0)
                .minimumStock(request.getMinimumStock() != null ? request.getMinimumStock() : 5)
                .imageUrl(request.getImageUrl())
                .active(true)
                .build();

        ProductVariant saved = variantRepository.save(variant);
        auditLogService.log(AuditAction.VARIANT_CREATED, "ProductVariant", saved.getId(),
                "Variant created: " + saved.getProductCode());
        log.info("Product variant created: {}", saved.getProductCode());
        return variantMapper.toResponse(saved);
    }

    @Override
    public ProductVariantResponse updateVariant(Long id, ProductVariantRequest request) {
        ProductVariant variant = getVariantEntityById(id);

        if (!variant.getProductCode().equals(request.getProductCode()) &&
                variantRepository.existsByProductCode(request.getProductCode())) {
            throw new DuplicateResourceException("Product code already exists: " + request.getProductCode());
        }

        variant.setProductCode(request.getProductCode());
        variant.setColor(request.getColor());
        variant.setSize(request.getSize());
        variant.setSellingPrice(request.getSellingPrice());
        variant.setCostPrice(request.getCostPrice());
        if (request.getStock() != null) variant.setStock(request.getStock());
        if (request.getMinimumStock() != null) variant.setMinimumStock(request.getMinimumStock());
        if (request.getImageUrl() != null) variant.setImageUrl(request.getImageUrl());

        ProductVariant saved = variantRepository.save(variant);
        auditLogService.log(AuditAction.VARIANT_UPDATED, "ProductVariant", saved.getId(),
                "Variant updated: " + saved.getProductCode());
        return variantMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductVariantResponse getVariantById(Long id) {
        ProductVariant variant = getVariantEntityById(id);
        ProductVariantResponse response = variantMapper.toResponse(variant);
        // Hide cost price for non-admin
        if (!securityUtils.isAdmin()) {
            response.setCostPrice(null);
        }
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ProductVariantResponse getVariantByBarcode(String barcode) {
        ProductVariant variant = variantRepository.findByBarcode(barcode)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "barcode", barcode));
        return variantMapper.toResponse(variant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductVariantResponse> getVariantsByProduct(Long productId) {
        List<ProductVariant> variants = variantRepository.findByProductId(productId);
        return variantMapper.toResponseList(variants);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductVariantResponse> getLowStockVariants() {
        return variantMapper.toResponseList(variantRepository.findLowStockVariants());
    }

    @Override
    public void deleteVariant(Long id) {
        ProductVariant variant = getVariantEntityById(id);
        variant.setActive(false);
        variantRepository.save(variant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductVariantResponse> searchVariants(String query) {
        return variantMapper.toResponseList(variantRepository.searchVariants(query));
    }

    private Product getProductEntityById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }

    private ProductVariant getVariantEntityById(Long id) {
        return variantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "id", id));
    }
}
