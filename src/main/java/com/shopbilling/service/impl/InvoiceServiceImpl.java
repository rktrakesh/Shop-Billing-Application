package com.shopbilling.service.impl;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.shopbilling.dto.request.InvoiceItemRequest;
import com.shopbilling.dto.request.InvoiceRequest;
import com.shopbilling.dto.response.InvoiceResponse;
import com.shopbilling.entity.*;
import com.shopbilling.enums.AuditAction;
import com.shopbilling.enums.StockChangeType;
import com.shopbilling.exception.BusinessException;
import com.shopbilling.exception.ResourceNotFoundException;
import com.shopbilling.mapper.InvoiceMapper;
import com.shopbilling.repository.*;
import com.shopbilling.service.AuditLogService;
import com.shopbilling.service.InvoiceService;
import com.shopbilling.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final ProductVariantRepository variantRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ShopSettingsRepository shopSettingsRepository;
    private final InvoiceMapper invoiceMapper;
    private final AuditLogService auditLogService;
    private final SecurityUtils securityUtils;
    private final CustomerCreditRepository customerCreditRepository;

    @Override
    public InvoiceResponse createInvoice(InvoiceRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        // Resolve customer
        Customer customer = null;
        String customerName = request.getCustomerName();
        String customerMobile = request.getCustomerMobile();

        if (request.getCustomerId() != null) {
            customer = customerRepository.findById(request.getCustomerId()).orElse(null);
            if (customer != null) {
                customerName = customer.getName();
                customerMobile = customer.getMobileNumber();
            }
        }

        // Normalize mobile number so walk-in lookups (GET /api/invoices/by-mobile)
        // match consistently regardless of spacing/country-code formatting.
        if (customerMobile != null && !customerMobile.isBlank()) {
            customerMobile = normalizeMobile(customerMobile);
        }

        // Build invoice items
        List<InvoiceItem> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (InvoiceItemRequest itemReq : request.getItems()) {
            InvoiceItem item = buildInvoiceItem(itemReq);
            items.add(item);
            subtotal = subtotal.add(item.getLineTotal());
        }

        BigDecimal discountAmount = request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal taxAmount = request.getTaxAmount() != null ? request.getTaxAmount() : BigDecimal.ZERO;
        BigDecimal grandTotal = subtotal.subtract(discountAmount).add(taxAmount);

        Invoice invoice = Invoice.builder()
                .invoiceNumber(generateInvoiceNumber())
                .invoiceDate(LocalDateTime.now())
                .customer(customer)
                .customerName(customerName)
                .customerMobile(customerMobile)
                .subtotal(subtotal)
                .discountAmount(discountAmount)
                .taxAmount(taxAmount)
                .grandTotal(grandTotal)
                .paymentMode(request.getPaymentMode() != null ? request.getPaymentMode() : com.shopbilling.enums.PaymentMode.CASH)
                .createdBy(currentUser)
                .notes(request.getNotes())
                .build();

        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Save items and reduce stock
        for (InvoiceItem item : items) {
            item.setInvoice(savedInvoice);
        }
        savedInvoice.setItems(items);
        invoiceRepository.save(savedInvoice);

        // Reduce stock for barcode items
        for (InvoiceItem item : items) {
            if (item.getProductVariant() != null) {
                reduceStock(item.getProductVariant(), item.getQuantity(), savedInvoice.getId(), currentUser);
            }
        }

        // Auto-create credit record if customer paid less than the invoice total
        if (request.getAmountPaid() != null && request.getAmountPaid().compareTo(grandTotal) < 0) {
            BigDecimal outstanding = grandTotal.subtract(request.getAmountPaid());
            com.shopbilling.entity.CustomerCredit credit = com.shopbilling.entity.CustomerCredit.builder()
                    .invoice(savedInvoice)
                    .customer(customer)
                    .customerName(customerName)
                    .customerMobile(customerMobile)
                    .totalAmount(grandTotal)
                    .amountPaid(request.getAmountPaid())
                    .outstandingAmount(outstanding)
                    .status(request.getAmountPaid().compareTo(BigDecimal.ZERO) == 0
                            ? com.shopbilling.enums.CreditStatus.PENDING
                            : com.shopbilling.enums.CreditStatus.PARTIAL)
                    .createdBy(currentUser)
                    .build();
            customerCreditRepository.save(credit);
            log.info("Credit auto-created for invoice {} — outstanding: {}", savedInvoice.getInvoiceNumber(), outstanding);
        }

        auditLogService.log(AuditAction.INVOICE_CREATED, "Invoice", savedInvoice.getId(),
                "Invoice created: " + savedInvoice.getInvoiceNumber() + " Total: " + grandTotal);
        log.info("Invoice created: {}", savedInvoice.getInvoiceNumber());

        return enrichWithCredit(invoiceMapper.toResponse(savedInvoice));
    }

    private InvoiceItem buildInvoiceItem(InvoiceItemRequest itemReq) {
        InvoiceItem item = new InvoiceItem();
        ProductVariant variant = null;

        if (itemReq.getBarcode() != null && !itemReq.getBarcode().isEmpty()) {
            variant = variantRepository.findByBarcode(itemReq.getBarcode())
                    .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "barcode", itemReq.getBarcode()));

            if (variant.getStock() < itemReq.getQuantity()) {
                throw new BusinessException("Insufficient stock for: " + variant.getProductCode() +
                        ". Available: " + variant.getStock());
            }

            item.setProductVariant(variant);
            item.setBarcode(variant.getBarcode());
            item.setDesignName(variant.getProduct().getDesignName());
            item.setProductCode(variant.getProductCode());
            item.setColor(variant.getColor());
            item.setSize(variant.getSize());
            item.setPrintType(variant.getProduct().getPrintType());
            item.setUnitPrice(variant.getSellingPrice());
        } else {
            // Manual mode
            item.setDesignName(itemReq.getDesignName());
            item.setProductCode(itemReq.getProductCode());
            item.setColor(itemReq.getColor());
            item.setSize(itemReq.getSize());
            item.setPrintType(itemReq.getPrintType());
            item.setUnitPrice(itemReq.getUnitPrice());
        }

        BigDecimal discount = itemReq.getDiscountAmount() != null ? itemReq.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal lineTotal = item.getUnitPrice()
                .multiply(BigDecimal.valueOf(itemReq.getQuantity()))
                .subtract(discount);

        item.setQuantity(itemReq.getQuantity());
        item.setDiscountAmount(discount);
        item.setLineTotal(lineTotal);

        return item;
    }

    private void reduceStock(ProductVariant variant, int quantity, Long invoiceId, User user) {
        int stockBefore = variant.getStock();
        variant.setStock(Math.max(0, stockBefore - quantity));
        variantRepository.save(variant);

        StockMovement movement = StockMovement.builder()
                .productVariant(variant)
                .changeType(StockChangeType.SALE)
                .quantity(quantity)
                .stockBefore(stockBefore)
                .stockAfter(variant.getStock())
                .reason("Invoice sale")
                .referenceId(invoiceId)
                .createdBy(user)
                .build();
        stockMovementRepository.save(movement);
    }

    private String generateInvoiceNumber() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        List<String> lastNumbers = invoiceRepository.findLastInvoiceNumber(PageRequest.of(0, 1));

        int sequence = 1;
        if (!lastNumbers.isEmpty()) {
            String lastNumber = lastNumbers.get(0);
            try {
                String[] parts = lastNumber.split("-");
                if (parts.length == 3 && parts[1].equals(dateStr)) {
                    sequence = Integer.parseInt(parts[2]) + 1;
                }
            } catch (Exception e) {
                log.warn("Could not parse last invoice number: {}", lastNumber);
            }
        }
        return String.format("INV-%s-%04d", dateStr, sequence);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));
        return invoiceMapper.toResponse(invoice);
    }

    // Populate hasCredit and outstandingAmount on a single InvoiceResponse.
    // Only called for detail views (not list views) to avoid N queries.
    private InvoiceResponse enrichWithCredit(InvoiceResponse response) {
        customerCreditRepository.findByInvoice_Id(response.getId()).ifPresent(credit -> {
            response.setHasCredit(credit.getStatus() != com.shopbilling.enums.CreditStatus.CLEARED);
            response.setOutstandingAmount(credit.getOutstandingAmount());
        });
        if (response.getHasCredit() == null) response.setHasCredit(false);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getAllInvoices() {
        return invoiceMapper.toResponseList(invoiceRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getMyInvoices() {
        User currentUser = securityUtils.getCurrentUser();
        return invoiceMapper.toResponseList(invoiceRepository.findByCreatedByOrderByCreatedAtDesc(currentUser));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getInvoicesByMobile(String mobile) {
        String normalized = normalizeMobile(mobile);
        if (normalized.isEmpty()) {
            return List.of();
        }
        // customer_mobile is normalized at write time (see createInvoice), so a
        // direct equality match is sufficient and indexed for performance.
        return invoiceMapper.toResponseList(invoiceRepository.findAllByCustomerMobileOrderByInvoiceDateDesc(normalized));
    }

    /**
     * Strips spaces, dashes, and a leading "+91"/"91" country code so that
     * "9876543210", "+91 98765 43210", and "91-9876543210" are all stored
     * and matched as the same 10-digit number.
     */
    private String normalizeMobile(String mobile) {
        if (mobile == null) return "";
        String digits = mobile.replaceAll("[^0-9]", "");
        if (digits.length() > 10 && digits.startsWith("91")) {
            digits = digits.substring(digits.length() - 10);
        }
        return digits;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateInvoicePdf(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", invoiceId));
        ShopSettings settings = shopSettingsRepository.findFirstByOrderByIdAsc()
                .orElse(ShopSettings.builder().shopName("Shop").build());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);
            document.open();

            // Shop Header
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);

            Paragraph shopName = new Paragraph(settings.getShopName(), titleFont);
            shopName.setAlignment(Element.ALIGN_CENTER);
            document.add(shopName);

            if (settings.getShopAddress() != null) {
                Paragraph addr = new Paragraph(settings.getShopAddress(), normalFont);
                addr.setAlignment(Element.ALIGN_CENTER);
                document.add(addr);
            }
            if (settings.getMobileNumber() != null) {
                Paragraph mobile = new Paragraph("Mobile: " + settings.getMobileNumber(), normalFont);
                mobile.setAlignment(Element.ALIGN_CENTER);
                document.add(mobile);
            }
            if (settings.getGstNumber() != null) {
                Paragraph gst = new Paragraph("GST: " + settings.getGstNumber(), normalFont);
                gst.setAlignment(Element.ALIGN_CENTER);
                document.add(gst);
            }

            document.add(new Paragraph(" "));
            document.add(new com.lowagie.text.pdf.draw.LineSeparator());
            document.add(new Paragraph(" "));

            // Invoice Details
            Paragraph invTitle = new Paragraph("INVOICE", headerFont);
            invTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(invTitle);
            document.add(new Paragraph("Invoice No: " + invoice.getInvoiceNumber(), normalFont));
            document.add(new Paragraph("Date: " + invoice.getInvoiceDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")), normalFont));

            if (invoice.getCustomerName() != null) {
                document.add(new Paragraph(" "));
                document.add(new Paragraph("Customer: " + invoice.getCustomerName(), normalFont));
                if (invoice.getCustomerMobile() != null) {
                    document.add(new Paragraph("Mobile: " + invoice.getCustomerMobile(), normalFont));
                }
            }
            document.add(new Paragraph(" "));

            // Items Table
            PdfPTable table = new PdfPTable(new float[]{3, 1, 1, 1, 1, 1.5f});
            table.setWidthPercentage(100);

            String[] headers = {"Item", "Color", "Size", "Qty", "Price", "Total"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(new Color(200, 200, 200));
                cell.setPadding(5);
                table.addCell(cell);
            }

            for (InvoiceItem item : invoice.getItems()) {
                table.addCell(new PdfPCell(new Phrase(item.getDesignName() != null ? item.getDesignName() : "", normalFont)));
                table.addCell(new PdfPCell(new Phrase(item.getColor() != null ? item.getColor() : "", normalFont)));
                table.addCell(new PdfPCell(new Phrase(item.getSize() != null ? item.getSize() : "", normalFont)));
                table.addCell(new PdfPCell(new Phrase(String.valueOf(item.getQuantity()), normalFont)));
                table.addCell(new PdfPCell(new Phrase(item.getUnitPrice().toString(), normalFont)));
                table.addCell(new PdfPCell(new Phrase(item.getLineTotal().toString(), normalFont)));
            }
            document.add(table);
            document.add(new Paragraph(" "));

            // Totals
            Paragraph subtotal = new Paragraph("Subtotal: ₹" + invoice.getSubtotal(), normalFont);
            subtotal.setAlignment(Element.ALIGN_RIGHT);
            document.add(subtotal);

            if (invoice.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                Paragraph disc = new Paragraph("Discount: -₹" + invoice.getDiscountAmount(), normalFont);
                disc.setAlignment(Element.ALIGN_RIGHT);
                document.add(disc);
            }
            if (invoice.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
                Paragraph tax = new Paragraph("Tax: +₹" + invoice.getTaxAmount(), normalFont);
                tax.setAlignment(Element.ALIGN_RIGHT);
                document.add(tax);
            }

            Font grandTotalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
            Paragraph grand = new Paragraph("GRAND TOTAL: ₹" + invoice.getGrandTotal(), grandTotalFont);
            grand.setAlignment(Element.ALIGN_RIGHT);
            document.add(grand);

            if (settings.getFooterMessage() != null) {
                document.add(new Paragraph(" "));
                document.add(new com.lowagie.text.pdf.draw.LineSeparator());
                Paragraph footer = new Paragraph(settings.getFooterMessage(), smallFont);
                footer.setAlignment(Element.ALIGN_CENTER);
                document.add(footer);
            }

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("Failed to generate PDF: " + e.getMessage());
        }
    }
}