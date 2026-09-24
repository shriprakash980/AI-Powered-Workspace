package com.devpilot.ai.controller;

import com.devpilot.ai.dto.file.*;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "admin@devpilot.ai", roles = {"ADMIN"})
class ProjectFileControllerTest {

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
                .name("workspace-test-project")
                .description("Automated test workspace")
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
    @DisplayName("POST /api/v1/projects/{projectId}/files/folders should create folder")
    void shouldCreateFolder() throws Exception {
        CreateFolderRequest request = new CreateFolderRequest("src", null);

        mockMvc.perform(post("/api/v1/projects/" + projectId + "/files/folders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("src"))
                .andExpect(jsonPath("$.data.path").value("src"))
                .andExpect(jsonPath("$.data.directory").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/projects/{projectId}/files should create file")
    void shouldCreateFile() throws Exception {
        CreateFileRequest request = new CreateFileRequest("index.html", null, "<h1>Hello</h1>");

        mockMvc.perform(post("/api/v1/projects/" + projectId + "/files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("index.html"))
                .andExpect(jsonPath("$.data.fileType").value("html"))
                .andExpect(jsonPath("$.data.content").value("<h1>Hello</h1>"))
                .andExpect(jsonPath("$.data.directory").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/projects/{projectId}/files/tree should return file hierarchy")
    void shouldGetFileTree() throws Exception {
        // Create folder
        CreateFolderRequest folderReq = new CreateFolderRequest("docs", null);
        mockMvc.perform(post("/api/v1/projects/" + projectId + "/files/folders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(folderReq)))
                .andExpect(status().isCreated());

        // Create file
        CreateFileRequest fileReq = new CreateFileRequest("README.md", null, "# Docs");
        mockMvc.perform(post("/api/v1/projects/" + projectId + "/files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fileReq)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/projects/" + projectId + "/files/tree")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].name").value("docs"))
                .andExpect(jsonPath("$.data[0].directory").value(true))
                .andExpect(jsonPath("$.data[1].name").value("README.md"))
                .andExpect(jsonPath("$.data[1].directory").value(false));
    }

    @Test
    @DisplayName("PUT /api/v1/projects/{projectId}/files/{fileId} should update content")
    void shouldUpdateFileContent() throws Exception {
        CreateFileRequest fileReq = new CreateFileRequest("app.js", null, "console.log('v1');");
        String createResponse = mockMvc.perform(post("/api/v1/projects/" + projectId + "/files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fileReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String fileId = objectMapper.readTree(createResponse).path("data").path("id").asText();

        UpdateFileContentRequest updateReq = new UpdateFileContentRequest("console.log('v2');");
        mockMvc.perform(put("/api/v1/projects/" + projectId + "/files/" + fileId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").value("console.log('v2');"));
    }

    @Test
    @DisplayName("DELETE /api/v1/projects/{projectId}/files/{fileId} should delete file")
    void shouldDeleteFile() throws Exception {
        CreateFileRequest fileReq = new CreateFileRequest("temp.txt", null, "temp");
        String createResponse = mockMvc.perform(post("/api/v1/projects/" + projectId + "/files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fileReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String fileId = objectMapper.readTree(createResponse).path("data").path("id").asText();

        mockMvc.perform(delete("/api/v1/projects/" + projectId + "/files/" + fileId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/v1/projects/" + projectId + "/files/" + fileId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/projects/{projectId}/files/search should find matching files")
    void shouldSearchFiles() throws Exception {
        CreateFileRequest fileReq = new CreateFileRequest("DatabaseConfig.java", null, "class DatabaseConfig {}");
        mockMvc.perform(post("/api/v1/projects/" + projectId + "/files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fileReq)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/projects/" + projectId + "/files/search")
                        .param("query", "database"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("DatabaseConfig.java"));
    }
}
