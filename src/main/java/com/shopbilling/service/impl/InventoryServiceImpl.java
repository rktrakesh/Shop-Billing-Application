package com.shopbilling.service.impl;

import com.shopbilling.dto.request.StockAdjustmentRequest;
import com.shopbilling.dto.response.ProductVariantResponse;
import com.shopbilling.dto.response.StockMovementResponse;
import com.shopbilling.entity.ProductVariant;
import com.shopbilling.entity.StockMovement;
import com.shopbilling.entity.User;
import com.shopbilling.enums.AuditAction;
import com.shopbilling.enums.StockChangeType;
import com.shopbilling.exception.BusinessException;
import com.shopbilling.exception.ResourceNotFoundException;
import com.shopbilling.mapper.ProductVariantMapper;
import com.shopbilling.repository.ProductVariantRepository;
import com.shopbilling.repository.StockMovementRepository;
import com.shopbilling.service.AuditLogService;
import com.shopbilling.service.InventoryService;
import com.shopbilling.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InventoryServiceImpl implements InventoryService {

    private final ProductVariantRepository variantRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductVariantMapper variantMapper;
    private final AuditLogService auditLogService;
    private final SecurityUtils securityUtils;

    @Override
    public ProductVariantResponse adjustStock(StockAdjustmentRequest request) {
        ProductVariant variant = variantRepository.findById(request.getProductVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "id", request.getProductVariantId()));

        User currentUser = securityUtils.getCurrentUser();
        int stockBefore = variant.getStock();
        int newStock;

        switch (request.getChangeType()) {
            case ADD_STOCK:
                newStock = stockBefore + request.getQuantity();
                break;
            case RETURN:
                newStock = stockBefore + request.getQuantity();
                break;
            case MANUAL_ADJUSTMENT:
                newStock = request.getQuantity();
                break;
            case SALE:
                if (stockBefore < request.getQuantity()) {
                    throw new BusinessException("Insufficient stock. Available: " + stockBefore);
                }
                newStock = stockBefore - request.getQuantity();
                break;
            default:
                throw new BusinessException("Invalid change type: " + request.getChangeType());
        }

        variant.setStock(Math.max(0, newStock));
        variantRepository.save(variant);

        StockMovement movement = StockMovement.builder()
                .productVariant(variant)
                .changeType(request.getChangeType())
                .quantity(request.getQuantity())
                .stockBefore(stockBefore)
                .stockAfter(variant.getStock())
                .reason(request.getReason())
                .createdBy(currentUser)
                .build();
        stockMovementRepository.save(movement);

        auditLogService.log(AuditAction.INVENTORY_UPDATED, "ProductVariant", variant.getId(),
                "Stock adjusted: " + request.getChangeType() + " qty=" + request.getQuantity() +
                        " [" + stockBefore + " -> " + variant.getStock() + "]");
        log.info("Stock adjusted for variant {}: {} -> {}", variant.getProductCode(), stockBefore, variant.getStock());

        return variantMapper.toResponse(variant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockMovementResponse> getStockHistory(Long variantId) {
        List<StockMovement> movements = stockMovementRepository.findByProductVariantIdOrderByCreatedAtDesc(variantId);
        return movements.stream().map(this::toMovementResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductVariantResponse> getLowStockProducts() {
        return variantMapper.toResponseList(variantRepository.findLowStockVariants());
    }

    private StockMovementResponse toMovementResponse(StockMovement movement) {
        StockMovementResponse response = new StockMovementResponse();
        response.setId(movement.getId());
        response.setProductVariantId(movement.getProductVariant().getId());
        response.setProductCode(movement.getProductVariant().getProductCode());
        response.setDesignName(movement.getProductVariant().getProduct().getDesignName());
        response.setChangeType(movement.getChangeType());
        response.setQuantity(movement.getQuantity());
        response.setStockBefore(movement.getStockBefore());
        response.setStockAfter(movement.getStockAfter());
        response.setReason(movement.getReason());
        response.setCreatedBy(movement.getCreatedBy() != null ? movement.getCreatedBy().getUsername() : null);
        response.setCreatedAt(movement.getCreatedAt());
        return response;
    }
}
