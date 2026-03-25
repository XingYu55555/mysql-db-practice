package com.sqljudge.userservice.controller;

import com.sqljudge.userservice.model.dto.request.LoginRequest;
import com.sqljudge.userservice.model.dto.request.RegisterRequest;
import com.sqljudge.userservice.model.dto.response.LoginResponse;
import com.sqljudge.userservice.model.dto.response.RegisterResponse;
import com.sqljudge.userservice.model.dto.response.UserInfo;
import com.sqljudge.userservice.security.CustomUserDetails;
import com.sqljudge.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication related endpoints")
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "User login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user information", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<UserInfo> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UserInfo userInfo = userService.getCurrentUser(userDetails.getUserId());
        return ResponseEntity.ok(userInfo);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user information by ID", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<UserInfo> getUserById(@PathVariable Long userId) {
        UserInfo userInfo = userService.getUserById(userId);
        return ResponseEntity.ok(userInfo);
    }
}
