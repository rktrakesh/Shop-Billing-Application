package com.shopbilling.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BarcodeProductSearchRequest {
    @NotBlank
    private String barcode;
}
