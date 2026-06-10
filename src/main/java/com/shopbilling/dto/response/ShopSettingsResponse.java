package com.shopbilling.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ShopSettingsResponse {
    private Long id;
    private String shopName;
    private String shopAddress;
    private String mobileNumber;
    private String email;
    private String gstNumber;
    private String footerMessage;
    private String logoPath;
    private LocalDateTime updatedAt;
}
