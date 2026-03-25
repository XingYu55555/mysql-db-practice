package com.sqljudge.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.userservice.model.dto.request.LoginRequest;
import com.sqljudge.userservice.model.dto.request.RegisterRequest;
import com.sqljudge.userservice.model.dto.response.LoginResponse;
import com.sqljudge.userservice.model.dto.response.RegisterResponse;
import com.sqljudge.userservice.model.dto.response.UserInfo;
import com.sqljudge.userservice.security.CustomUserDetails;
import com.sqljudge.userservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .username("testuser")
                .password("password123")
                .role("STUDENT")
                .email("test@example.com")
                .build();

        loginRequest = LoginRequest.builder()
                .username("testuser")
                .password("password123")
                .build();
    }

    @Test
    void register_Success() {
        RegisterResponse response = RegisterResponse.builder()
                .userId(1L)
                .username("testuser")
                .role("STUDENT")
                .build();

        when(userService.register(any(RegisterRequest.class))).thenReturn(response);

        ResponseEntity<RegisterResponse> result = authController.register(registerRequest);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1L, result.getBody().getUserId());
        assertEquals("testuser", result.getBody().getUsername());
        assertEquals("STUDENT", result.getBody().getRole());
    }

    @Test
    void login_Success() {
        LoginResponse response = LoginResponse.builder()
                .token("jwt-token")
                .tokenType("Bearer")
                .expiresIn(86400)
                .userId(1L)
                .username("testuser")
                .role("STUDENT")
                .build();

        when(userService.login(any(LoginRequest.class))).thenReturn(response);

        ResponseEntity<LoginResponse> result = authController.login(loginRequest);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("jwt-token", result.getBody().getToken());
        assertEquals("Bearer", result.getBody().getTokenType());
        assertEquals("testuser", result.getBody().getUsername());
    }

    @Test
    void getUserById_Success() {
        UserInfo userInfo = UserInfo.builder()
                .userId(1L)
                .username("testuser")
                .role("STUDENT")
                .email("test@example.com")
                .createdAt(LocalDateTime.now())
                .build();

        when(userService.getUserById(1L)).thenReturn(userInfo);

        ResponseEntity<UserInfo> result = authController.getUserById(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("testuser", result.getBody().getUsername());
    }
}
