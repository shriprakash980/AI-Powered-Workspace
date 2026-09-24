package com.devpilot.ai.git;

import com.devpilot.ai.dto.project.ProjectResponse;
import com.devpilot.ai.entity.enums.ProjectStatus;
import com.devpilot.ai.entity.enums.ProjectTemplate;
import com.devpilot.ai.git.dto.*;
import com.devpilot.ai.git.service.GitHubOAuthService;
import com.devpilot.ai.git.service.GitHubService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "testuser", roles = {"USER"})
class GitHubControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GitHubOAuthService gitHubOAuthService;

    @MockBean
    private GitHubService gitHubService;

    @Test
    @DisplayName("GET /api/v1/github/status returns connection status")
    void testGetStatus() throws Exception {
        when(gitHubOAuthService.getStatus(any()))
                .thenReturn(new GitHubStatusResponse(true, "octocat", "repo,user:email", Instant.now()));

        mockMvc.perform(get("/api/v1/github/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.connected").value(true))
                .andExpect(jsonPath("$.data.username").value("octocat"));
    }

    @Test
    @DisplayName("GET /api/v1/github/oauth/start returns authorization url")
    void testStartOAuth() throws Exception {
        when(gitHubOAuthService.startOAuth())
                .thenReturn(new GitHubOAuthStartResponse("https://github.com/login/oauth/authorize?client_id=123", "state123"));

        mockMvc.perform(get("/api/v1/github/oauth/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authorizationUrl").value("https://github.com/login/oauth/authorize?client_id=123"));
    }

    @Test
    @DisplayName("GET /api/v1/github/repositories returns user repositories")
    void testGetRepositories() throws Exception {
        when(gitHubService.getRepositories(any(), eq(1), eq(30), any()))
                .thenReturn(List.of(
                        new GitHubRepositoryDto(12345L, "devpilot", "testuser/devpilot", false, "main",
                                "https://github.com/testuser/devpilot", "https://github.com/testuser/devpilot.git",
                                "AI Workspace", "2026-09-24T12:00:00Z")
                ));

        mockMvc.perform(get("/api/v1/github/repositories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("devpilot"))
                .andExpect(jsonPath("$.data[0].fullName").value("testuser/devpilot"));
    }

    @Test
    @DisplayName("POST /api/v1/github/repositories/import clones and creates project")
    void testImportRepository() throws Exception {
        UUID newProjectId = UUID.randomUUID();
        when(gitHubService.importRepository(any(), any()))
                .thenReturn(ProjectResponse.builder()
                        .id(newProjectId)
                        .name("devpilot")
                        .description("Imported from testuser/devpilot")
                        .template(ProjectTemplate.HTML_CSS_JS)
                        .language("java")
                        .status(ProjectStatus.ACTIVE)
                        .gitEnabled(true)
                        .defaultBranch("main")
                        .build());

        GitHubImportRequest req = new GitHubImportRequest("testuser/devpilot", "devpilot", "Imported repo", "HTML_CSS_JS");

        mockMvc.perform(post("/api/v1/github/repositories/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(newProjectId.toString()))
                .andExpect(jsonPath("$.data.gitEnabled").value(true));
    }
}
