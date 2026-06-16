package com.shopbilling.service.impl;

import com.shopbilling.dto.request.MonthlyProfitRequest;
import com.shopbilling.dto.response.MonthlyProfitResponse;
import com.shopbilling.dto.response.ProfitSummaryResponse;
import com.shopbilling.entity.MonthlyProfit;
import com.shopbilling.mapper.MonthlyProfitMapper;
import com.shopbilling.repository.*;
import com.shopbilling.service.MonthlyProfitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.shopbilling.entity.ShopSettings;
import com.shopbilling.exception.BusinessException;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MonthlyProfitServiceImpl implements MonthlyProfitService {

    private final MonthlyProfitRepository profitRepository;
    private final MonthlyProfitMapper profitMapper;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final ShopSettingsRepository shopSettingsRepository;
    private final ItemReturnRepository itemReturnRepository;

    @Override
    public MonthlyProfitResponse createOrUpdateProfit(MonthlyProfitRequest request) {
        MonthlyProfit profit = profitRepository.findByMonthAndYear(request.getMonth(), request.getYear())
                .orElse(new MonthlyProfit());

        profit.setMonth(request.getMonth());
        profit.setYear(request.getYear());
        profit.setTotalSales(request.getTotalSales());
        profit.setProductionCost(request.getProductionCost());
        BigDecimal otherExpenses = request.getOtherExpenses() != null ? request.getOtherExpenses() : BigDecimal.ZERO;
        profit.setOtherExpenses(otherExpenses);
        profit.setNetProfit(request.getTotalSales().subtract(request.getProductionCost()).subtract(otherExpenses));
        profit.setNotes(request.getNotes());

        MonthlyProfit saved = profitRepository.save(profit);
        log.info("Monthly profit saved for {}/{}: net profit = {}", request.getMonth(), request.getYear(), saved.getNetProfit());
        return profitMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public MonthlyProfitResponse getProfitByMonthYear(int month, int year) {
        return profitRepository.findByMonthAndYear(month, year)
                .map(profitMapper::toResponse)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonthlyProfitResponse> getProfitByYear(int year) {
        return profitMapper.toResponseList(profitRepository.findByYearOrderByMonthAsc(year));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonthlyProfitResponse> getAllProfits() {
        return profitMapper.toResponseList(profitRepository.findAll());
    }

    // ── Auto-calculated summaries (from invoices, net of returns) ──
    @Override
    @Transactional(readOnly = true)
    public ProfitSummaryResponse getDailyProfit(LocalDate date) {
        return buildSummary(date, date, date.toString());
    }

    @Override
    @Transactional(readOnly = true)
    public ProfitSummaryResponse getMonthlyProfit(int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        String label = start.getMonth().name() + " " + year;
        return buildSummary(start, end, label);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfitSummaryResponse getYearlyProfit(int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        return buildSummary(start, end, String.valueOf(year));
    }

    // Returns the last N months of summaries, oldest first, for dashboard charts.
    // Uses the 3-letter abbreviated month name (Jan, Feb...) as the period label
    // so it fits neatly on a chart X-axis.
    @Override
    @Transactional(readOnly = true)
    public List<ProfitSummaryResponse> getLastNMonthsSummary(int months) {
        List<ProfitSummaryResponse> result = new ArrayList<>();
        LocalDate now = LocalDate.now();
        // oldest first — e.g. for months=6: [Jan, Feb, Mar, Apr, May, Jun]
        for (int i = months - 1; i >= 0; i--) {
            LocalDate target = now.minusMonths(i);
            LocalDate start = target.withDayOfMonth(1);
            LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
            // Short month label for chart X-axis
            String label = target.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            ProfitSummaryResponse summary = buildSummary(start, end, label);
            result.add(summary);
        }
        return result;
    }


    // Guard against null from aggregation queries (empty date range → null from COALESCE-less JPQL)
    private BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    // ── PDF generation ──────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public byte[] generateProfitPdf(ProfitSummaryResponse summary) {
        ShopSettings settings = shopSettingsRepository.findFirstByOrderByIdAsc()
                .orElse(ShopSettings.builder().shopName("My Shop").build());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Font titleFont  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
            Font h2Font     = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(30, 80, 160));
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK);
            Font boldFont   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);
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

            // ── Title ────────────────────────────────────────────────────
            Paragraph title = new Paragraph("PROFIT SUMMARY REPORT", h2Font);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);

            Paragraph period = new Paragraph("Period: " + summary.getPeriodLabel()
                    + "  (" + summary.getStartDate() + " to " + summary.getEndDate() + ")", normalFont);
            period.setAlignment(Element.ALIGN_CENTER);
            doc.add(period);

            doc.add(new Paragraph("Generated: " + LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")), smallFont));
            doc.add(new Paragraph(" "));

            // ── Summary table ────────────────────────────────────────────
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(70);
            table.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.setWidths(new float[]{3, 2});

            addRow(table, "Total Invoices", String.valueOf(summary.getInvoiceCount()), normalFont, normalFont);
            addRow(table, "Total Sales (Revenue)", "Rs. " + summary.getTotalSales(), normalFont, normalFont);
            addRow(table, "Production Cost", "Rs. " + summary.getProductionCost(), normalFont, normalFont);

            // Net profit row — highlighted
            PdfPCell labelCell = new PdfPCell(new Phrase("Net Profit", boldFont));
            labelCell.setPadding(8);
            labelCell.setBackgroundColor(new Color(230, 245, 233));
            PdfPCell valueCell = new PdfPCell(new Phrase("Rs. " + summary.getNetProfit(), boldFont));
            valueCell.setPadding(8);
            valueCell.setBackgroundColor(new Color(230, 245, 233));
            table.addCell(labelCell);
            table.addCell(valueCell);

            addRow(table, "Profit Margin", summary.getMarginPercent() + " %", normalFont, normalFont);

            doc.add(table);
            doc.add(new Paragraph(" "));

            // ── Footer ───────────────────────────────────────────────────
            doc.add(new com.lowagie.text.pdf.draw.LineSeparator());
            if (settings.getFooterMessage() != null) {
                Paragraph footer = new Paragraph(settings.getFooterMessage(), smallFont);
                footer.setAlignment(Element.ALIGN_CENTER);
                doc.add(footer);
            }

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("Failed to generate profit PDF: " + e.getMessage());
        }
    }

    private void addRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, labelFont));
        c1.setPadding(6);
        PdfPCell c2 = new PdfPCell(new Phrase(value, valueFont));
        c2.setPadding(6);
        table.addCell(c1);
        table.addCell(c2);
    }

    private ProfitSummaryResponse buildSummary(LocalDate start, LocalDate end, String label) {
        LocalDateTime s = start.atStartOfDay();
        LocalDateTime e = end.atTime(23, 59, 59);

        BigDecimal totalSales = nz(invoiceRepository.sumGrandTotalByDateRange(s, e));
        BigDecimal totalReturns = nz(itemReturnRepository.sumRefundsByDateRange(s, e));
        BigDecimal netSales = totalSales.subtract(totalReturns);

        BigDecimal productionCost = nz(invoiceItemRepository.sumProductionCostByDateRange(s, e));
        long invoiceCount = invoiceRepository.countByDateRange(s, e);

        BigDecimal netProfit = netSales.subtract(productionCost);
        BigDecimal margin = netSales.compareTo(BigDecimal.ZERO) > 0
                ? netProfit.divide(netSales, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        return ProfitSummaryResponse.builder()
                .periodLabel(label)
                .startDate(start)
                .endDate(end)
                .totalSales(netSales)          // now net of returns
                .productionCost(productionCost)
                .netProfit(netProfit)
                .marginPercent(margin)
                .invoiceCount(invoiceCount)
                .build();
    }

}
