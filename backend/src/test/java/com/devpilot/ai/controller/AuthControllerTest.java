package com.devpilot.ai.controller;

import com.devpilot.ai.dto.user.AuthResponse;
import com.devpilot.ai.dto.user.LoginRequest;
import com.devpilot.ai.dto.user.RegisterRequest;
import com.devpilot.ai.dto.user.UserResponse;
import com.devpilot.ai.entity.enums.UserStatus;
import com.devpilot.ai.exception.UnauthorizedException;
import com.devpilot.ai.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("POST /api/v1/auth/register should return 201 on valid request")
    void shouldRegisterSuccessfully() throws Exception {
        RegisterRequest request = new RegisterRequest("Test User", "test@devpilot.ai", "Password123!");
        UserResponse response = UserResponse.builder()
                .id(UUID.randomUUID())
                .fullName("Test User")
                .email("test@devpilot.ai")
                .roles(List.of("USER"))
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("test@devpilot.ai"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register should return 400 on invalid input")
    void shouldFailRegistrationOnInvalidInput() throws Exception {
        RegisterRequest request = new RegisterRequest("", "invalid-email", "short");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should return 200 with tokens on valid credentials")
    void shouldLoginSuccessfully() throws Exception {
        LoginRequest request = new LoginRequest("user@devpilot.ai", "Password123!");
        AuthResponse response = AuthResponse.builder()
                .accessToken("mocked-access-token")
                .tokenType("Bearer")
                .expiresIn(900)
                .refreshToken("mocked-refresh-token")
                .user(UserResponse.builder()
                        .id(UUID.randomUUID())
                        .fullName("Test User")
                        .email("user@devpilot.ai")
                        .roles(List.of("USER"))
                        .build())
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("mocked-access-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should return 401 on bad credentials")
    void shouldFailLoginOnBadCredentials() throws Exception {
        LoginRequest request = new LoginRequest("user@devpilot.ai", "WrongPassword");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new UnauthorizedException("Invalid email or password"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}
