package com.shopbilling.service.impl;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.shopbilling.dto.request.ReportRequest;
import com.shopbilling.dto.response.ReportHistoryResponse;
import com.shopbilling.entity.*;
import com.shopbilling.enums.AuditAction;
import com.shopbilling.enums.ReportType;
import com.shopbilling.exception.BusinessException;
import com.shopbilling.exception.ResourceNotFoundException;
import com.shopbilling.repository.*;
import com.shopbilling.service.AuditLogService;
import com.shopbilling.service.ReportService;
import com.shopbilling.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReportServiceImpl implements ReportService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final CustomerRepository customerRepository;
    private final MonthlyProfitRepository profitRepository;
    private final ReportHistoryRepository reportHistoryRepository;
    private final ShopSettingsRepository shopSettingsRepository;
    private final ItemReturnRepository itemReturnRepository;
    private final AuditLogService auditLogService;
    private final SecurityUtils securityUtils;

    @Value("${app.report-dir:./reports}")
    private String reportDir;

    // Threshold: reports covering more than this many days use the
    // condensed (no per-customer breakdown) layout instead of the
    // fully detailed, per-customer layout.
    private static final long DETAILED_LAYOUT_MAX_DAYS = 1;

    // ── Fonts (shared across report builders) ───────────────────────
    private static final Color BRAND_COLOR   = new Color(30, 80, 160);
    private static final Color LIGHT_GREY    = new Color(245, 246, 248);
    private static final Color SUCCESS_GREEN = new Color(34, 139, 34);
    private static final Color WARNING_AMBER = new Color(180, 95, 6);

    private Font titleFont()   { return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK); }
    private Font h2Font()      { return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, BRAND_COLOR); }
    private Font h3Font()      { return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK); }
    private Font headerFont()  { return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE); }
    private Font normalFont()  { return FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK); }
    private Font smallFont()   { return FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY); }
    private Font boldFont()    { return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK); }
    private Font totalFont()   { return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK); }
    private Font refundFont()  { return FontFactory.getFont(FontFactory.HELVETICA, 9, WARNING_AMBER); }
    private Font successFont() { return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, SUCCESS_GREEN); }

    // ──────────────────────────────────────────────────────────────
    // Public entry points
    // ──────────────────────────────────────────────────────────────

    @Override
    public byte[] generateDailyReport() {
        LocalDate today = LocalDate.now();
        return generateReport(today, today, ReportType.DAILY);
    }

    @Override
    public byte[] generateMonthlyReport() {
        LocalDate now = LocalDate.now();
        LocalDate start = now.withDayOfMonth(1);
        LocalDate end = now.withDayOfMonth(now.lengthOfMonth());
        return generateReport(start, end, ReportType.MONTHLY);
    }

    @Override
    public byte[] generateYearlyReport() {
        LocalDate now = LocalDate.now();
        LocalDate start = now.withDayOfYear(1);
        LocalDate end = now.withDayOfYear(now.lengthOfYear());
        return generateReport(start, end, ReportType.YEARLY);
    }

    @Override
    public byte[] generateCustomReport(ReportRequest request) {
        return generateReport(request.getStartDate(), request.getEndDate(), ReportType.CUSTOM);
    }

    private byte[] generateReport(LocalDate startDate, LocalDate endDate, ReportType reportType) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        ShopSettings settings = shopSettingsRepository.findFirstByOrderByIdAsc()
                .orElse(ShopSettings.builder().shopName("My Shop").build());

        List<Invoice> invoices = invoiceRepository.findByDateRange(start, end);
        List<ItemReturn> returns = itemReturnRepository.findAllByCreatedAtBetweenOrderByCreatedAtDesc(start, end);

        long daySpan = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        boolean detailed = daySpan <= DETAILED_LAYOUT_MAX_DAYS
                && (reportType == ReportType.DAILY || reportType == ReportType.CUSTOM);

        byte[] pdfBytes = detailed
                ? buildDetailedReportPdf(settings, reportType, startDate, endDate, invoices, returns, start, end)
                : buildSummaryReportPdf(settings, reportType, startDate, endDate, invoices, returns, start, end);

        saveReportHistory(reportType, startDate, endDate, pdfBytes);

        auditLogService.log(AuditAction.REPORT_GENERATED, "Report", null,
                reportType + " report generated: " + startDate + " to " + endDate);

        return pdfBytes;
    }

    // ──────────────────────────────────────────────────────────────
    // Shared calculations
    // ──────────────────────────────────────────────────────────────

    /**
     * Aggregate sales/discount/return figures for a set of invoices and returns.
     */
    private static class ReportTotals {
        BigDecimal grossSales = BigDecimal.ZERO;     // sum of (subtotal) i.e. tag-price x qty - item discounts, before invoice discount
        BigDecimal totalDiscounts = BigDecimal.ZERO; // sum of invoice-level discounts
        BigDecimal netSales = BigDecimal.ZERO;       // grandTotal sum (what was actually collected)
        BigDecimal totalRefunds = BigDecimal.ZERO;   // sum of refund amounts
        int totalReturnedQty = 0;
        BigDecimal finalNetRevenue() {
            return netSales.subtract(totalRefunds);
        }
    }

    private ReportTotals computeTotals(List<Invoice> invoices, List<ItemReturn> returns) {
        ReportTotals t = new ReportTotals();
        for (Invoice inv : invoices) {
            t.grossSales = t.grossSales.add(inv.getSubtotal());
            t.totalDiscounts = t.totalDiscounts.add(inv.getDiscountAmount());
            t.netSales = t.netSales.add(inv.getGrandTotal());
        }
        for (ItemReturn r : returns) {
            t.totalRefunds = t.totalRefunds.add(r.getRefundAmount());
            t.totalReturnedQty += r.getQuantity();
        }
        return t;
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String fmt(BigDecimal value) {
        return "Rs. " + money(value).toPlainString();
    }

    // ──────────────────────────────────────────────────────────────
    // DETAILED report (Daily / Custom <= 1 day): per-customer breakdown
    // ──────────────────────────────────────────────────────────────

    private byte[] buildDetailedReportPdf(ShopSettings settings, ReportType reportType,
                                          LocalDate startDate, LocalDate endDate,
                                          List<Invoice> invoices, List<ItemReturn> returns,
                                          LocalDateTime start, LocalDateTime end) {

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 50, 50);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            writeShopHeader(doc, settings);
            writeReportTitle(doc, reportType, startDate, endDate, "DETAILED SALES & RETURNS REPORT");

            ReportTotals totals = computeTotals(invoices, returns);
            writeOverallSummary(doc, totals, invoices.size());

            // ── Customer-wise breakdown ─────────────────────────────────
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("CUSTOMER-WISE BREAKDOWN", h2Font()));
            doc.add(new Paragraph(" "));

            // Group returns by invoice number for quick lookup
            Map<String, List<ItemReturn>> returnsByInvoice = returns.stream()
                    .collect(Collectors.groupingBy(r -> r.getInvoice().getInvoiceNumber()));

            // Group invoices by a "customer key" (customerId if present, else mobile, else name+"walk-in")
            Map<String, List<Invoice>> grouped = new LinkedHashMap<>();
            Map<String, String> displayNames = new LinkedHashMap<>();
            Map<String, String> displayMobiles = new LinkedHashMap<>();

            for (Invoice inv : invoices) {
                String key;
                String name = inv.getCustomerName() != null ? inv.getCustomerName() : "Walk-in Customer";
                String mobile = inv.getCustomerMobile();

                if (inv.getCustomer() != null) {
                    key = "C" + inv.getCustomer().getId();
                } else if (mobile != null && !mobile.isBlank()) {
                    key = "M" + mobile;
                } else {
                    // No reliable key — each such invoice is its own "customer" entry
                    key = "INV" + inv.getId();
                }

                grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(inv);
                displayNames.putIfAbsent(key, name);
                displayMobiles.putIfAbsent(key, mobile);
            }

            // Sort groups by total spend (descending) for a nicer report
            List<Map.Entry<String, List<Invoice>>> sortedGroups = grouped.entrySet().stream()
                    .sorted((a, b) -> {
                        BigDecimal sa = a.getValue().stream().map(Invoice::getGrandTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
                        BigDecimal sb = b.getValue().stream().map(Invoice::getGrandTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
                        return sb.compareTo(sa);
                    })
                    .collect(Collectors.toList());

            for (Map.Entry<String, List<Invoice>> entry : sortedGroups) {
                String key = entry.getKey();
                List<Invoice> custInvoices = entry.getValue();
                String name = displayNames.get(key);
                String mobile = displayMobiles.get(key);

                writeCustomerBlock(doc, name, mobile, custInvoices, returnsByInvoice);
            }

            // ── Grand total ──────────────────────────────────────────────
            doc.add(new Paragraph(" "));
            writeGrandTotal(doc, totals);

            // ── Top selling items (kept) ────────────────────────────────
            writeTopSelling(doc, start, end);

            // ── Profit summary (admin only) ─────────────────────────────
            writeProfitSummary(doc, startDate);

            writeFooter(doc, settings);

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("Failed to generate report PDF: " + e.getMessage());
        }
    }

    /**
     * Renders one customer's invoices + returns as a bordered block.
     */
    private void writeCustomerBlock(Document doc, String name, String mobile,
                                    List<Invoice> custInvoices, Map<String, List<ItemReturn>> returnsByInvoice) throws DocumentException {

        // Outer container table (single cell with border) so the whole block
        // visually groups together.
        PdfPTable outer = new PdfPTable(1);
        outer.setWidthPercentage(100);
        outer.setSpacingBefore(6);
        outer.setSpacingAfter(6);

        PdfPCell outerCell = new PdfPCell();
        outerCell.setPadding(8);
        outerCell.setBorderColor(new Color(210, 210, 210));

        // Customer name header
        String headerText = name + (mobile != null && !mobile.isBlank() ? "  (" + mobile + ")" : "");
        Paragraph custHeader = new Paragraph(headerText, h3Font());
        outerCell.addElement(custHeader);
        outerCell.addElement(new Paragraph(" ", smallFont()));

        BigDecimal custNet = BigDecimal.ZERO;
        BigDecimal custRefund = BigDecimal.ZERO;

        for (Invoice inv : custInvoices) {
            // Invoice line: number, date/time
            Paragraph invHeader = new Paragraph(
                    inv.getInvoiceNumber() + "  —  " + inv.getInvoiceDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")),
                    boldFont());
            outerCell.addElement(invHeader);

            // Items table
            PdfPTable itemsTable = new PdfPTable(new float[]{4, 1, 1.5f, 1.5f, 1.5f});
            itemsTable.setWidthPercentage(100);
            itemsTable.setSpacingBefore(2);
            for (String h : new String[]{"Item", "Qty", "Tag Price", "Discount", "Line Total"}) {
                PdfPCell c = new PdfPCell(new Phrase(h, headerFont()));
                c.setBackgroundColor(BRAND_COLOR);
                c.setPadding(4);
                itemsTable.addCell(c);
            }
            for (InvoiceItem item : inv.getItems()) {
                String desc = item.getDesignName();
                String variant = Arrays.asList(item.getColor(), item.getSize()).stream()
                        .filter(Objects::nonNull).filter(s -> !s.isBlank())
                        .collect(Collectors.joining(" / "));
                if (!variant.isBlank()) desc += " (" + variant + ")";

                itemsTable.addCell(cell(desc, normalFont(), Element.ALIGN_LEFT));
                itemsTable.addCell(cell(String.valueOf(item.getQuantity()), normalFont(), Element.ALIGN_CENTER));
                itemsTable.addCell(cell(fmt(item.getUnitPrice()), normalFont(), Element.ALIGN_RIGHT));
                itemsTable.addCell(cell(fmt(item.getDiscountAmount()), normalFont(), Element.ALIGN_RIGHT));
                itemsTable.addCell(cell(fmt(item.getLineTotal()), normalFont(), Element.ALIGN_RIGHT));
            }
            outerCell.addElement(itemsTable);

            // Invoice totals
            PdfPTable invTotals = new PdfPTable(new float[]{4, 2});
            invTotals.setWidthPercentage(60);
            invTotals.setHorizontalAlignment(Element.ALIGN_RIGHT);
            invTotals.setSpacingBefore(2);
            addPlainRow(invTotals, "Subtotal (Tag Price Total)", fmt(inv.getSubtotal()), normalFont());
            if (inv.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                addPlainRow(invTotals, "Invoice Discount", "- " + fmt(inv.getDiscountAmount()), normalFont());
            }
            if (inv.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
                addPlainRow(invTotals, "Tax", fmt(inv.getTaxAmount()), normalFont());
            }
            addPlainRow(invTotals, "Amount Paid", fmt(inv.getGrandTotal()), boldFont());
            outerCell.addElement(invTotals);

            custNet = custNet.add(inv.getGrandTotal());

            // Returns for this invoice
            List<ItemReturn> invReturns = returnsByInvoice.getOrDefault(inv.getInvoiceNumber(), Collections.emptyList());
            if (!invReturns.isEmpty()) {
                Paragraph retHeader = new Paragraph("Returns for this invoice:", boldFont());
                retHeader.setSpacingBefore(4);
                outerCell.addElement(retHeader);

                PdfPTable retTable = new PdfPTable(new float[]{4, 1, 2, 3});
                retTable.setWidthPercentage(100);
                retTable.setSpacingBefore(2);
                for (String h : new String[]{"Item Returned", "Qty", "Refund", "Reason"}) {
                    PdfPCell c = new PdfPCell(new Phrase(h, headerFont()));
                    c.setBackgroundColor(WARNING_AMBER);
                    c.setPadding(4);
                    retTable.addCell(c);
                }
                for (ItemReturn r : invReturns) {
                    String desc = r.getInvoiceItem().getDesignName();
                    String variant = Arrays.asList(r.getInvoiceItem().getColor(), r.getInvoiceItem().getSize()).stream()
                            .filter(Objects::nonNull).filter(s -> !s.isBlank())
                            .collect(Collectors.joining(" / "));
                    if (!variant.isBlank()) desc += " (" + variant + ")";

                    retTable.addCell(cell(desc, normalFont(), Element.ALIGN_LEFT));
                    retTable.addCell(cell(String.valueOf(r.getQuantity()), normalFont(), Element.ALIGN_CENTER));
                    retTable.addCell(cell("- " + fmt(r.getRefundAmount()), refundFont(), Element.ALIGN_RIGHT));
                    retTable.addCell(cell(r.getReason() != null ? r.getReason() : "-", normalFont(), Element.ALIGN_LEFT));

                    custRefund = custRefund.add(r.getRefundAmount());
                }
                outerCell.addElement(retTable);
            }

            outerCell.addElement(new Paragraph(" ", smallFont()));
        }

        // Per-customer net total (only if there's more than one invoice or any return,
        // otherwise it just repeats "Amount Paid" above)
        if (custInvoices.size() > 1 || custRefund.compareTo(BigDecimal.ZERO) > 0) {
            PdfPTable netTable = new PdfPTable(new float[]{4, 2});
            netTable.setWidthPercentage(60);
            netTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            addPlainRow(netTable, "Total Paid (all invoices)", fmt(custNet), normalFont());
            if (custRefund.compareTo(BigDecimal.ZERO) > 0) {
                addPlainRow(netTable, "Total Refunded", "- " + fmt(custRefund), refundFont());
            }
            addPlainRow(netTable, "Net for this customer", fmt(custNet.subtract(custRefund)), boldFont());
            outerCell.addElement(netTable);
        }

        outer.addCell(outerCell);
        doc.add(outer);
    }

    // ──────────────────────────────────────────────────────────────
    // SUMMARY report (Monthly / Yearly / multi-day Custom): condensed
    // ──────────────────────────────────────────────────────────────

    private byte[] buildSummaryReportPdf(ShopSettings settings, ReportType reportType,
                                         LocalDate startDate, LocalDate endDate,
                                         List<Invoice> invoices, List<ItemReturn> returns,
                                         LocalDateTime start, LocalDateTime end) {

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 50, 50);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            writeShopHeader(doc, settings);
            writeReportTitle(doc, reportType, startDate, endDate, reportType.name() + " SALES REPORT");

            ReportTotals totals = computeTotals(invoices, returns);
            writeOverallSummary(doc, totals, invoices.size());

            // ── Daily breakdown table ────────────────────────────────────
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("DAY-WISE BREAKDOWN", h2Font()));
            doc.add(new Paragraph(" "));

            Map<LocalDate, BigDecimal> salesByDay = new TreeMap<>();
            Map<LocalDate, BigDecimal> discountByDay = new TreeMap<>();
            Map<LocalDate, Integer> invoiceCountByDay = new TreeMap<>();
            Map<LocalDate, BigDecimal> refundByDay = new TreeMap<>();
            Map<LocalDate, Integer> returnQtyByDay = new TreeMap<>();

            for (Invoice inv : invoices) {
                LocalDate d = inv.getInvoiceDate().toLocalDate();
                salesByDay.merge(d, inv.getGrandTotal(), BigDecimal::add);
                discountByDay.merge(d, inv.getDiscountAmount(), BigDecimal::add);
                invoiceCountByDay.merge(d, 1, Integer::sum);
            }
            for (ItemReturn r : returns) {
                LocalDate d = r.getCreatedAt().toLocalDate();
                refundByDay.merge(d, r.getRefundAmount(), BigDecimal::add);
                returnQtyByDay.merge(d, r.getQuantity(), Integer::sum);
            }

            PdfPTable dayTable = new PdfPTable(new float[]{2, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f});
            dayTable.setWidthPercentage(100);
            for (String h : new String[]{"Date", "Invoices", "Net Sales", "Discounts", "Returns", "Net Revenue"}) {
                PdfPCell c = new PdfPCell(new Phrase(h, headerFont()));
                c.setBackgroundColor(BRAND_COLOR);
                c.setPadding(5);
                dayTable.addCell(c);
            }

            for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
                BigDecimal sales = salesByDay.getOrDefault(d, BigDecimal.ZERO);
                BigDecimal discount = discountByDay.getOrDefault(d, BigDecimal.ZERO);
                BigDecimal refund = refundByDay.getOrDefault(d, BigDecimal.ZERO);
                int invCount = invoiceCountByDay.getOrDefault(d, 0);

                // Skip fully-empty days for very long ranges (yearly) to keep it readable
                if (invCount == 0 && refund.compareTo(BigDecimal.ZERO) == 0
                        && (reportType == ReportType.YEARLY)) {
                    continue;
                }

                dayTable.addCell(cell(d.format(DateTimeFormatter.ofPattern("dd MMM yyyy")), normalFont(), Element.ALIGN_LEFT));
                dayTable.addCell(cell(String.valueOf(invCount), normalFont(), Element.ALIGN_CENTER));
                dayTable.addCell(cell(fmt(sales), normalFont(), Element.ALIGN_RIGHT));
                dayTable.addCell(cell(fmt(discount), normalFont(), Element.ALIGN_RIGHT));
                dayTable.addCell(cell(refund.compareTo(BigDecimal.ZERO) > 0 ? "- " + fmt(refund) : "-", refundFont(), Element.ALIGN_RIGHT));
                dayTable.addCell(cell(fmt(sales.subtract(refund)), boldFont(), Element.ALIGN_RIGHT));
            }
            doc.add(dayTable);

            // ── Grand total ──────────────────────────────────────────────
            doc.add(new Paragraph(" "));
            writeGrandTotal(doc, totals);

            // ── Top selling items ────────────────────────────────────────
            writeTopSelling(doc, start, end);

            // ── Profit summary (admin only) ─────────────────────────────
            writeProfitSummary(doc, startDate);

            writeFooter(doc, settings);

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("Failed to generate report PDF: " + e.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Shared sections
    // ──────────────────────────────────────────────────────────────

    private void writeShopHeader(Document doc, ShopSettings settings) throws DocumentException {
        Paragraph shopName = new Paragraph(settings.getShopName(), titleFont());
        shopName.setAlignment(Element.ALIGN_CENTER);
        doc.add(shopName);
        if (settings.getShopAddress() != null) {
            Paragraph addr = new Paragraph(settings.getShopAddress(), normalFont());
            addr.setAlignment(Element.ALIGN_CENTER);
            doc.add(addr);
        }
        StringBuilder contact = new StringBuilder();
        if (settings.getMobileNumber() != null) contact.append("Mobile: ").append(settings.getMobileNumber());
        if (settings.getGstNumber() != null) {
            if (contact.length() > 0) contact.append("    ");
            contact.append("GSTIN: ").append(settings.getGstNumber());
        }
        if (contact.length() > 0) {
            Paragraph c = new Paragraph(contact.toString(), normalFont());
            c.setAlignment(Element.ALIGN_CENTER);
            doc.add(c);
        }
        doc.add(new Paragraph(" "));
        doc.add(new com.lowagie.text.pdf.draw.LineSeparator());
        doc.add(new Paragraph(" "));
    }

    private void writeReportTitle(Document doc, ReportType reportType, LocalDate startDate, LocalDate endDate, String title) throws DocumentException {
        Paragraph rTitle = new Paragraph(title, h2Font());
        rTitle.setAlignment(Element.ALIGN_CENTER);
        doc.add(rTitle);

        String periodText = startDate.equals(endDate)
                ? "Date: " + startDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                : "Period: " + startDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                  + "  to  " + endDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
        Paragraph period = new Paragraph(periodText, normalFont());
        period.setAlignment(Element.ALIGN_CENTER);
        doc.add(period);

        Paragraph meta = new Paragraph(
                "Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"))
                        + "   |   By: " + securityUtils.getCurrentUsername(), smallFont());
        meta.setAlignment(Element.ALIGN_CENTER);
        doc.add(meta);
        doc.add(new Paragraph(" "));
    }

    /**
     * The headline summary block:
     *   Gross Sales (Tag Price)  ->  Discounts  ->  Net Sales (Collected)  ->  Returns  ->  Final Net Revenue
     */
    private void writeOverallSummary(Document doc, ReportTotals totals, int invoiceCount) throws DocumentException {
        doc.add(new Paragraph("SALES SUMMARY", h2Font()));
        doc.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(new float[]{4, 2});
        table.setWidthPercentage(65);
        table.setHorizontalAlignment(Element.ALIGN_LEFT);

        addSummaryRow(table, "Total Invoices", String.valueOf(invoiceCount), normalFont(), normalFont());
        addSummaryRow(table, "Gross Sales (Tag Price Total)", fmt(totals.grossSales), normalFont(), normalFont());
        addSummaryRow(table, "Discounts Given", "- " + fmt(totals.totalDiscounts), normalFont(), refundFont());
        addSummaryRow(table, "Net Sales (Amount Collected)", fmt(totals.netSales), boldFont(), boldFont());

        if (totals.totalRefunds.compareTo(BigDecimal.ZERO) > 0) {
            addSummaryRow(table, "Returns / Refunds (" + totals.totalReturnedQty + " item(s))",
                    "- " + fmt(totals.totalRefunds), normalFont(), refundFont());
        }

        // Final net revenue — highlighted
        PdfPCell labelCell = new PdfPCell(new Phrase("FINAL NET REVENUE", totalFont()));
        labelCell.setPadding(6);
        labelCell.setBackgroundColor(LIGHT_GREY);
        PdfPCell valueCell = new PdfPCell(new Phrase(fmt(totals.finalNetRevenue()), successFont()));
        valueCell.setPadding(6);
        valueCell.setBackgroundColor(LIGHT_GREY);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(labelCell);
        table.addCell(valueCell);

        doc.add(table);
    }

    private void writeGrandTotal(Document doc, ReportTotals totals) throws DocumentException {
        doc.add(new Paragraph("GRAND TOTAL", h2Font()));
        doc.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(new float[]{4, 2});
        table.setWidthPercentage(60);
        table.setHorizontalAlignment(Element.ALIGN_LEFT);

        addSummaryRow(table, "Total Collected (after discounts)", fmt(totals.netSales), normalFont(), normalFont());
        if (totals.totalRefunds.compareTo(BigDecimal.ZERO) > 0) {
            addSummaryRow(table, "Total Refunded", "- " + fmt(totals.totalRefunds), normalFont(), refundFont());
        }

        PdfPCell labelCell = new PdfPCell(new Phrase("GRAND NET TOTAL", totalFont()));
        labelCell.setPadding(6);
        labelCell.setBackgroundColor(LIGHT_GREY);
        PdfPCell valueCell = new PdfPCell(new Phrase(fmt(totals.finalNetRevenue()), successFont()));
        valueCell.setPadding(6);
        valueCell.setBackgroundColor(LIGHT_GREY);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(labelCell);
        table.addCell(valueCell);

        doc.add(table);
        doc.add(new Paragraph(" "));
    }

    private void writeTopSelling(Document doc, LocalDateTime start, LocalDateTime end) throws DocumentException {
        List<Object[]> topDesigns = invoiceItemRepository.findTopSellingDesigns(start, end);
        if (!topDesigns.isEmpty()) {
            doc.add(new Paragraph("TOP SELLING DESIGNS", h2Font()));
            doc.add(new Paragraph(" "));
            PdfPTable dTable = new PdfPTable(new float[]{4, 2, 2});
            dTable.setWidthPercentage(80);
            for (String h : new String[]{"Design", "Qty Sold", "Revenue"}) {
                PdfPCell c = new PdfPCell(new Phrase(h, headerFont()));
                c.setBackgroundColor(BRAND_COLOR); c.setPadding(5);
                dTable.addCell(c);
            }
            int rank = 1;
            for (Object[] row : topDesigns) {
                if (rank++ > 10) break;
                dTable.addCell(cell(String.valueOf(row[0]), normalFont(), Element.ALIGN_LEFT));
                dTable.addCell(cell(String.valueOf(row[1]), normalFont(), Element.ALIGN_CENTER));
                dTable.addCell(cell("Rs. " + row[2], normalFont(), Element.ALIGN_RIGHT));
            }
            doc.add(dTable);
            doc.add(new Paragraph(" "));
        }

        List<Object[]> topColors = invoiceItemRepository.findTopSellingColors(start, end);
        List<Object[]> topSizes  = invoiceItemRepository.findTopSellingSizes(start, end);
        if (!topColors.isEmpty() || !topSizes.isEmpty()) {
            doc.add(new Paragraph("TOP SELLING COLOURS & SIZES", h2Font()));
            doc.add(new Paragraph(" "));
            PdfPTable csTable = new PdfPTable(new float[]{3, 2, 1, 3, 2});
            csTable.setWidthPercentage(90);
            for (String h : new String[]{"Colour", "Qty", "", "Size", "Qty"}) {
                PdfPCell c = new PdfPCell(new Phrase(h, headerFont()));
                c.setBackgroundColor(BRAND_COLOR); c.setPadding(4);
                csTable.addCell(c);
            }
            int max = Math.max(topColors.size(), topSizes.size());
            for (int i = 0; i < Math.min(max, 8); i++) {
                if (i < topColors.size()) {
                    csTable.addCell(cell(String.valueOf(topColors.get(i)[0]), normalFont(), Element.ALIGN_LEFT));
                    csTable.addCell(cell(String.valueOf(topColors.get(i)[1]), normalFont(), Element.ALIGN_CENTER));
                } else { csTable.addCell(emptyCell()); csTable.addCell(emptyCell()); }
                csTable.addCell(emptyCell());
                if (i < topSizes.size()) {
                    csTable.addCell(cell(String.valueOf(topSizes.get(i)[0]), normalFont(), Element.ALIGN_LEFT));
                    csTable.addCell(cell(String.valueOf(topSizes.get(i)[1]), normalFont(), Element.ALIGN_CENTER));
                } else { csTable.addCell(emptyCell()); csTable.addCell(emptyCell()); }
            }
            doc.add(csTable);
            doc.add(new Paragraph(" "));
        }
    }

    private void writeProfitSummary(Document doc, LocalDate startDate) throws DocumentException {
        if (!securityUtils.isAdmin()) return;

        int yr = startDate.getYear();
        int mo = startDate.getMonthValue();
        Optional<MonthlyProfit> profitOpt = profitRepository.findByMonthAndYear(mo, yr);
        if (profitOpt.isEmpty()) return;

        MonthlyProfit p = profitOpt.get();
        doc.add(new Paragraph("PROFIT SUMMARY (Admin)", h2Font()));
        doc.add(new Paragraph(" "));
        PdfPTable pTable = new PdfPTable(new float[]{4, 2});
        pTable.setWidthPercentage(60);
        pTable.setHorizontalAlignment(Element.ALIGN_LEFT);
        addSummaryRow(pTable, "Total Sales (Manual Entry)", fmt(p.getTotalSales()), normalFont(), normalFont());
        addSummaryRow(pTable, "Production Cost", fmt(p.getProductionCost()), normalFont(), normalFont());
        addSummaryRow(pTable, "Other Expenses", fmt(p.getOtherExpenses()), normalFont(), normalFont());
        addSummaryRow(pTable, "Net Profit", fmt(p.getNetProfit()), boldFont(), boldFont());
        doc.add(pTable);
        doc.add(new Paragraph(" "));
    }

    private void writeFooter(Document doc, ShopSettings settings) throws DocumentException {
        doc.add(new com.lowagie.text.pdf.draw.LineSeparator());
        if (settings.getFooterMessage() != null) {
            Paragraph footer = new Paragraph(settings.getFooterMessage(), smallFont());
            footer.setAlignment(Element.ALIGN_CENTER);
            doc.add(footer);
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Small helpers
    // ──────────────────────────────────────────────────────────────

    private PdfPCell cell(String text, Font font, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setPadding(4);
        c.setHorizontalAlignment(align);
        return c;
    }

    private PdfPCell emptyCell() {
        PdfPCell c = new PdfPCell(new Phrase(""));
        c.setPadding(4);
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }

    private void addSummaryRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setPadding(5);
        labelCell.setBorder(Rectangle.NO_BORDER);
        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setPadding(5);
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addPlainRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        labelCell.setPadding(3);
        labelCell.setBorder(Rectangle.NO_BORDER);
        PdfPCell valueCell = new PdfPCell(new Phrase(value, font));
        valueCell.setPadding(3);
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    // ──────────────────────────────────────────────────────────────
    // Report history (unchanged)
    // ──────────────────────────────────────────────────────────────

    private void saveReportHistory(ReportType type, LocalDate start, LocalDate end, byte[] pdfBytes) {
        try {
            User user = securityUtils.getCurrentUser();
            String fileName = type.name().toLowerCase() + "_" + start + "_" + end + "_" +
                    System.currentTimeMillis() + ".pdf";
            Path dir = Paths.get(reportDir);
            Files.createDirectories(dir);
            Path filePath = dir.resolve(fileName);
            Files.write(filePath, pdfBytes);

            ReportHistory history = ReportHistory.builder()
                    .reportType(type).startDate(start).endDate(end)
                    .generatedBy(user).fileName(fileName).filePath(filePath.toString())
                    .build();
            reportHistoryRepository.save(history);
        } catch (Exception e) {
            log.warn("Could not save report history: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportHistoryResponse> getReportHistory() {
        return reportHistoryRepository.findAllByOrderByGeneratedAtDesc().stream()
                .map(this::toHistoryResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadReport(Long reportId) {
        ReportHistory history = reportHistoryRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", "id", reportId));
        try {
            return Files.readAllBytes(Paths.get(history.getFilePath()));
        } catch (Exception e) {
            throw new BusinessException("Report file not found on disk. Please regenerate.");
        }
    }

    @Override
    public void deleteReport(Long reportId) {
        ReportHistory history = reportHistoryRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", "id", reportId));
        try { Files.deleteIfExists(Paths.get(history.getFilePath())); } catch (Exception ignored) {}
        reportHistoryRepository.delete(history);
    }

    private ReportHistoryResponse toHistoryResponse(ReportHistory h) {
        ReportHistoryResponse r = new ReportHistoryResponse();
        r.setId(h.getId());
        r.setReportType(h.getReportType());
        r.setStartDate(h.getStartDate());
        r.setEndDate(h.getEndDate());
        r.setGeneratedBy(h.getGeneratedBy() != null ? h.getGeneratedBy().getUsername() : null);
        r.setGeneratedAt(h.getGeneratedAt());
        r.setFileName(h.getFileName());
        return r;
    }
}
