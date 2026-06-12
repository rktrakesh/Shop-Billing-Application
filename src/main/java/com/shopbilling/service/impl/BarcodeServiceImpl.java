package com.shopbilling.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfWriter;
import com.shopbilling.dto.response.ProductVariantResponse;
import com.shopbilling.entity.ProductVariant;
import com.shopbilling.enums.AuditAction;
import com.shopbilling.exception.BusinessException;
import com.shopbilling.exception.ResourceNotFoundException;
import com.shopbilling.mapper.ProductVariantMapper;
import com.shopbilling.repository.ProductVariantRepository;
import com.shopbilling.service.AuditLogService;
import com.shopbilling.service.BarcodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BarcodeServiceImpl implements BarcodeService {

    private final ProductVariantRepository variantRepository;
    private final ProductVariantMapper variantMapper;
    private final AuditLogService auditLogService;

    @Value("${app.barcode-dir:./barcodes}")
    private String barcodeDir;

    @Override
    public ProductVariantResponse generateBarcode(Long variantId) {
        ProductVariant variant = getVariantById(variantId);
        if (variant.getBarcode() != null && !variant.getBarcode().isEmpty()) {
            throw new BusinessException("Barcode already exists. Use regenerate endpoint to create a new one.");
        }
        return doGenerateBarcode(variant);
    }

    @Override
    public ProductVariantResponse regenerateBarcode(Long variantId) {
        ProductVariant variant = getVariantById(variantId);
        return doGenerateBarcode(variant);
    }

    private ProductVariantResponse doGenerateBarcode(ProductVariant variant) {
        String barcodeValue = generateUniqueBarcodeValue();
        String imagePath = saveBarcodeImage(barcodeValue, variant.getProductCode());

        variant.setBarcode(barcodeValue);
        variant.setBarcodeImagePath(imagePath);
        ProductVariant saved = variantRepository.save(variant);

        auditLogService.log(AuditAction.BARCODE_GENERATED, "ProductVariant", variant.getId(),
                "Barcode generated: " + barcodeValue + " for " + variant.getProductCode());

        log.info("Barcode generated for variant: {}", variant.getProductCode());
        return variantMapper.toResponse(saved);
    }

    @Override
    public byte[] downloadBarcodePng(Long variantId) {
        ProductVariant variant = getVariantById(variantId);
        if (variant.getBarcode() == null) {
            throw new BusinessException("No barcode generated for this product variant");
        }
//        return generateBarcodeImageBytes(variant.getBarcode());
        return generateBarcodeWithDetailsImageBytes(variant);
    }

    @Override
    public byte[] downloadBarcodePdf(Long variantId) {
        ProductVariant variant = getVariantById(variantId);
        if (variant.getBarcode() == null) {
            throw new BusinessException("No barcode generated for this product variant");
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(new Rectangle(200, 100));
            PdfWriter.getInstance(document, baos);
            document.open();

            byte[] imageBytes = generateBarcodeImageBytes(variant.getBarcode());
            Image barcodeImage = Image.getInstance(imageBytes);
            barcodeImage.scaleToFit(180, 60);
            barcodeImage.setAlignment(Image.ALIGN_CENTER);
            document.add(barcodeImage);

            Font font = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL);
            Paragraph p = new Paragraph(variant.getProductCode() + " | " + variant.getBarcode(), font);
            p.setAlignment(Element.ALIGN_CENTER);
            document.add(p);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("Failed to generate barcode PDF: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ProductVariantResponse searchByBarcode(String barcode) {
        ProductVariant variant = variantRepository.findByBarcode(barcode)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "barcode", barcode));
        return variantMapper.toResponse(variant);
    }

    private String generateUniqueBarcodeValue() {
        String barcode;
        do {
            barcode = String.valueOf(100000 + (long)(Math.random() * 900000));
        } while (variantRepository.existsByBarcode(barcode));
        return barcode;
    }

    private byte[] generateBarcodeImageBytes(String barcodeValue) {
        try {
            Code128Writer writer = new Code128Writer();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.MARGIN, 10);
            BitMatrix bitMatrix = writer.encode(barcodeValue, BarcodeFormat.CODE_128, 300, 80, hints);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("Failed to generate barcode image: " + e.getMessage());
        }
    }

    private byte[] generateBarcodeWithDetailsImageBytes(ProductVariant variant) {
        try {
            // ── 1. Raw barcode strip (Code-128 via ZXing) ─────────────────────
            byte[] rawBarcodeBytes = generateBarcodeImageBytes(variant.getBarcode());
            BufferedImage barcodeStrip = ImageIO.read(new ByteArrayInputStream(rawBarcodeBytes));

            // ── 2. Canvas ─────────────────────────────────────────────────────
            int W = 520, H = 290, CARD_MARGIN = 6;
            BufferedImage canvas = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = canvas.createGraphics();

            // Anti-aliasing
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);

            // Transparent background
            g.setColor(new Color(0, 0, 0, 0));
            g.fillRect(0, 0, W, H);

            // ── 3. Drop shadow ─────────────────────────────────────────────────
            g.setColor(new Color(0, 0, 0, 40));
            g.fill(new java.awt.geom.RoundRectangle2D.Float(
                    CARD_MARGIN + 3, CARD_MARGIN + 3,
                    W - CARD_MARGIN * 2, H - CARD_MARGIN * 2, 24, 24));

            // ── 4. White card ──────────────────────────────────────────────────
            g.setColor(Color.WHITE);
            g.fill(new java.awt.geom.RoundRectangle2D.Float(
                    CARD_MARGIN, CARD_MARGIN,
                    W - CARD_MARGIN * 2, H - CARD_MARGIN * 2, 24, 24));

            // Card border
            g.setColor(new Color(210, 210, 210));
            g.setStroke(new BasicStroke(1.5f));
            g.draw(new java.awt.geom.RoundRectangle2D.Float(
                    CARD_MARGIN, CARD_MARGIN,
                    W - CARD_MARGIN * 2, H - CARD_MARGIN * 2, 24, 24));

            // ── 5. Fonts ───────────────────────────────────────────────────────
            java.awt.Font fontBrand  = new java.awt.Font("SansSerif", java.awt.Font.BOLD,  18);
            java.awt.Font fontBold   = new java.awt.Font("SansSerif", java.awt.Font.BOLD,  13);
            java.awt.Font fontPlain  = new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 13);
            java.awt.Font fontSmall  = new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 10);
            java.awt.Font fontPrice  = new java.awt.Font("SansSerif", java.awt.Font.BOLD,  30);
            java.awt.Font fontBnum   = new java.awt.Font("SansSerif", java.awt.Font.BOLD,  13);

            FontMetrics fm;

            // ── 6. Design / brand name centred at top ─────────────────────────
            g.setFont(fontBrand);
            g.setColor(new Color(20, 20, 20));
            fm = g.getFontMetrics();
            String brandName = "R K T";
            g.drawString(brandName, (W - fm.stringWidth(brandName)) / 2, 36);

            // Top divider
            g.setColor(new Color(210, 210, 210));
            g.setStroke(new BasicStroke(1f));
            g.drawLine(22, 46, W - 22, 46);

            // ── 7. Barcode strip centred ───────────────────────────────────────
            int bcX = (W - barcodeStrip.getWidth()) / 2;
            int bcY = 54;
            g.drawImage(barcodeStrip, bcX, bcY, null);

            // Barcode number below strip
            g.setFont(fontBnum);
            g.setColor(new Color(30, 30, 30));
            fm = g.getFontMetrics();
            String barcodeVal = variant.getBarcode();
            g.drawString(barcodeVal,
                    (W - fm.stringWidth(barcodeVal)) / 2,
                    bcY + barcodeStrip.getHeight() + 16);

            // Bottom divider
            int divY = bcY + barcodeStrip.getHeight() + 26;
            g.setColor(new Color(210, 210, 210));
            g.drawLine(22, divY, W - 22, divY);

            // ── 8. Product details (bottom-left) ──────────────────────────────
            int dy = divY + 16;

            String[][] rows = {
                    {"Design:",variant.getProduct().getDesignName()},
                    {"Size:",  variant.getSize()},
                    {"Color:", variant.getColor()}
            };

            for (String[] row : rows) {
                g.setFont(fontBold);
                g.setColor(new Color(70, 70, 70));
                g.drawString(row[0], 24, dy);

                g.setFont(fontPlain);
                g.setColor(new Color(40, 40, 40));
                g.drawString(row[1] != null ? row[1] : "-", 80, dy);

                dy += 22;
            }

            // ── 9. Price (bottom-right) ────────────────────────────────────────
            // Small label above price
            g.setFont(fontSmall);
            g.setColor(new Color(130, 130, 130));
            fm = g.getFontMetrics();
            String mrpLabel = "MRP (Incl. of all taxes)";
            g.drawString(mrpLabel, W - 22 - fm.stringWidth(mrpLabel), divY + 16);

            // Price in large bold
            g.setFont(fontPrice);
            g.setColor(new Color(15, 15, 15));
            fm = g.getFontMetrics();
            String priceStr = "\u20B9" + variant.getSellingPrice().intValue();
            g.drawString(priceStr, W - 22 - fm.stringWidth(priceStr), divY + 48);

            g.dispose();

            // ── 10. Write PNG bytes ────────────────────────────────────────────
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(canvas, "PNG", baos);
            return baos.toByteArray();

        } catch (Exception e) {
            throw new BusinessException("Failed to generate barcode sticker: " + e.getMessage());
        }
    }

    private String saveBarcodeImage(String barcodeValue, String productCode) {
        try {
            Path dir = Paths.get(barcodeDir);
            Files.createDirectories(dir);
            String fileName = productCode.replaceAll("[^a-zA-Z0-9]", "_") + "_" + barcodeValue + ".png";
            Path imagePath = dir.resolve(fileName);
            byte[] imageBytes = generateBarcodeImageBytes(barcodeValue);
            Files.write(imagePath, imageBytes);
            return imagePath.toString();
        } catch (Exception e) {
            log.warn("Could not save barcode image to disk: {}", e.getMessage());
            return barcodeDir + "/" + productCode + "_" + barcodeValue + ".png";
        }
    }

    private ProductVariant getVariantById(Long id) {
        return variantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "id", id));
    }
}
