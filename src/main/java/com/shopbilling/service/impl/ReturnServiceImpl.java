package com.shopbilling.service.impl;

import com.shopbilling.dto.request.ItemReturnRequest;
import com.shopbilling.dto.response.ItemReturnResponse;
import com.shopbilling.entity.*;
import com.shopbilling.enums.AuditAction;
import com.shopbilling.enums.StockChangeType;
import com.shopbilling.exception.BusinessException;
import com.shopbilling.exception.ResourceNotFoundException;
import com.shopbilling.repository.*;
import com.shopbilling.service.AuditLogService;
import com.shopbilling.service.ReturnService;
import com.shopbilling.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final AuditLogService auditLogService;
    private final SecurityUtils securityUtils;

    // Small tolerance for rounding differences between frontend-calculated
    // refund amounts and the backend's proportional-discount recomputation.
    private static final BigDecimal REFUND_TOLERANCE = new BigDecimal("0.02");

    @Override
    @Transactional
    public ItemReturnResponse createReturn(ItemReturnRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", request.getInvoiceId()));

        InvoiceItem invoiceItem = invoiceItemRepository.findById(request.getInvoiceItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice item", "id", request.getInvoiceItemId()));

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

        // Validate: refund amount cannot exceed what was actually paid for the
        // requested quantity (accounts for item-level AND proportional
        // invoice-level discount). Small tolerance allows for rounding.
        BigDecimal maxRefund = calculateMaxRefund(invoice, invoiceItem, request.getQuantity());
        if (request.getRefundAmount().compareTo(maxRefund.add(REFUND_TOLERANCE)) > 0) {
            throw new BusinessException(
                    "Refund amount (" + request.getRefundAmount() + ") exceeds the amount actually paid "
                            + "for " + request.getQuantity() + " unit(s) of this item (" + maxRefund + ")");
        }

        // Restock — increase variant stock and log a RETURN stock movement
        ProductVariant variant = invoiceItem.getProductVariant();

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
                    .referenceId(itemReturn.getId())
                    .reason("Customer return - Invoice " + invoice.getInvoiceNumber()
                            + (request.getReason() != null && !request.getReason().isBlank()
                            ? " (" + request.getReason() + ")" : ""))
                    .createdBy(currentUser)
                    .build();
            stockMovementRepository.save(movement);
        }

        auditLogService.log(AuditAction.ITEM_RETURNED, "ItemReturn", itemReturn.getId(),
                "Returned " + request.getQuantity() + "x " + invoiceItem.getDesignName()
                        + " from invoice " + invoice.getInvoiceNumber()
                        + " (refund: " + request.getRefundAmount() + ")");

        return toResponse(itemReturn);
    }

    /**
     * The maximum refundable amount for {@code returnQty} units of this line item,
     * i.e. what the customer actually paid per unit after BOTH the item-level
     * discount AND a proportional share of the invoice-level discount.
     */
    private BigDecimal calculateMaxRefund(Invoice invoice, InvoiceItem item, int returnQty) {
        BigDecimal netLineTotal = item.getLineTotal(); // qty*price - item discount
        BigDecimal subtotal = invoice.getSubtotal();
        BigDecimal invoiceDiscount = invoice.getDiscountAmount();

        BigDecimal proportionalDiscount = BigDecimal.ZERO;
        if (subtotal != null && subtotal.compareTo(BigDecimal.ZERO) > 0
                && invoiceDiscount != null && invoiceDiscount.compareTo(BigDecimal.ZERO) > 0) {
            proportionalDiscount = netLineTotal
                    .divide(subtotal, 6, RoundingMode.HALF_UP)
                    .multiply(invoiceDiscount);
        }

        BigDecimal actualPaidForLine = netLineTotal.subtract(proportionalDiscount);
        BigDecimal actualUnitPrice = item.getQuantity() > 0
                ? actualPaidForLine.divide(BigDecimal.valueOf(item.getQuantity()), 6, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return actualUnitPrice.multiply(BigDecimal.valueOf(returnQty)).setScale(2, RoundingMode.HALF_UP);
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
        ItemReturnResponse res = new ItemReturnResponse();
        res.setId(r.getId());
        res.setInvoiceId(r.getInvoice().getId());
        res.setInvoiceNumber(r.getInvoice().getInvoiceNumber());
        res.setInvoiceItemId(item.getId());
        res.setDesignName(item.getDesignName());
        res.setColor(item.getColor());
        res.setSize(item.getSize());
        res.setProductCode(item.getProductCode());
        res.setQuantity(r.getQuantity());
        res.setRefundAmount(r.getRefundAmount());
        res.setReason(r.getReason());
        res.setReturnedByUsername(r.getReturnedBy() != null ? r.getReturnedBy().getUsername() : null);
        res.setCreatedAt(r.getCreatedAt());
        return res;
    }
}