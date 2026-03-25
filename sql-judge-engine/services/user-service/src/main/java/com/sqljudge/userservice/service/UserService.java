package com.sqljudge.userservice.service;

import com.sqljudge.userservice.model.dto.request.LoginRequest;
import com.sqljudge.userservice.model.dto.request.RegisterRequest;
import com.sqljudge.userservice.model.dto.response.LoginResponse;
import com.sqljudge.userservice.model.dto.response.RegisterResponse;
import com.sqljudge.userservice.model.dto.response.UserInfo;
import com.sqljudge.userservice.model.entity.User;

public interface UserService {
    RegisterResponse register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
    UserInfo getCurrentUser(Long userId);
    UserInfo getUserById(Long userId);
    User findByUsername(String username);
}
