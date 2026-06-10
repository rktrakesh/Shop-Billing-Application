package com.shopbilling.dto.response;

import com.shopbilling.enums.AuditAction;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditLogResponse {
    private Long id;
    private AuditAction action;
    private String entityType;
    private Long entityId;
    private String description;
    private String performedBy;
    private String ipAddress;
    private LocalDateTime createdAt;
}
