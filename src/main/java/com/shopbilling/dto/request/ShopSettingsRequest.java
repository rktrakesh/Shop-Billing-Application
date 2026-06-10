package com.shopbilling.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ShopSettingsRequest {
    @NotBlank(message = "Shop name is required")
    private String shopName;
    
    private String shopAddress;
    private String mobileNumber;
    private String email;
    private String gstNumber;
    private String footerMessage;
}
