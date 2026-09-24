package com.devpilot.ai.build;

import com.devpilot.ai.build.controller.BuildController;
import com.devpilot.ai.build.dto.BuildRequest;
import com.devpilot.ai.build.dto.BuildResponse;
import com.devpilot.ai.build.dto.ProjectDetectionResponse;
import com.devpilot.ai.build.entity.enums.BuildMode;
import com.devpilot.ai.build.entity.enums.BuildStatus;
import com.devpilot.ai.build.entity.enums.ProjectType;
import com.devpilot.ai.build.service.BuildAiService;
import com.devpilot.ai.build.service.BuildDetectionService;
import com.devpilot.ai.build.service.BuildLogService;
import com.devpilot.ai.build.service.BuildService;
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
class BuildControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BuildService buildService;

    @MockBean
    private BuildDetectionService buildDetectionService;

    @MockBean
    private BuildLogService buildLogService;

    @MockBean
    private BuildAiService buildAiService;

    private UUID projectId;
    private UUID buildId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        buildId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("POST /api/v1/projects/{projectId}/builds/detect - Should detect project type")
    void testDetectProject() throws Exception {
        ProjectDetectionResponse detection = new ProjectDetectionResponse(
                List.of(ProjectType.JAVA_SPRING, ProjectType.JAVA_MAVEN),
                ProjectType.JAVA_SPRING,
                "mvn compile",
                "mvn test",
                "java -jar target/*.jar",
                "target",
                null
        );

        when(buildDetectionService.detectProjectType(projectId)).thenReturn(detection);

        mockMvc.perform(post("/api/v1/projects/{projectId}/builds/detect", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.primaryType").value("JAVA_SPRING"));
    }

    @Test
    @DisplayName("POST /api/v1/projects/{projectId}/builds - Should trigger build")
    void testTriggerBuild() throws Exception {
        BuildRequest req = new BuildRequest(BuildMode.BUILD, null);
        BuildResponse res = new BuildResponse(
                buildId, projectId, userId, BuildStatus.QUEUED, ProjectType.JAVA_SPRING,
                BuildMode.BUILD, "mvn compile", null, null, null, 0L, Instant.now(), null, Instant.now()
        );

        when(buildService.triggerBuild(eq(projectId), any(User.class), any(BuildRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/v1/projects/{projectId}/builds", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.buildId").value(buildId.toString()))
                .andExpect(jsonPath("$.data.status").value("QUEUED"));
    }
}
