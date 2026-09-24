package com.devpilot.ai;

import com.devpilot.ai.dto.ApiResponse;
import com.devpilot.ai.dto.project.ProjectRequest;
import com.devpilot.ai.dto.user.AuthResponse;
import com.devpilot.ai.dto.user.LoginRequest;
import com.devpilot.ai.dto.user.LogoutRequest;
import com.devpilot.ai.dto.user.RefreshTokenRequest;
import com.devpilot.ai.dto.user.RegisterRequest;
import com.devpilot.ai.entity.enums.ProjectTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("End-to-End Authentication & Authorization Lifecycle: Register -> Login -> Me -> Project CRUD -> Ownership -> Refresh -> Logout")
    void testCompleteAuthLifecycle() throws Exception {
        // Step 1: Register User A
        RegisterRequest registerA = new RegisterRequest("User Alpha", "alpha@devpilot.ai", "AlphaSecure123!");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("alpha@devpilot.ai"));

        // Register User B
        RegisterRequest registerB = new RegisterRequest("User Beta", "beta@devpilot.ai", "BetaSecure123!");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerB)))
                .andExpect(status().isCreated());

        // Step 2 & 3: Login User A and receive access & refresh tokens
        LoginRequest loginA = new LoginRequest("alpha@devpilot.ai", "AlphaSecure123!");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String tokenA = loginJson.get("data").get("accessToken").asText();
        String refreshTokenA = loginJson.get("data").get("refreshToken").asText();
        String userAId = loginJson.get("data").get("user").get("id").asText();

        // Login User B and get token
        LoginRequest loginB = new LoginRequest("beta@devpilot.ai", "BetaSecure123!");
        MvcResult loginBResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginB)))
                .andExpect(status().isOk())
                .andReturn();
        String tokenB = objectMapper.readTree(loginBResult.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();

        // Step 4: Call GET /api/v1/auth/me with User A's token
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("alpha@devpilot.ai"))
                .andExpect(jsonPath("$.data.id").value(userAId));

        // Step 5: User A creates a project
        ProjectRequest projectReq = ProjectRequest.builder()
                .name("alpha-cloud-ide")
                .description("User A workspace project")
                .template(ProjectTemplate.HTML_CSS_JS)
                .language("JavaScript")
                .build();

        MvcResult projectCreateResult = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("alpha-cloud-ide"))
                .andExpect(jsonPath("$.data.ownerId").value(userAId))
                .andReturn();

        String projectAId = objectMapper.readTree(projectCreateResult.getResponse().getContentAsString())
                .get("data").get("id").asText();

        // Step 6 & 7: User A lists projects and verifies ownership
        mockMvc.perform(get("/api/v1/projects")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].ownerId").value(userAId));

        // Step 8: User B attempts to access User A's project -> MUST BE 403 Forbidden!
        mockMvc.perform(get("/api/v1/projects/" + projectAId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // User B attempts to delete User A's project -> MUST BE 403 Forbidden!
        mockMvc.perform(delete("/api/v1/projects/" + projectAId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // Step 9, 10, 11: Refresh authentication with Refresh Token
        RefreshTokenRequest refreshReq = new RefreshTokenRequest(refreshTokenA);
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn();

        JsonNode refreshJson = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String newAccessToken = refreshJson.get("data").get("accessToken").asText();
        String newRefreshToken = refreshJson.get("data").get("refreshToken").asText();

        assertNotEquals(refreshTokenA, newRefreshToken, "Refresh token should be rotated");

        // Old refresh token must now be revoked and rejected on reuse!
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshTokenA))))
                .andExpect(status().isUnauthorized());

        // Step 12: Logout with the new refresh token
        LogoutRequest logoutReq = new LogoutRequest(newRefreshToken);
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + newAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutReq)))
                .andExpect(status().isOk());

        // Step 13: Verify new refresh token is revoked after logout
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(newRefreshToken))))
                .andExpect(status().isUnauthorized());
    }
}
