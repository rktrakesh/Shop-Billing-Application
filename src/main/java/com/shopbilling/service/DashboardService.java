package com.shopbilling.service;

import com.shopbilling.dto.response.DashboardResponse;

public interface DashboardService {
    DashboardResponse getAdminDashboard();
    DashboardResponse getManagerDashboard();
    DashboardResponse getUserDashboard();
}
