package com.example.activity.controller;

import com.example.activity.common.ApiResponse;
import com.example.activity.dto.LoginRequest;
import com.example.activity.dto.RegisterRequest;
import com.example.activity.service.AuthService;
import com.example.activity.vo.LoginResponse;
import com.example.activity.vo.UserProfile;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<UserProfile> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.success();
    }

    @GetMapping("/me")
    public ApiResponse<UserProfile> me(Authentication authentication) {
        return ApiResponse.success(authService.currentUser(authentication));
    }
}
