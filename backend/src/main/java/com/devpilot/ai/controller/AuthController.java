package com.devpilot.ai.controller;

import com.devpilot.ai.dto.ApiResponse;
import com.devpilot.ai.dto.user.*;
import com.devpilot.ai.security.UserPrincipal;
import com.devpilot.ai.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Endpoints for user registration, authentication, JWT tokens, and session management")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user account", description = "Creates a new developer account with BCrypt password hashing and default ROLE_USER.")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("REST request to register user with email: '{}'", request.getEmail());
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "User registered successfully"));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and issue JWT", description = "Verifies credentials and issues a short-lived JWT access token and refresh token.")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("REST request to login user: '{}'", request.getEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Rotates refresh token and issues a new JWT access token.")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("REST request to refresh access token");
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user session", description = "Revokes the active refresh token session.")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody(required = false) LogoutRequest request) {
        log.info("REST request to logout user");
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.message("Logged out successfully"));
    }

    @GetMapping("/me")
    @Operation(
            summary = "Get current authenticated user profile",
            description = "Retrieves user details from JWT claims and database.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        log.info("REST request to get profile for authenticated user ID: {}", principal != null ? principal.getId() : "null");
        UserResponse response = authService.getCurrentUser(principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Current user retrieved successfully"));
    }
}
