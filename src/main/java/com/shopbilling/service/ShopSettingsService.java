package com.shopbilling.service;

import com.shopbilling.dto.request.ShopSettingsRequest;
import com.shopbilling.dto.response.ShopSettingsResponse;

public interface ShopSettingsService {
    ShopSettingsResponse getSettings();
    ShopSettingsResponse updateSettings(ShopSettingsRequest request);
}
