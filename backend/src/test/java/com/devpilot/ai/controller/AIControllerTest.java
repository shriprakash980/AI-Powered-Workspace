package com.devpilot.ai.controller;

import com.devpilot.ai.dto.ai.AIChatRequest;
import com.devpilot.ai.dto.ai.AICodeActionRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "devpilot-test@devpilot.ai", roles = {"USER"})
class AIControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/v1/ai/providers should return list of available AI providers")
    void testGetProviders() throws Exception {
        mockMvc.perform(get("/api/v1/ai/providers")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.providers", hasSize(greaterThanOrEqualTo(3))));
    }

    @Test
    @DisplayName("POST /api/v1/ai/chat should execute chat and return response")
    void testChatEndpoint() throws Exception {
        AIChatRequest request = new AIChatRequest();
        request.setMessage("Write a hello world method in Java");
        request.setProvider("OPENAI");

        mockMvc.perform(post("/api/v1/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isNotEmpty())
                .andExpect(jsonPath("$.data.conversationId").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/v1/ai/code/explain should explain selected code")
    void testExplainCodeEndpoint() throws Exception {
        AICodeActionRequest request = new AICodeActionRequest();
        request.setSelectedCode("public static void main(String[] args) { System.out.println(\"DevPilot\"); }");
        request.setLanguage("java");

        mockMvc.perform(post("/api/v1/ai/code/explain")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.action").value("EXPLAIN"))
                .andExpect(jsonPath("$.data.explanation").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/v1/ai/code/explain should reject empty selection with 400 Bad Request")
    void testExplainCodeEmptySelection() throws Exception {
        AICodeActionRequest request = new AICodeActionRequest();
        request.setSelectedCode("");

        mockMvc.perform(post("/api/v1/ai/code/explain")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
