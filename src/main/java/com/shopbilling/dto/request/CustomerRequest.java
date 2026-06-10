package com.shopbilling.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomerRequest {
    @NotBlank(message = "Customer name is required")
    private String name;
    
    private String mobileNumber;
    private String address;
}
