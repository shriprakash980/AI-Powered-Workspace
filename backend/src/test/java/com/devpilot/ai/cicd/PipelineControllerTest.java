package com.devpilot.ai.cicd;

import com.devpilot.ai.cicd.controller.PipelineController;
import com.devpilot.ai.cicd.dto.PipelineCreateRequest;
import com.devpilot.ai.cicd.dto.PipelineResponse;
import com.devpilot.ai.cicd.service.PipelineService;
import com.devpilot.ai.deployment.entity.enums.DeploymentEnvironment;
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
import java.util.List;
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
class PipelineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PipelineService pipelineService;

    private UUID projectId;
    private UUID pipelineId;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        pipelineId = UUID.randomUUID();
    }

    @Test
    @DisplayName("GET /api/v1/projects/{projectId}/pipelines - Should list project pipelines")
    void testGetProjectPipelines() throws Exception {
        PipelineResponse res = new PipelineResponse(
                pipelineId, projectId, "Main Pipeline", "Description", true, "devpilot-ci.yml", "main", true, true, true, false, DeploymentEnvironment.DEVELOPMENT, Instant.now(), Instant.now()
        );

        when(pipelineService.getProjectPipelines(eq(projectId), any())).thenReturn(List.of(res));

        mockMvc.perform(get("/api/v1/projects/{projectId}/pipelines", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Main Pipeline"));
    }

    @Test
    @DisplayName("POST /api/v1/projects/{projectId}/pipelines - Should create pipeline")
    void testCreatePipeline() throws Exception {
        PipelineCreateRequest req = new PipelineCreateRequest(
                "Build Pipeline", "Desc", true, "devpilot-ci.yml", "main", true, true, true, false, DeploymentEnvironment.DEVELOPMENT
        );

        PipelineResponse res = new PipelineResponse(
                pipelineId, projectId, "Build Pipeline", "Desc", true, "devpilot-ci.yml", "main", true, true, true, false, DeploymentEnvironment.DEVELOPMENT, Instant.now(), Instant.now()
        );

        when(pipelineService.createPipeline(eq(projectId), any(), any())).thenReturn(res);

        mockMvc.perform(post("/api/v1/projects/{projectId}/pipelines", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Build Pipeline"));
    }
}
