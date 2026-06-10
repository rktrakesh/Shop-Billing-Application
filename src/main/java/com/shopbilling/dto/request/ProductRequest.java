package com.shopbilling.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProductRequest {
    @NotBlank(message = "Design name is required")
    private String designName;
    
    private String description;
    
    @NotBlank(message = "Category is required")
    private String category;
    
    @NotBlank(message = "Print type is required")
    private String printType;
    
    private boolean active = true;
}
