package com.devpilot.ai.git.controller;

import com.devpilot.ai.dto.ApiResponse;
import com.devpilot.ai.git.dto.*;
import com.devpilot.ai.git.service.*;
import com.devpilot.ai.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/git")
@Tag(name = "Project Git Integration", description = "Source control operations powered by JGit")
public class GitController {

    private final GitService gitService;
    private final GitStatusService gitStatusService;
    private final GitCommitService gitCommitService;
    private final GitBranchService gitBranchService;
    private final GitRemoteService gitRemoteService;
    private final GitAiService gitAiService;

    public GitController(GitService gitService,
                         GitStatusService gitStatusService,
                         GitCommitService gitCommitService,
                         GitBranchService gitBranchService,
                         GitRemoteService gitRemoteService,
                         GitAiService gitAiService) {
        this.gitService = gitService;
        this.gitStatusService = gitStatusService;
        this.gitCommitService = gitCommitService;
        this.gitBranchService = gitBranchService;
        this.gitRemoteService = gitRemoteService;
        this.gitAiService = gitAiService;
    }

    @PostMapping("/init")
    @Operation(summary = "Initialize Git repository for project")
    public ResponseEntity<ApiResponse<GitInitResponse>> initRepository(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        GitInitResponse response = gitService.initRepository(projectId, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(response, response.message()));
    }

    @GetMapping("/status")
    @Operation(summary = "Get current Git working tree and index status")
    public ResponseEntity<ApiResponse<GitStatusResponse>> getStatus(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        GitStatusResponse response = gitStatusService.getStatus(projectId, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(response, "Git status retrieved successfully"));
    }

    @GetMapping("/diff")
    @Operation(summary = "Get Git diff for working tree or staged changes")
    public ResponseEntity<ApiResponse<GitDiffResponse>> getDiff(
            @PathVariable UUID projectId,
            @RequestParam(required = false) String path,
            @RequestParam(required = false, defaultValue = "false") Boolean staged,
            @RequestParam(required = false) String base,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        GitDiffResponse response = gitCommitService.getWorkingDiff(projectId, path, staged, base, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(response, "Diff generated successfully"));
    }

    @PostMapping("/stage")
    @Operation(summary = "Stage specific files")
    public ResponseEntity<ApiResponse<Void>> stage(
            @PathVariable UUID projectId,
            @RequestBody GitStageRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        gitCommitService.stage(projectId, request != null ? request.paths() : null, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(null, "Files staged successfully"));
    }

    @PostMapping("/unstage")
    @Operation(summary = "Unstage specific files")
    public ResponseEntity<ApiResponse<Void>> unstage(
            @PathVariable UUID projectId,
            @RequestBody GitStageRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        gitCommitService.unstage(projectId, request != null ? request.paths() : null, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(null, "Files unstaged successfully"));
    }

    @PostMapping("/stage-all")
    @Operation(summary = "Stage all workspace changes")
    public ResponseEntity<ApiResponse<Void>> stageAll(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        gitCommitService.stageAll(projectId, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(null, "All changes staged successfully"));
    }

    @PostMapping("/unstage-all")
    @Operation(summary = "Unstage all staged changes")
    public ResponseEntity<ApiResponse<Void>> unstageAll(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        gitCommitService.unstageAll(projectId, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(null, "All changes unstaged successfully"));
    }

    @PostMapping("/commit")
    @Operation(summary = "Create a new Git commit from staged changes")
    public ResponseEntity<ApiResponse<GitCommitResponse>> commit(
            @PathVariable UUID projectId,
            @Valid @RequestBody GitCommitRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        GitCommitResponse response = gitCommitService.commit(projectId, request, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(response, "Changes committed successfully"));
    }

    @GetMapping("/commits")
    @Operation(summary = "Get commit history")
    public ResponseEntity<ApiResponse<List<GitCommitDto>>> getCommits(
            @PathVariable UUID projectId,
            @RequestParam(required = false) String branch,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<GitCommitDto> commits = gitCommitService.getCommits(projectId, branch, page, size, search, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(commits, "Commit history retrieved successfully"));
    }

    @GetMapping("/commits/{commitHash}")
    @Operation(summary = "Get detailed commit information")
    public ResponseEntity<ApiResponse<GitCommitDetailDto>> getCommit(
            @PathVariable UUID projectId,
            @PathVariable String commitHash,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        GitCommitDetailDto commit = gitCommitService.getCommit(projectId, commitHash, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(commit, "Commit details retrieved successfully"));
    }

    @GetMapping("/commits/{commitHash}/diff")
    @Operation(summary = "Get diff for a specific commit")
    public ResponseEntity<ApiResponse<List<GitDiffFileDto>>> getCommitDiff(
            @PathVariable UUID projectId,
            @PathVariable String commitHash,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        GitCommitDetailDto commit = gitCommitService.getCommit(projectId, commitHash, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(commit.changedFiles(), "Commit diff retrieved successfully"));
    }

    @GetMapping("/branches")
    @Operation(summary = "Get list of branches")
    public ResponseEntity<ApiResponse<GitBranchListResponse>> getBranches(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        GitBranchListResponse branches = gitBranchService.getBranches(projectId, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(branches, "Branches retrieved successfully"));
    }

    @PostMapping("/branches")
    @Operation(summary = "Create a new branch")
    public ResponseEntity<ApiResponse<GitBranchDto>> createBranch(
            @PathVariable UUID projectId,
            @Valid @RequestBody GitCreateBranchRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        GitBranchDto branch = gitBranchService.createBranch(projectId, request, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(branch, "Branch created successfully"));
    }

    @PostMapping("/branches/checkout")
    @Operation(summary = "Switch or checkout branch")
    public ResponseEntity<ApiResponse<GitBranchDto>> checkoutBranch(
            @PathVariable UUID projectId,
            @Valid @RequestBody GitCheckoutRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        GitBranchDto branch = gitBranchService.checkoutBranch(projectId, request, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(branch, "Switched to branch " + branch.name()));
    }

    @DeleteMapping("/branches/{branchName}")
    @Operation(summary = "Delete branch")
    public ResponseEntity<ApiResponse<Void>> deleteBranch(
            @PathVariable UUID projectId,
            @PathVariable String branchName,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        gitBranchService.deleteBranch(projectId, branchName, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(null, "Branch deleted successfully"));
    }

    @PostMapping("/fetch")
    @Operation(summary = "Fetch remote updates")
    public ResponseEntity<ApiResponse<GitFetchResponse>> fetch(
            @PathVariable UUID projectId,
            @RequestParam(required = false, defaultValue = "origin") String remote,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        GitFetchResponse response = gitRemoteService.fetch(projectId, remote, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(response, response.message()));
    }

    @PostMapping("/pull")
    @Operation(summary = "Pull updates from remote")
    public ResponseEntity<ApiResponse<GitPullResponse>> pull(
            @PathVariable UUID projectId,
            @RequestParam(required = false, defaultValue = "origin") String remote,
            @RequestParam(required = false) String branch,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        GitPullResponse response = gitRemoteService.pull(projectId, remote, branch, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(response, response.message()));
    }

    @PostMapping("/push")
    @Operation(summary = "Push commits to remote")
    public ResponseEntity<ApiResponse<GitPushResponse>> push(
            @PathVariable UUID projectId,
            @RequestParam(required = false, defaultValue = "origin") String remote,
            @RequestParam(required = false) String branch,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        GitPushResponse response = gitRemoteService.push(projectId, remote, branch, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(response, response.message()));
    }

    @GetMapping("/remotes")
    @Operation(summary = "List project remotes")
    public ResponseEntity<ApiResponse<List<GitRemoteDto>>> getRemotes(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<GitRemoteDto> remotes = gitRemoteService.getRemotes(projectId, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(remotes, "Remotes retrieved successfully"));
    }

    @PostMapping("/remotes")
    @Operation(summary = "Add project remote")
    public ResponseEntity<ApiResponse<GitRemoteDto>> addRemote(
            @PathVariable UUID projectId,
            @Valid @RequestBody GitAddRemoteRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        GitRemoteDto remote = gitRemoteService.addRemote(projectId, request, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(remote, "Remote added successfully"));
    }

    @PostMapping("/ai-commit-message")
    @Operation(summary = "Generate commit message using AI from staged/working diff")
    public ResponseEntity<ApiResponse<AiCommitMessageResponse>> generateCommitMessage(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        AiCommitMessageResponse response = gitAiService.generateCommitMessage(projectId, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(response, "AI commit message suggested"));
    }
}
