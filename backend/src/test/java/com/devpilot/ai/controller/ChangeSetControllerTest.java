package com.devpilot.ai.controller;

import com.devpilot.ai.entity.enums.FileChangeOperation;
import com.devpilot.ai.patch.ChangeSetProposal;
import com.devpilot.ai.patch.ChangeSetResponse;
import com.devpilot.ai.patch.FileChangeProposal;
import com.devpilot.ai.patch.FileChangeResponse;
import com.devpilot.ai.service.PatchService;
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

import static org.hamcrest.Matchers.hasSize;
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
@WithMockUser(username = "admin@devpilot.ai", roles = {"ADMIN"})
class ChangeSetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PatchService patchService;

    private UUID projectId;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
    }

    @Test
    @DisplayName("POST /api/v1/projects/{projectId}/changesets creates a proposed changeset")
    void testProposeChangeSetEndpoint() throws Exception {
        ChangeSetProposal proposal = new ChangeSetProposal(
                null,
                "Add log statement",
                List.of(new FileChangeProposal(
                        FileChangeOperation.UPDATE,
                        "src/index.js",
                        "console.log('modified');",
                        "test edit"
                ))
        );

        ChangeSetResponse response = new ChangeSetResponse(
                UUID.randomUUID(),
                projectId,
                UUID.randomUUID(),
                null,
                "Add log statement",
                "PROPOSED",
                List.of(new FileChangeResponse(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "UPDATE",
                        "src/index.js",
                        "src/index.js",
                        "oldhash",
                        "newhash",
                        "console.log('original');",
                        "console.log('modified');",
                        "+console.log('modified');",
                        "PROPOSED",
                        1, 0,
                        Instant.now(), null
                )),
                Instant.now(), null, null
        );

        when(patchService.proposeChangeSet(eq(projectId), any(ChangeSetProposal.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/projects/" + projectId + "/changesets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(proposal)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PROPOSED"))
                .andExpect(jsonPath("$.data.filesCount").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/projects/{projectId}/changesets lists proposed changesets")
    void testListChangeSetsEndpoint() throws Exception {
        ChangeSetResponse response = new ChangeSetResponse(
                UUID.randomUUID(),
                projectId,
                UUID.randomUUID(),
                null,
                "Listing test",
                "PROPOSED",
                List.of(),
                Instant.now(), null, null
        );

        when(patchService.getChangeSets(eq(projectId), any())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/projects/" + projectId + "/changesets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].summary").value("Listing test"));
    }
}
