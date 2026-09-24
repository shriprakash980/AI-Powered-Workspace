package com.devpilot.ai.controller;

import com.devpilot.ai.context.ContextRequest;
import com.devpilot.ai.context.ContextResponse;
import com.devpilot.ai.context.ContextService;
import com.devpilot.ai.dto.ApiResponse;
import com.devpilot.ai.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/context")
@Tag(name = "AI Context Engine", description = "Endpoints for context preview and workspace context ranking")
@SecurityRequirement(name = "BearerAuth")
public class ContextController {

    private static final Logger log = LoggerFactory.getLogger(ContextController.class);

    private final ContextService contextService;

    public ContextController(ContextService contextService) {
        this.contextService = contextService;
    }

    @PostMapping("/preview")
    @Operation(summary = "Preview context", description = "Simulates and previews context assembly, ranking scores, and token budgets for a prompt.")
    public ResponseEntity<ApiResponse<ContextResponse>> previewContext(
            @Valid @RequestBody ContextRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("REST request to preview AI context for project: {}", request.getProjectId());
        ContextResponse response = contextService.previewContext(request, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(response, "Context assembled successfully"));
    }
}
