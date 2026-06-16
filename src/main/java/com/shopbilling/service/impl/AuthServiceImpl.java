package com.shopbilling.service.impl;

import com.shopbilling.dto.request.LoginRequest;
import com.shopbilling.dto.response.AuthResponse;
import com.shopbilling.entity.User;
import com.shopbilling.enums.AuditAction;
import com.shopbilling.exception.UnauthorizedException;
import com.shopbilling.repository.UserRepository;
import com.shopbilling.security.util.JwtUtil;
import com.shopbilling.service.AuditLogService;
import com.shopbilling.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final AuditLogService auditLogService;

    @Override
    public AuthResponse login(LoginRequest request, String ipAddress) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        String token = jwtUtil.generateToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        User user = userRepository.findByUsername(request.getUsername()).orElseThrow();

        auditLogService.log(AuditAction.LOGIN, "User", user.getId(),
                "User logged in from IP: " + ipAddress);

        log.info("User '{}' logged in successfully", request.getUsername());

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .username(user.getUsername())
                .role(user.getRole())
                .fullName(user.getFullName())
                .build();
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        String username;
        try {
            username = jwtUtil.extractUsername(refreshToken);
        } catch (Exception e) {
            throw new UnauthorizedException("Invalid or expired refresh token. Please log in again.");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (!jwtUtil.isRefreshTokenValid(refreshToken, userDetails)) {
            throw new UnauthorizedException("Invalid or expired refresh token. Please log in again.");
        }

        User user = userRepository.findByUsername(username).orElseThrow();

        String newAccessToken = jwtUtil.generateToken(userDetails);
        // Issue a new refresh token too ("rotation") so a long-lived session
        // keeps extending as long as the user stays active.
        String newRefreshToken = jwtUtil.generateRefreshToken(userDetails);

        return AuthResponse.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .username(user.getUsername())
                .role(user.getRole())
                .fullName(user.getFullName())
                .build();
    }
}