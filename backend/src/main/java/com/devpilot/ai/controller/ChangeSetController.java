package com.devpilot.ai.controller;

import com.devpilot.ai.dto.ApiResponse;
import com.devpilot.ai.patch.ApplyChangeSetRequest;
import com.devpilot.ai.patch.ChangeSetProposal;
import com.devpilot.ai.patch.ChangeSetResponse;
import com.devpilot.ai.security.UserPrincipal;
import com.devpilot.ai.service.PatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/changesets")
@Tag(name = "AI ChangeSets", description = "Endpoints for proposing, reviewing, applying, rejecting, and rolling back AI changesets")
@SecurityRequirement(name = "BearerAuth")
public class ChangeSetController {

    private static final Logger log = LoggerFactory.getLogger(ChangeSetController.class);

    private final PatchService patchService;

    public ChangeSetController(PatchService patchService) {
        this.patchService = patchService;
    }

    @PostMapping
    @Operation(summary = "Propose a changeset", description = "Generates a structured multi-file changeset proposal with unified diffs.")
    public ResponseEntity<ApiResponse<ChangeSetResponse>> proposeChangeSet(
            @PathVariable UUID projectId,
            @Valid @RequestBody ChangeSetProposal proposal,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("REST request to propose changeset for project: {}", projectId);
        ChangeSetResponse response = patchService.proposeChangeSet(projectId, proposal, userPrincipal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "ChangeSet proposed successfully"));
    }

    @GetMapping
    @Operation(summary = "List project changesets", description = "Lists all proposed, applied, and rejected changesets for a project.")
    public ResponseEntity<ApiResponse<List<ChangeSetResponse>>> getChangeSets(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("REST request to list changesets for project: {}", projectId);
        List<ChangeSetResponse> response = patchService.getChangeSets(projectId, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(response, "ChangeSets retrieved successfully"));
    }

    @GetMapping("/{changeSetId}")
    @Operation(summary = "Get changeset details", description = "Retrieves full details and unified diffs for a specific changeset.")
    public ResponseEntity<ApiResponse<ChangeSetResponse>> getChangeSet(
            @PathVariable UUID projectId,
            @PathVariable UUID changeSetId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("REST request to get changeset {} for project: {}", changeSetId, projectId);
        ChangeSetResponse response = patchService.getChangeSet(projectId, changeSetId, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(response, "ChangeSet retrieved successfully"));
    }

    @PostMapping("/{changeSetId}/apply")
    @Operation(summary = "Apply changeset", description = "Atomically applies selected or all file changes with optimistic concurrency validation.")
    public ResponseEntity<ApiResponse<ChangeSetResponse>> applyChangeSet(
            @PathVariable UUID projectId,
            @PathVariable UUID changeSetId,
            @RequestBody(required = false) ApplyChangeSetRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("REST request to apply changeset {} in project: {}", changeSetId, projectId);
        ChangeSetResponse response = patchService.applyChangeSet(projectId, changeSetId, request, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(response, "ChangeSet applied successfully"));
    }

    @PostMapping("/{changeSetId}/reject")
    @Operation(summary = "Reject changeset", description = "Marks a proposed changeset as rejected.")
    public ResponseEntity<ApiResponse<ChangeSetResponse>> rejectChangeSet(
            @PathVariable UUID projectId,
            @PathVariable UUID changeSetId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("REST request to reject changeset {} in project: {}", changeSetId, projectId);
        ChangeSetResponse response = patchService.rejectChangeSet(projectId, changeSetId, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(response, "ChangeSet rejected successfully"));
    }

    @PostMapping("/{changeSetId}/rollback")
    @Operation(summary = "Rollback changeset", description = "Rolls back an applied changeset non-destructively by creating rollback snapshots.")
    public ResponseEntity<ApiResponse<ChangeSetResponse>> rollbackChangeSet(
            @PathVariable UUID projectId,
            @PathVariable UUID changeSetId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("REST request to rollback changeset {} in project: {}", changeSetId, projectId);
        ChangeSetResponse response = patchService.rollbackChangeSet(projectId, changeSetId, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(response, "ChangeSet rolled back successfully"));
    }
}
