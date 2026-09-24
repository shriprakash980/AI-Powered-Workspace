package com.devpilot.ai.controller;

import com.devpilot.ai.dto.project.ProjectRequest;
import com.devpilot.ai.entity.enums.ProjectTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "developer@devpilot.ai", roles = {"USER"})
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldGetAllProjects() throws Exception {
        mockMvc.perform(get("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void shouldCreateNewProjectSuccessfully() throws Exception {
        ProjectRequest request = ProjectRequest.builder()
                .name("test-new-workspace")
                .description("Automated test project workspace")
                .template(ProjectTemplate.HTML_CSS_JS)
                .language("JavaScript")
                .framework("Vanilla")
                .build();

        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("test-new-workspace"))
                .andExpect(jsonPath("$.data.language").value("JavaScript"))
                .andExpect(jsonPath("$.data.id").isNotEmpty());
    }

    @Test
    void shouldFailValidationWhenProjectNameIsBlank() throws Exception {
        ProjectRequest request = ProjectRequest.builder()
                .name("")
                .description("Invalid project")
                .template(ProjectTemplate.BLANK)
                .language("JavaScript")
                .build();

        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Input validation failed"))
                .andExpect(jsonPath("$.errors", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void shouldReturn404WhenProjectNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/projects/99999999-9999-9999-9999-999999999999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
