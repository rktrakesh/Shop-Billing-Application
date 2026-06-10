package com.shopbilling.dto.response;

import com.shopbilling.enums.ReportType;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ReportHistoryResponse {
    private Long id;
    private ReportType reportType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String generatedBy;
    private LocalDateTime generatedAt;
    private String fileName;
}
