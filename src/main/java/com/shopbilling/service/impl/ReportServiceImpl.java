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
    private final AuditLogService auditLogService;
    private final SecurityUtils securityUtils;

    @Value("${app.report-dir:./reports}")
    private String reportDir;

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
        BigDecimal totalSales = invoices.stream()
                .map(Invoice::getGrandTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDiscounts = invoices.stream()
                .map(Invoice::getDiscountAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgOrderValue = invoices.isEmpty() ? BigDecimal.ZERO :
                totalSales.divide(BigDecimal.valueOf(invoices.size()), 2, RoundingMode.HALF_UP);

        byte[] pdfBytes = buildReportPdf(settings, reportType, startDate, endDate,
                invoices, totalSales, totalDiscounts, avgOrderValue, start, end);

        saveReportHistory(reportType, startDate, endDate, pdfBytes);

        auditLogService.log(AuditAction.REPORT_GENERATED, "Report", null,
                reportType + " report generated: " + startDate + " to " + endDate);

        return pdfBytes;
    }

    private byte[] buildReportPdf(ShopSettings settings, ReportType reportType,
            LocalDate startDate, LocalDate endDate,
            List<Invoice> invoices, BigDecimal totalSales,
            BigDecimal totalDiscounts, BigDecimal avgOrderValue,
            LocalDateTime start, LocalDateTime end) {

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Font titleFont  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
            Font h2Font     = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(30, 80, 160));
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Font smallFont  = FontFactory.getFont(FontFactory.HELVETICA, 8,  Color.DARK_GRAY);

            // ── Shop header ──────────────────────────────────────────────
            Paragraph shopName = new Paragraph(settings.getShopName(), titleFont);
            shopName.setAlignment(Element.ALIGN_CENTER);
            doc.add(shopName);
            if (settings.getShopAddress() != null) {
                Paragraph addr = new Paragraph(settings.getShopAddress(), normalFont);
                addr.setAlignment(Element.ALIGN_CENTER);
                doc.add(addr);
            }
            if (settings.getMobileNumber() != null) {
                Paragraph mob = new Paragraph("Mobile: " + settings.getMobileNumber(), normalFont);
                mob.setAlignment(Element.ALIGN_CENTER);
                doc.add(mob);
            }
            doc.add(new Paragraph(" "));
            doc.add(new com.lowagie.text.pdf.draw.LineSeparator());
            doc.add(new Paragraph(" "));

            // ── Report meta ───────────────────────────────────────────────
            Paragraph rTitle = new Paragraph(reportType.name() + " SALES REPORT", h2Font);
            rTitle.setAlignment(Element.ALIGN_CENTER);
            doc.add(rTitle);
            doc.add(new Paragraph("Period : " + startDate + "  to  " + endDate, normalFont));
            doc.add(new Paragraph("Generated : " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")), smallFont));
            doc.add(new Paragraph("Generated By : " + securityUtils.getCurrentUsername(), smallFont));
            doc.add(new Paragraph(" "));

            // ── Sales summary ─────────────────────────────────────────────
            doc.add(new Paragraph("SALES SUMMARY", h2Font));
            PdfPTable summary = new PdfPTable(2);
            summary.setWidthPercentage(60);
            summary.setHorizontalAlignment(Element.ALIGN_LEFT);
            addSummaryRow(summary, "Total Invoices",   String.valueOf(invoices.size()),       normalFont);
            addSummaryRow(summary, "Total Sales",      "₹" + totalSales,                     normalFont);
            addSummaryRow(summary, "Total Discounts",  "₹" + totalDiscounts,                 normalFont);
            addSummaryRow(summary, "Average Order",    "₹" + avgOrderValue,                  normalFont);
            addSummaryRow(summary, "Total Customers",  String.valueOf(customerRepository.count()), normalFont);
            doc.add(summary);
            doc.add(new Paragraph(" "));

            // ── Invoice list ──────────────────────────────────────────────
            doc.add(new Paragraph("INVOICE DETAILS", h2Font));
            PdfPTable invTable = new PdfPTable(new float[]{2, 2, 3, 2, 2});
            invTable.setWidthPercentage(100);
            String[] invHeaders = {"Invoice No", "Date", "Customer", "Amount", "Discount"};
            for (String h : invHeaders) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(new Color(30, 80, 160));
                cell.setPadding(5);
                invTable.addCell(cell);
            }
            for (Invoice inv : invoices) {
                invTable.addCell(new PdfPCell(new Phrase(inv.getInvoiceNumber(), normalFont)));
                invTable.addCell(new PdfPCell(new Phrase(inv.getInvoiceDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")), normalFont)));
                invTable.addCell(new PdfPCell(new Phrase(inv.getCustomerName() != null ? inv.getCustomerName() : "-", normalFont)));
                invTable.addCell(new PdfPCell(new Phrase("₹" + inv.getGrandTotal(), normalFont)));
                invTable.addCell(new PdfPCell(new Phrase("₹" + inv.getDiscountAmount(), normalFont)));
            }
            doc.add(invTable);
            doc.add(new Paragraph(" "));

            // ── Top selling designs ───────────────────────────────────────
            List<Object[]> topDesigns = invoiceItemRepository.findTopSellingDesigns(start, end);
            if (!topDesigns.isEmpty()) {
                doc.add(new Paragraph("TOP SELLING DESIGNS", h2Font));
                PdfPTable dTable = new PdfPTable(new float[]{4, 2, 2});
                dTable.setWidthPercentage(80);
                for (String h : new String[]{"Design", "Qty Sold", "Revenue"}) {
                    PdfPCell c = new PdfPCell(new Phrase(h, headerFont));
                    c.setBackgroundColor(new Color(30, 80, 160)); c.setPadding(5);
                    dTable.addCell(c);
                }
                int rank = 1;
                for (Object[] row : topDesigns) {
                    if (rank++ > 10) break;
                    dTable.addCell(new PdfPCell(new Phrase(String.valueOf(row[0]), normalFont)));
                    dTable.addCell(new PdfPCell(new Phrase(String.valueOf(row[1]), normalFont)));
                    dTable.addCell(new PdfPCell(new Phrase("₹" + row[2], normalFont)));
                }
                doc.add(dTable);
                doc.add(new Paragraph(" "));
            }

            // ── Top colours & sizes ───────────────────────────────────────
            List<Object[]> topColors = invoiceItemRepository.findTopSellingColors(start, end);
            List<Object[]> topSizes  = invoiceItemRepository.findTopSellingSizes(start, end);
            if (!topColors.isEmpty() || !topSizes.isEmpty()) {
                doc.add(new Paragraph("TOP SELLING COLOURS & SIZES", h2Font));
                PdfPTable csTable = new PdfPTable(new float[]{3, 2, 1, 3, 2});
                csTable.setWidthPercentage(90);
                for (String h : new String[]{"Colour", "Qty", "", "Size", "Qty"}) {
                    PdfPCell c = new PdfPCell(new Phrase(h, headerFont));
                    c.setBackgroundColor(new Color(30, 80, 160)); c.setPadding(4);
                    csTable.addCell(c);
                }
                int max = Math.max(topColors.size(), topSizes.size());
                for (int i = 0; i < Math.min(max, 8); i++) {
                    if (i < topColors.size()) {
                        csTable.addCell(new PdfPCell(new Phrase(String.valueOf(topColors.get(i)[0]), normalFont)));
                        csTable.addCell(new PdfPCell(new Phrase(String.valueOf(topColors.get(i)[1]), normalFont)));
                    } else { csTable.addCell(""); csTable.addCell(""); }
                    csTable.addCell("");
                    if (i < topSizes.size()) {
                        csTable.addCell(new PdfPCell(new Phrase(String.valueOf(topSizes.get(i)[0]), normalFont)));
                        csTable.addCell(new PdfPCell(new Phrase(String.valueOf(topSizes.get(i)[1]), normalFont)));
                    } else { csTable.addCell(""); csTable.addCell(""); }
                }
                doc.add(csTable);
                doc.add(new Paragraph(" "));
            }

            // ── Profit summary (admin only) ───────────────────────────────
            if (securityUtils.isAdmin()) {
                doc.add(new Paragraph("PROFIT SUMMARY", h2Font));
                int yr = startDate.getYear();
                int mo = startDate.getMonthValue();
                profitRepository.findByMonthAndYear(mo, yr).ifPresent(p -> {
                    try {
                        PdfPTable pTable = new PdfPTable(2);
                        pTable.setWidthPercentage(60);
                        pTable.setHorizontalAlignment(Element.ALIGN_LEFT);
                        addSummaryRow(pTable, "Total Sales",       "₹" + p.getTotalSales(),      normalFont);
                        addSummaryRow(pTable, "Production Cost",   "₹" + p.getProductionCost(),  normalFont);
                        addSummaryRow(pTable, "Other Expenses",    "₹" + p.getOtherExpenses(),   normalFont);
                        addSummaryRow(pTable, "Net Profit",        "₹" + p.getNetProfit(),       normalFont);
                        doc.add(pTable);
                    } catch (DocumentException e) {
                        log.warn("Could not add profit table to report PDF");
                    }
                });
                doc.add(new Paragraph(" "));
            }

            // ── Footer ────────────────────────────────────────────────────
            doc.add(new com.lowagie.text.pdf.draw.LineSeparator());
            if (settings.getFooterMessage() != null) {
                Paragraph footer = new Paragraph(settings.getFooterMessage(), smallFont);
                footer.setAlignment(Element.ALIGN_CENTER);
                doc.add(footer);
            }

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("Failed to generate report PDF: " + e.getMessage());
        }
    }

    private void addSummaryRow(PdfPTable table, String label, String value, Font font) {
        table.addCell(new PdfPCell(new Phrase(label, font)));
        table.addCell(new PdfPCell(new Phrase(value, font)));
    }

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
