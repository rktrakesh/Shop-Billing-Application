package com.shopbilling.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;
import java.nio.file.*;

@Configuration
@Slf4j
public class AppConfig {

    @Value("${app.upload-dir:./uploads}")   private String uploadDir;
    @Value("${app.backup-dir:./backups}")   private String backupDir;
    @Value("${app.report-dir:./reports}")   private String reportDir;
    @Value("${app.barcode-dir:./barcodes}") private String barcodeDir;

    @PostConstruct
    public void initDirectories() {
        for (String dir : new String[]{uploadDir, backupDir, reportDir, barcodeDir}) {
            try {
                Files.createDirectories(Paths.get(dir));
                log.debug("Directory ready: {}", dir);
            } catch (Exception e) {
                log.warn("Could not create directory {}: {}", dir, e.getMessage());
            }
        }
    }
}
