package com.devpilot.ai.git.service;

import com.devpilot.ai.dto.project.ProjectResponse;
import com.devpilot.ai.entity.Project;
import com.devpilot.ai.entity.enums.ProjectStatus;
import com.devpilot.ai.entity.enums.ProjectTemplate;
import com.devpilot.ai.git.dto.*;
import com.devpilot.ai.git.entity.enums.GitOperationType;
import com.devpilot.ai.git.exception.GitException;
import com.devpilot.ai.git.exception.GitHubApiException;
import com.devpilot.ai.git.exception.GitHubRateLimitException;
import com.devpilot.ai.repository.ProjectRepository;
import com.devpilot.ai.security.UserPrincipal;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.time.Instant;
import java.util.*;

@Service
public class GitHubService {

    private static final Logger log = LoggerFactory.getLogger(GitHubService.class);

    private final String apiBaseUrl;
    private final GitHubOAuthService oauthService;
    private final GitWorkspaceManager workspaceManager;
    private final GitService gitService;
    private final ProjectRepository projectRepository;
    private final RestTemplate restTemplate;

    public GitHubService(
            @Value("${app.github.api-base-url:https://api.github.com}") String apiBaseUrl,
            GitHubOAuthService oauthService,
            GitWorkspaceManager workspaceManager,
            GitService gitService,
            ProjectRepository projectRepository,
            RestTemplateBuilder restTemplateBuilder) {
        this.apiBaseUrl = apiBaseUrl;
        this.oauthService = oauthService;
        this.workspaceManager = workspaceManager;
        this.gitService = gitService;
        this.projectRepository = projectRepository;
        this.restTemplate = restTemplateBuilder.build();
    }

    /**
     * Lists repositories for the authenticated GitHub user.
     */
    public List<GitHubRepositoryDto> getRepositories(UserPrincipal userPrincipal, int page, int perPage, String search) {
        String token = oauthService.getDecryptedToken(userPrincipal != null ? userPrincipal.getId() : null);
        if (token == null) {
            throw new GitHubApiException(401, "GITHUB_NOT_CONNECTED", "GitHub account is not connected. Please connect via Settings > Integrations.");
        }

        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            int p = Math.max(1, page);
            int size = Math.min(100, Math.max(1, perPage));
            String url = String.format("%s/user/repos?sort=updated&per_page=%d&page=%d&type=all", apiBaseUrl, size, p);

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );

            List<Map<String, Object>> repoList = response.getBody();
            if (repoList == null) return Collections.emptyList();

            String searchLower = (search != null && !search.isBlank()) ? search.trim().toLowerCase() : null;
            List<GitHubRepositoryDto> result = new ArrayList<>();

            for (Map<String, Object> r : repoList) {
                String name = String.valueOf(r.get("name"));
                String fullName = String.valueOf(r.get("full_name"));
                if (searchLower != null && !name.toLowerCase().contains(searchLower) && !fullName.toLowerCase().contains(searchLower)) {
                    continue;
                }

                result.add(new GitHubRepositoryDto(
                        r.get("id") != null ? Long.valueOf(r.get("id").toString()) : null,
                        name,
                        fullName,
                        Boolean.TRUE.equals(r.get("private")),
                        r.get("default_branch") != null ? r.get("default_branch").toString() : "main",
                        r.get("html_url") != null ? r.get("html_url").toString() : "",
                        r.get("clone_url") != null ? r.get("clone_url").toString() : "",
                        r.get("description") != null ? r.get("description").toString() : "",
                        r.get("updated_at") != null ? r.get("updated_at").toString() : ""
                ));
            }

            return result;
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new GitHubRateLimitException("GitHub API rate limit exceeded");
        } catch (HttpClientErrorException e) {
            log.error("GitHub API error fetching repositories: {} - {}", e.getStatusCode(), e.getMessage());
            throw new GitHubApiException(e.getStatusCode().value(), "GITHUB_API_ERROR", e.getStatusText());
        } catch (Exception e) {
            log.error("Failed to query GitHub repositories: {}", e.getMessage());
            throw new GitHubApiException(500, "GITHUB_ERROR", e.getMessage());
        }
    }

    /**
     * Creates a new GitHub repository for the user.
     */
    public GitHubRepositoryDto createRepository(GitHubCreateRepoRequest request, UserPrincipal userPrincipal) {
        String token = oauthService.getDecryptedToken(userPrincipal != null ? userPrincipal.getId() : null);
        if (token == null) {
            throw new GitHubApiException(401, "GITHUB_NOT_CONNECTED", "GitHub account is not connected.");
        }

        try {
            HttpHeaders headers = createAuthHeaders(token);
            Map<String, Object> body = new HashMap<>();
            body.put("name", request.name().trim());
            if (request.description() != null) body.put("description", request.description().trim());
            body.put("private", request.isPrivate());
            body.put("auto_init", request.autoInit());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(apiBaseUrl + "/user/repos", entity, Map.class);

            Map<?, ?> r = response.getBody();
            if (r == null) throw new GitHubApiException(500, "CREATE_FAILED", "Failed to parse repository response");

            return new GitHubRepositoryDto(
                    r.get("id") != null ? Long.valueOf(r.get("id").toString()) : null,
                    String.valueOf(r.get("name")),
                    String.valueOf(r.get("full_name")),
                    Boolean.TRUE.equals(r.get("private")),
                    r.get("default_branch") != null ? r.get("default_branch").toString() : "main",
                    String.valueOf(r.get("html_url")),
                    String.valueOf(r.get("clone_url")),
                    r.get("description") != null ? r.get("description").toString() : "",
                    r.get("updated_at") != null ? r.get("updated_at").toString() : ""
            );
        } catch (HttpClientErrorException e) {
            throw new GitHubApiException(e.getStatusCode().value(), "GITHUB_CREATE_ERROR", e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new GitHubApiException(500, "GITHUB_ERROR", e.getMessage());
        }
    }

    /**
     * Imports a GitHub repository: clones into isolated workspace, creates project, and indexes files into DB.
     */
    @Transactional
    public ProjectResponse importRepository(GitHubImportRequest request, UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            throw new GitHubApiException(401, "UNAUTHORIZED", "Authentication required to import repository");
        }

        String repoTarget = request.repository().trim();
        String cloneUrl;
        if (repoTarget.startsWith("https://") || repoTarget.startsWith("http://")) {
            cloneUrl = repoTarget;
        } else {
            cloneUrl = "https://github.com/" + repoTarget + ".git";
        }

        UUID projectId = UUID.randomUUID();
        File targetDir = workspaceManager.getProjectDirectory(projectId);

        String token = oauthService.getDecryptedToken(userPrincipal.getId());

        try {
            var cloneCmd = Git.cloneRepository()
                    .setURI(cloneUrl)
                    .setDirectory(targetDir)
                    .setCloneAllBranches(false);

            if (token != null && !token.isBlank()) {
                cloneCmd.setCredentialsProvider(new UsernamePasswordCredentialsProvider(token, ""));
            }

            try (Git git = cloneCmd.call()) {
                workspaceManager.secureGitConfig(git);

                String defaultBranch = "main";
                try {
                    defaultBranch = git.getRepository().getBranch();
                } catch (Exception ignored) {}

                ProjectTemplate template = ProjectTemplate.HTML_CSS_JS;
                if (request.template() != null) {
                    try {
                        template = ProjectTemplate.valueOf(request.template());
                    } catch (Exception ignored) {}
                }

                Project project = Project.builder()
                        .id(projectId)
                        .name(request.projectName().trim())
                        .description(request.description() != null ? request.description().trim() : "Imported from " + repoTarget)
                        .template(template)
                        .language("java") // defaults to multi-language
                        .framework("standard")
                        .status(ProjectStatus.ACTIVE)
                        .ownerId(userPrincipal.getId())
                        .repositoryUrl(cloneUrl)
                        .gitEnabled(true)
                        .defaultBranch(defaultBranch)
                        .gitProvider("GITHUB")
                        .lastFetchedAt(Instant.now())
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();

                Project savedProject = projectRepository.save(project);

                // Index disk files into database ProjectFiles
                workspaceManager.syncDiskFilesToDb(projectId, savedProject);

                gitService.logOperation(projectId, userPrincipal.getId(),
                        GitOperationType.CLONE, defaultBranch, null, "SUCCESS", "Imported GitHub repository " + repoTarget);

                log.info("Successfully imported GitHub repo {} as DevPilot project {}", repoTarget, savedProject.getId());

                return ProjectResponse.builder()
                        .id(savedProject.getId())
                        .name(savedProject.getName())
                        .description(savedProject.getDescription())
                        .template(savedProject.getTemplate())
                        .language(savedProject.getLanguage())
                        .framework(savedProject.getFramework())
                        .status(savedProject.getStatus())
                        .ownerId(savedProject.getOwnerId())
                        .repositoryUrl(savedProject.getRepositoryUrl())
                        .gitEnabled(true)
                        .defaultBranch(defaultBranch)
                        .gitProvider("GITHUB")
                        .lastFetchedAt(savedProject.getLastFetchedAt())
                        .createdAt(savedProject.getCreatedAt())
                        .updatedAt(savedProject.getUpdatedAt())
                        .build();
            }
        } catch (Exception e) {
            log.error("Failed to clone and import GitHub repository {}: {}", repoTarget, e.getMessage());
            throw new GitException("Failed to import repository from GitHub: " + e.getMessage(), e);
        }
    }

    /**
     * Lists pull requests for a repository.
     */
    public List<GitHubPullRequestDto> getPullRequests(String owner, String repo, String state, UserPrincipal userPrincipal) {
        String token = oauthService.getDecryptedToken(userPrincipal != null ? userPrincipal.getId() : null);
        if (token == null) {
            throw new GitHubApiException(401, "GITHUB_NOT_CONNECTED", "GitHub account is not connected.");
        }

        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String prState = (state != null && !state.isBlank()) ? state.trim().toLowerCase() : "open";
            String url = String.format("%s/repos/%s/%s/pulls?state=%s", apiBaseUrl, owner, repo, prState);

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );

            List<Map<String, Object>> prs = response.getBody();
            if (prs == null) return Collections.emptyList();

            List<GitHubPullRequestDto> result = new ArrayList<>();
            for (Map<String, Object> pr : prs) {
                Map<?, ?> head = (Map<?, ?>) pr.get("head");
                Map<?, ?> base = (Map<?, ?>) pr.get("base");
                Map<?, ?> user = (Map<?, ?>) pr.get("user");

                result.add(new GitHubPullRequestDto(
                        pr.get("number") != null ? (Integer) pr.get("number") : 0,
                        String.valueOf(pr.get("title")),
                        pr.get("body") != null ? pr.get("body").toString() : "",
                        String.valueOf(pr.get("state")),
                        String.valueOf(pr.get("html_url")),
                        head != null ? String.valueOf(head.get("ref")) : "",
                        base != null ? String.valueOf(base.get("ref")) : "",
                        user != null ? String.valueOf(user.get("login")) : "",
                        pr.get("created_at") != null ? pr.get("created_at").toString() : ""
                ));
            }

            return result;
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new GitHubRateLimitException("GitHub API rate limit exceeded");
        } catch (HttpClientErrorException e) {
            throw new GitHubApiException(e.getStatusCode().value(), "GITHUB_PR_ERROR", e.getMessage());
        } catch (Exception e) {
            throw new GitHubApiException(500, "GITHUB_ERROR", e.getMessage());
        }
    }

    /**
     * Creates a new pull request on GitHub.
     */
    public GitHubPullRequestDto createPullRequest(String owner, String repo, GitHubCreatePullRequest request, UserPrincipal userPrincipal) {
        String token = oauthService.getDecryptedToken(userPrincipal != null ? userPrincipal.getId() : null);
        if (token == null) {
            throw new GitHubApiException(401, "GITHUB_NOT_CONNECTED", "GitHub account is not connected.");
        }

        try {
            HttpHeaders headers = createAuthHeaders(token);
            Map<String, Object> body = new HashMap<>();
            body.put("title", request.title().trim());
            if (request.body() != null) body.put("body", request.body().trim());
            body.put("head", request.head().trim());
            body.put("base", request.base().trim());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String url = String.format("%s/repos/%s/%s/pulls", apiBaseUrl, owner, repo);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            Map<?, ?> pr = response.getBody();
            if (pr == null) throw new GitHubApiException(500, "PR_CREATION_FAILED", "Empty PR response");

            Map<?, ?> head = (Map<?, ?>) pr.get("head");
            Map<?, ?> base = (Map<?, ?>) pr.get("base");
            Map<?, ?> user = (Map<?, ?>) pr.get("user");

            return new GitHubPullRequestDto(
                    pr.get("number") != null ? (Integer) pr.get("number") : 0,
                    String.valueOf(pr.get("title")),
                    pr.get("body") != null ? pr.get("body").toString() : "",
                    String.valueOf(pr.get("state")),
                    String.valueOf(pr.get("html_url")),
                    head != null ? String.valueOf(head.get("ref")) : "",
                    base != null ? String.valueOf(base.get("ref")) : "",
                    user != null ? String.valueOf(user.get("login")) : "",
                    pr.get("created_at") != null ? pr.get("created_at").toString() : ""
            );
        } catch (HttpClientErrorException e) {
            throw new GitHubApiException(e.getStatusCode().value(), "GITHUB_PR_ERROR", e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new GitHubApiException(500, "GITHUB_ERROR", e.getMessage());
        }
    }

    private HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("User-Agent", "DevPilot-AI-Workspace");
        return headers;
    }
}
