package com.shopbilling.service.impl;

import com.shopbilling.dto.request.ShopSettingsRequest;
import com.shopbilling.dto.response.ShopSettingsResponse;
import com.shopbilling.entity.ShopSettings;
import com.shopbilling.enums.AuditAction;
import com.shopbilling.repository.ShopSettingsRepository;
import com.shopbilling.service.AuditLogService;
import com.shopbilling.service.ShopSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ShopSettingsServiceImpl implements ShopSettingsService {

    private final ShopSettingsRepository settingsRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public ShopSettingsResponse getSettings() {
        ShopSettings settings = settingsRepository.findFirstByOrderByIdAsc()
                .orElse(ShopSettings.builder()
                        .shopName("My Shop")
                        .footerMessage("Thank you for your purchase!")
                        .build());
        return toResponse(settings);
    }

    @Override
    public ShopSettingsResponse updateSettings(ShopSettingsRequest request) {
        ShopSettings settings = settingsRepository.findFirstByOrderByIdAsc()
                .orElse(new ShopSettings());
        settings.setShopName(request.getShopName());
        settings.setShopAddress(request.getShopAddress());
        settings.setMobileNumber(request.getMobileNumber());
        settings.setEmail(request.getEmail());
        settings.setGstNumber(request.getGstNumber());
        settings.setFooterMessage(request.getFooterMessage());
        ShopSettings saved = settingsRepository.save(settings);
        auditLogService.log(AuditAction.SETTINGS_UPDATED, "ShopSettings", saved.getId(), "Shop settings updated");
        log.info("Shop settings updated");
        return toResponse(saved);
    }

    private ShopSettingsResponse toResponse(ShopSettings s) {
        ShopSettingsResponse r = new ShopSettingsResponse();
        r.setId(s.getId());
        r.setShopName(s.getShopName());
        r.setShopAddress(s.getShopAddress());
        r.setMobileNumber(s.getMobileNumber());
        r.setEmail(s.getEmail());
        r.setGstNumber(s.getGstNumber());
        r.setFooterMessage(s.getFooterMessage());
        r.setLogoPath(s.getLogoPath());
        r.setUpdatedAt(s.getUpdatedAt());
        return r;
    }
}
