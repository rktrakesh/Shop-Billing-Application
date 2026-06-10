package com.shopbilling.dto.response;

import com.shopbilling.enums.Role;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserResponse {
    private Long id;
    private String username;
    private Role role;
    private boolean active;
    private String fullName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
