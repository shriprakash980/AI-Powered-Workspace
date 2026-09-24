package com.devpilot.ai.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/v1/admin/test with ROLE_ADMIN should succeed with 200")
    void shouldAllowAdminAccess() throws Exception {
        mockMvc.perform(get("/api/v1/admin/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.scope").value("ROLE_ADMIN"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/v1/admin/test with ROLE_USER should return 403 Forbidden")
    void shouldDenyUserAccessToAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/admin/test"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/admin/test unauthenticated should return 401 Unauthorized or 403 Forbidden")
    void shouldDenyUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/admin/test"))
                .andExpect(status().is4xxClientError());
    }
}
