package com.devpilot.ai.git.controller;

import com.devpilot.ai.dto.ApiResponse;
import com.devpilot.ai.dto.project.ProjectResponse;
import com.devpilot.ai.git.dto.*;
import com.devpilot.ai.git.service.GitHubOAuthService;
import com.devpilot.ai.git.service.GitHubService;
import com.devpilot.ai.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/github")
@Tag(name = "GitHub Integration", description = "GitHub OAuth, repository cloning/import, and pull requests")
public class GitHubController {

    private final GitHubOAuthService gitHubOAuthService;
    private final GitHubService gitHubService;

    public GitHubController(GitHubOAuthService gitHubOAuthService, GitHubService gitHubService) {
        this.gitHubOAuthService = gitHubOAuthService;
        this.gitHubService = gitHubService;
    }

    @GetMapping("/status")
    @Operation(summary = "Check GitHub account connection status")
    public ResponseEntity<ApiResponse<GitHubStatusResponse>> getStatus(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        GitHubStatusResponse response = gitHubOAuthService.getStatus(userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(response, "GitHub status checked"));
    }

    @GetMapping("/oauth/start")
    @Operation(summary = "Start GitHub OAuth authorization flow")
    public ResponseEntity<ApiResponse<GitHubOAuthStartResponse>> startOAuth() {
        GitHubOAuthStartResponse response = gitHubOAuthService.startOAuth();
        return ResponseEntity.ok(ApiResponse.success(response, "OAuth authorization initiated"));
    }

    @GetMapping("/oauth/callback")
    @Operation(summary = "Handle GitHub OAuth authorization code callback")
    public ResponseEntity<ApiResponse<GitHubStatusResponse>> handleCallback(
            @RequestParam String code,
            @RequestParam(required = false) String state,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        GitHubStatusResponse response = gitHubOAuthService.handleCallback(code, state, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(response, "GitHub account connected successfully"));
    }

    @PostMapping("/disconnect")
    @Operation(summary = "Disconnect GitHub account")
    public ResponseEntity<ApiResponse<Void>> disconnect(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        gitHubOAuthService.disconnect(userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(null, "GitHub account disconnected"));
    }

    @GetMapping("/repositories")
    @Operation(summary = "List user GitHub repositories")
    public ResponseEntity<ApiResponse<List<GitHubRepositoryDto>>> getRepositories(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int perPage,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<GitHubRepositoryDto> repos = gitHubService.getRepositories(userPrincipal, page, perPage, search);
        return ResponseEntity.ok(ApiResponse.success(repos, "Repositories fetched successfully"));
    }

    @PostMapping("/repositories")
    @Operation(summary = "Create a new repository on GitHub")
    public ResponseEntity<ApiResponse<GitHubRepositoryDto>> createRepository(
            @Valid @RequestBody GitHubCreateRepoRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        GitHubRepositoryDto repo = gitHubService.createRepository(request, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(repo, "GitHub repository created successfully"));
    }

    @PostMapping("/repositories/import")
    @Operation(summary = "Import and clone a GitHub repository into DevPilot AI project")
    public ResponseEntity<ApiResponse<ProjectResponse>> importRepository(
            @Valid @RequestBody GitHubImportRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        ProjectResponse project = gitHubService.importRepository(request, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(project, "Repository imported successfully"));
    }

    @GetMapping("/repositories/{owner}/{repo}/pulls")
    @Operation(summary = "List pull requests for a repository")
    public ResponseEntity<ApiResponse<List<GitHubPullRequestDto>>> getPullRequests(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestParam(required = false, defaultValue = "open") String state,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<GitHubPullRequestDto> prs = gitHubService.getPullRequests(owner, repo, state, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(prs, "Pull requests retrieved successfully"));
    }

    @PostMapping("/repositories/{owner}/{repo}/pulls")
    @Operation(summary = "Create a pull request on GitHub")
    public ResponseEntity<ApiResponse<GitHubPullRequestDto>> createPullRequest(
            @PathVariable String owner,
            @PathVariable String repo,
            @Valid @RequestBody GitHubCreatePullRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        GitHubPullRequestDto pr = gitHubService.createPullRequest(owner, repo, request, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(pr, "Pull request created successfully"));
    }
}
