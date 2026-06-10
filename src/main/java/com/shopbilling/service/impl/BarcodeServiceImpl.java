package com.shopbilling.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import com.lowagie.text.*;
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
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
        return generateBarcodeImageBytes(variant.getBarcode());
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
