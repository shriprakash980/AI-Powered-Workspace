package com.devpilot.ai.controller;

import com.devpilot.ai.dto.file.CreateFileRequest;
import com.devpilot.ai.entity.Project;
import com.devpilot.ai.entity.enums.ProjectStatus;
import com.devpilot.ai.entity.enums.ProjectTemplate;
import com.devpilot.ai.repository.ProjectFileRepository;
import com.devpilot.ai.repository.ProjectRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "admin@devpilot.ai", roles = {"ADMIN"})
class WorkspaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectFileRepository projectFileRepository;

    private UUID projectId;

    @BeforeEach
    void setUp() {
        projectFileRepository.deleteAll();
        projectRepository.deleteAll();

        Project project = Project.builder()
                .name("workspace-overview-project")
                .description("Test project for workspace summary")
                .template(ProjectTemplate.HTML_CSS_JS)
                .language("JavaScript")
                .status(ProjectStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Project saved = projectRepository.save(project);
        projectId = saved.getId();
    }

    @Test
    @DisplayName("GET /api/v1/projects/{projectId}/workspace should return project metadata, tree, and recent files")
    void shouldGetWorkspaceOverview() throws Exception {
        // Create sample file
        CreateFileRequest fileReq = new CreateFileRequest("app.js", null, "console.log('workspace');");
        mockMvc.perform(post("/api/v1/projects/" + projectId + "/files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fileReq)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/projects/" + projectId + "/workspace")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.project.name").value("workspace-overview-project"))
                .andExpect(jsonPath("$.data.tree", hasSize(1)))
                .andExpect(jsonPath("$.data.tree[0].name").value("app.js"))
                .andExpect(jsonPath("$.data.recentFiles", hasSize(1)))
                .andExpect(jsonPath("$.data.totalFiles").value(1));
    }
}
