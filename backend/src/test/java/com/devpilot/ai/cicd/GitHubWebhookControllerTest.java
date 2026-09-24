package com.devpilot.ai.cicd;

import com.devpilot.ai.cicd.controller.GitHubWebhookController;
import com.devpilot.ai.cicd.service.PipelineTriggerService;
import com.devpilot.ai.cicd.service.WebhookVerificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GitHubWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WebhookVerificationService verificationService;

    @MockBean
    private PipelineTriggerService triggerService;

    @Test
    @DisplayName("POST /api/v1/webhooks/github - Should accept verified webhook event")
    void testHandleGitHubWebhookSuccess() throws Exception {
        when(verificationService.isVerificationEnabled()).thenReturn(true);
        when(verificationService.verifySignature(any(), any())).thenReturn(true);
        when(verificationService.isDuplicateDelivery(any())).thenReturn(false);

        String payload = "{\"ref\":\"refs/heads/main\",\"repository\":{\"full_name\":\"user/demo-app\"}}";

        mockMvc.perform(post("/api/v1/webhooks/github")
                        .header("X-GitHub-Event", "push")
                        .header("X-Hub-Signature-256", "sha256=mocksignature123")
                        .header("X-GitHub-Delivery", "delivery-uuid-99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
