package com.shopbilling.service;

import com.shopbilling.dto.response.AuditLogResponse;
import com.shopbilling.entity.AuditLog;
import com.shopbilling.enums.AuditAction;

import java.util.List;

public interface AuditLogService {
    void log(AuditAction action, String entityType, Long entityId, String description);
    List<AuditLogResponse> getAllLogs();
    List<AuditLogResponse> getRecentLogs(int limit);
}
