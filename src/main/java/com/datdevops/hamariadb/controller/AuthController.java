package com.datdevops.hamariadb.controller;


import com.datdevops.hamariadb.dto.request.LoginRequest;
import com.datdevops.hamariadb.dto.response.ApiResponse;
import com.datdevops.hamariadb.dto.response.LoginResponse;
import com.datdevops.hamariadb.service.auth.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            Authentication authentication) {

        String username = authentication.getName();
        authService.changePassword(username, currentPassword, newPassword);

        return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(Authentication authentication) {
        String username = authentication.getName();
        log.info("User {} logged out", username);
        // In a real implementation, you might want to blacklist the token
        return ResponseEntity.ok(ApiResponse.success(null, "Logout successful"));
    }
}
