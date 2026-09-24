package com.devpilot.ai.deployment;

import com.devpilot.ai.deployment.controller.DeploymentController;
import com.devpilot.ai.deployment.dto.DeploymentRequest;
import com.devpilot.ai.deployment.dto.DeploymentResponse;
import com.devpilot.ai.deployment.entity.enums.DeploymentEnvironment;
import com.devpilot.ai.deployment.entity.enums.DeploymentStatus;
import com.devpilot.ai.deployment.service.DeploymentAiService;
import com.devpilot.ai.deployment.service.DeploymentLogService;
import com.devpilot.ai.deployment.service.DeploymentService;
import com.devpilot.ai.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "testuser", roles = {"USER"})
class DeploymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DeploymentService deploymentService;

    @MockBean
    private DeploymentLogService deploymentLogService;

    @MockBean
    private DeploymentAiService deploymentAiService;

    private UUID projectId;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
    }

    @Test
    @DisplayName("POST /api/v1/projects/{projectId}/deployments - Should create deployment")
    void testCreateDeployment() throws Exception {
        DeploymentRequest req = new DeploymentRequest(DeploymentEnvironment.DEVELOPMENT, null);
        UUID depId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        DeploymentResponse res = new DeploymentResponse(
                depId, projectId, userId, DeploymentEnvironment.DEVELOPMENT, DeploymentStatus.RUNNING,
                10005, "http://localhost:10005", "devpilot_app", Instant.now(), null
        );

        when(deploymentService.createDeployment(eq(projectId), any(User.class), any(DeploymentRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/v1/projects/{projectId}/deployments", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RUNNING"))
                .andExpect(jsonPath("$.data.allocatedPort").value(10005));
    }
}
