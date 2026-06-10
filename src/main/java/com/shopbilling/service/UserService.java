package com.shopbilling.service;

import com.shopbilling.dto.request.CreateUserRequest;
import com.shopbilling.dto.request.ResetPasswordRequest;
import com.shopbilling.dto.response.UserResponse;

import java.util.List;

public interface UserService {
    UserResponse createUser(CreateUserRequest request);
    UserResponse updateUser(Long id, CreateUserRequest request);
    UserResponse getUserById(Long id);
    List<UserResponse> getAllUsers();
    void disableUser(Long id);
    void enableUser(Long id);
    void resetPassword(Long id, ResetPasswordRequest request);
}
