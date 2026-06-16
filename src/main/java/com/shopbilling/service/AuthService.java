package com.shopbilling.service;

import com.shopbilling.dto.request.LoginRequest;
import com.shopbilling.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request, String ipAddress);
    AuthResponse refreshToken(String refreshToken);
}