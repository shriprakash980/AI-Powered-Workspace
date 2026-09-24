package com.devpilot.ai.git;

import com.devpilot.ai.git.dto.*;
import com.devpilot.ai.git.service.*;
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

import java.util.Collections;
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
class GitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GitService gitService;

    @MockBean
    private GitStatusService gitStatusService;

    @MockBean
    private GitCommitService gitCommitService;

    @MockBean
    private GitBranchService gitBranchService;

    @MockBean
    private GitRemoteService gitRemoteService;

    @MockBean
    private GitAiService gitAiService;

    private UUID projectId;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
    }

    @Test
    @DisplayName("POST /api/v1/projects/{projectId}/git/init initializes repository")
    void testInitRepo() throws Exception {
        when(gitService.initRepository(eq(projectId), any()))
                .thenReturn(new GitInitResponse(projectId, true, "main", "Repository initialized"));

        mockMvc.perform(post("/api/v1/projects/" + projectId + "/git/init")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.initialized").value(true))
                .andExpect(jsonPath("$.data.defaultBranch").value("main"));
    }

    @Test
    @DisplayName("GET /api/v1/projects/{projectId}/git/status returns working status")
    void testGetStatus() throws Exception {
        when(gitStatusService.getStatus(eq(projectId), any()))
                .thenReturn(new GitStatusResponse("main", 1, 0, false,
                        List.of(new GitFileStatusDto("src/App.java", "MODIFIED", true, false)),
                        1, 0));

        mockMvc.perform(get("/api/v1/projects/" + projectId + "/git/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.branch").value("main"))
                .andExpect(jsonPath("$.data.ahead").value(1))
                .andExpect(jsonPath("$.data.files[0].path").value("src/App.java"))
                .andExpect(jsonPath("$.data.files[0].status").value("MODIFIED"));
    }

    @Test
    @DisplayName("POST /api/v1/projects/{projectId}/git/commit validates and commits")
    void testCommit() throws Exception {
        when(gitCommitService.commit(eq(projectId), any(), any()))
                .thenReturn(new GitCommitResponse("abc1234567890", "abc1234", "main", "Add login form", 2));

        mockMvc.perform(post("/api/v1/projects/" + projectId + "/git/commit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GitCommitRequest("Add login form"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shortHash").value("abc1234"))
                .andExpect(jsonPath("$.data.message").value("Add login form"));
    }

    @Test
    @DisplayName("GET /api/v1/projects/{projectId}/git/branches returns branch list")
    void testGetBranches() throws Exception {
        when(gitBranchService.getBranches(eq(projectId), any()))
                .thenReturn(new GitBranchListResponse("main", List.of(
                        new GitBranchDto("main", true, false, "abc1234"),
                        new GitBranchDto("feature/auth", false, false, "def5678")
                )));

        mockMvc.perform(get("/api/v1/projects/" + projectId + "/git/branches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentBranch").value("main"))
                .andExpect(jsonPath("$.data.branches[0].name").value("main"))
                .andExpect(jsonPath("$.data.branches[1].name").value("feature/auth"));
    }
}
