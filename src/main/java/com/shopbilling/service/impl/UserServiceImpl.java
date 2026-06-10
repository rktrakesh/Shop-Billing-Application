package com.shopbilling.service.impl;

import com.shopbilling.dto.request.CreateUserRequest;
import com.shopbilling.dto.request.ResetPasswordRequest;
import com.shopbilling.dto.response.UserResponse;
import com.shopbilling.entity.User;
import com.shopbilling.enums.AuditAction;
import com.shopbilling.exception.BusinessException;
import com.shopbilling.exception.DuplicateResourceException;
import com.shopbilling.exception.ResourceNotFoundException;
import com.shopbilling.mapper.UserMapper;
import com.shopbilling.repository.UserRepository;
import com.shopbilling.service.AuditLogService;
import com.shopbilling.service.UserService;
import com.shopbilling.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AuditLogService auditLogService;
    private final SecurityUtils securityUtils;

    @Override
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists: " + request.getUsername());
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .fullName(request.getFullName())
                .active(true)
                .build();

        User saved = userRepository.save(user);
        auditLogService.log(AuditAction.USER_CREATED, "User", saved.getId(),
                "User created: " + saved.getUsername() + " with role: " + saved.getRole());
        
        log.info("User created: {}", saved.getUsername());
        return userMapper.toResponse(saved);
    }

    @Override
    public UserResponse updateUser(Long id, CreateUserRequest request) {
        User user = getUserEntityById(id);
        
        if (!user.getUsername().equals(request.getUsername()) &&
                userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists: " + request.getUsername());
        }

        user.setUsername(request.getUsername());
        user.setRole(request.getRole());
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        User saved = userRepository.save(user);
        auditLogService.log(AuditAction.USER_UPDATED, "User", saved.getId(),
                "User updated: " + saved.getUsername());
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return userMapper.toResponse(getUserEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userMapper.toResponseList(userRepository.findAll());
    }

    @Override
    public void disableUser(Long id) {
        User user = getUserEntityById(id);
        User currentUser = securityUtils.getCurrentUser();
        if (user.getId().equals(currentUser.getId())) {
            throw new BusinessException("Cannot disable your own account");
        }
        user.setActive(false);
        userRepository.save(user);
        auditLogService.log(AuditAction.USER_DISABLED, "User", id, "User disabled: " + user.getUsername());
        log.info("User disabled: {}", user.getUsername());
    }

    @Override
    public void enableUser(Long id) {
        User user = getUserEntityById(id);
        user.setActive(true);
        userRepository.save(user);
        auditLogService.log(AuditAction.USER_ENABLED, "User", id, "User enabled: " + user.getUsername());
        log.info("User enabled: {}", user.getUsername());
    }

    @Override
    public void resetPassword(Long id, ResetPasswordRequest request) {
        User user = getUserEntityById(id);
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        auditLogService.log(AuditAction.PASSWORD_RESET, "User", id, "Password reset for: " + user.getUsername());
        log.info("Password reset for user: {}", user.getUsername());
    }

    private User getUserEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }
}
