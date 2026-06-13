package com.shopbilling.service.impl;

import com.shopbilling.dto.request.ItemReturnRequest;
import com.shopbilling.dto.response.ItemReturnResponse;
import com.shopbilling.entity.*;
import com.shopbilling.enums.StockChangeType;
import com.shopbilling.exception.BusinessException;
import com.shopbilling.exception.ResourceNotFoundException;
import com.shopbilling.repository.*;
import com.shopbilling.service.ReturnService;
import com.shopbilling.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReturnServiceImpl implements ReturnService {

    private final ItemReturnRepository itemReturnRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final StockMovementRepository stockMovementRepository;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public ItemReturnResponse createReturn(ItemReturnRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        InvoiceItem invoiceItem = invoiceItemRepository.findById(request.getInvoiceItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice item not found"));

        if (!invoiceItem.getInvoice().getId().equals(invoice.getId())) {
            throw new BusinessException("Invoice item does not belong to the specified invoice");
        }

        // Validate: cannot return more than purchased minus already returned
        Integer alreadyReturned = itemReturnRepository.sumReturnedQuantityForInvoiceItem(invoiceItem.getId());
        int availableToReturn = invoiceItem.getQuantity() - alreadyReturned;
        if (request.getQuantity() > availableToReturn) {
            throw new BusinessException(
                    "Cannot return " + request.getQuantity() + " item(s). Only "
                            + availableToReturn + " available to return for this line item.");
        }

        // Restock — increase variant stock and log a RETURN stock movement
        ProductVariant variant = invoiceItem.getProductVariant();
        if (variant != null) {
            int stockBefore = variant.getStock();
            variant.setStock(stockBefore + request.getQuantity());
            productVariantRepository.save(variant);

            StockMovement movement = StockMovement.builder()
                    .productVariant(variant)
                    .changeType(StockChangeType.RETURN)
                    .quantity(request.getQuantity())
                    .stockBefore(stockBefore)
                    .stockAfter(variant.getStock())
                    .reason("Customer return — Invoice " + invoice.getInvoiceNumber()
                            + (request.getReason() != null ? " (" + request.getReason() + ")" : ""))
                    .createdBy(currentUser)
                    .build();
            stockMovementRepository.save(movement);
        }

        ItemReturn itemReturn = ItemReturn.builder()
                .invoice(invoice)
                .invoiceItem(invoiceItem)
                .productVariant(variant)
                .quantity(request.getQuantity())
                .refundAmount(request.getRefundAmount())
                .reason(request.getReason())
                .returnedBy(currentUser)
                .build();

        itemReturn = itemReturnRepository.save(itemReturn);
        return toResponse(itemReturn);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemReturnResponse> getAllReturns() {
        return itemReturnRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemReturnResponse> getReturnsByInvoice(Long invoiceId) {
        return itemReturnRepository.findAllByInvoice_IdOrderByCreatedAtDesc(invoiceId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getReturnedQuantity(Long invoiceItemId) {
        return itemReturnRepository.sumReturnedQuantityForInvoiceItem(invoiceItemId);
    }

    private ItemReturnResponse toResponse(ItemReturn r) {
        InvoiceItem item = r.getInvoiceItem();
        return ItemReturnResponse.builder()
                .id(r.getId())
                .invoiceId(r.getInvoice().getId())
                .invoiceNumber(r.getInvoice().getInvoiceNumber())
                .invoiceItemId(item.getId())
                .designName(item.getDesignName())
                .color(item.getColor())
                .size(item.getSize())
                .productCode(item.getProductCode())
                .quantity(r.getQuantity())
                .refundAmount(r.getRefundAmount())
                .reason(r.getReason())
                .returnedByUsername(r.getReturnedBy() != null ? r.getReturnedBy().getUsername() : null)
                .createdAt(r.getCreatedAt())
                .build();
    }
}