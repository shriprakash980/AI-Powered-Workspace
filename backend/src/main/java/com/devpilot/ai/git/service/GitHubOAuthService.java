package com.devpilot.ai.git.service;

import com.devpilot.ai.git.dto.GitHubOAuthStartResponse;
import com.devpilot.ai.git.dto.GitHubStatusResponse;
import com.devpilot.ai.git.entity.GitHubConnection;
import com.devpilot.ai.git.exception.GitHubApiException;
import com.devpilot.ai.git.repository.GitHubConnectionRepository;
import com.devpilot.ai.git.security.TokenEncryptionService;
import com.devpilot.ai.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Service
public class GitHubOAuthService {

    private static final Logger log = LoggerFactory.getLogger(GitHubOAuthService.class);

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final GitHubConnectionRepository connectionRepository;
    private final TokenEncryptionService encryptionService;
    private final RestTemplate restTemplate;

    public GitHubOAuthService(
            @Value("${app.github.client-id:}") String clientId,
            @Value("${app.github.client-secret:}") String clientSecret,
            @Value("${app.github.redirect-uri:http://localhost:5500/settings.html?github=callback}") String redirectUri,
            GitHubConnectionRepository connectionRepository,
            TokenEncryptionService encryptionService,
            RestTemplateBuilder restTemplateBuilder) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.connectionRepository = connectionRepository;
        this.encryptionService = encryptionService;
        this.restTemplate = restTemplateBuilder.build();
    }

    /**
     * Initiates the server-side OAuth flow.
     */
    public GitHubOAuthStartResponse startOAuth() {
        if (clientId == null || clientId.isBlank()) {
            throw new GitHubApiException(400, "GITHUB_NOT_CONFIGURED", "GitHub OAuth Client ID is not configured on server.");
        }
        String state = UUID.randomUUID().toString();
        String scopes = "repo,user:email";
        String authUrl = String.format(
                "https://github.com/login/oauth/authorize?client_id=%s&redirect_uri=%s&scope=%s&state=%s",
                URLEncoder.encode(clientId, StandardCharsets.UTF_8),
                URLEncoder.encode(redirectUri, StandardCharsets.UTF_8),
                URLEncoder.encode(scopes, StandardCharsets.UTF_8),
                URLEncoder.encode(state, StandardCharsets.UTF_8)
        );

        return new GitHubOAuthStartResponse(authUrl, state);
    }

    /**
     * Handles OAuth callback: exchanges authorization code for access token, queries user profile, and stores encrypted token.
     */
    @Transactional
    public GitHubStatusResponse handleCallback(String code, String state, UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            throw new GitHubApiException(401, "UNAUTHORIZED", "User must be authenticated to connect GitHub account.");
        }
        if (code == null || code.isBlank()) {
            throw new GitHubApiException(400, "INVALID_CODE", "Missing OAuth authorization code from GitHub.");
        }

        try {
            // 1. Exchange code for access token
            Map<String, String> body = new HashMap<>();
            body.put("client_id", clientId);
            body.put("client_secret", clientSecret);
            body.put("code", code);
            body.put("redirect_uri", redirectUri);

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(
                    "https://github.com/login/oauth/access_token",
                    requestEntity,
                    Map.class
            );

            Map<?, ?> rawTokenData = tokenResponse.getBody();
            if (rawTokenData == null || !rawTokenData.containsKey("access_token")) {
                String errorDesc = rawTokenData != null ? String.valueOf(rawTokenData.get("error_description")) : "Unknown error";
                throw new GitHubApiException(400, "OAUTH_EXCHANGE_FAILED", "Failed to obtain access token: " + errorDesc);
            }

            String accessToken = String.valueOf(rawTokenData.get("access_token"));
            String tokenType = rawTokenData.get("token_type") != null ? String.valueOf(rawTokenData.get("token_type")) : "bearer";
            String scopes = rawTokenData.get("scope") != null ? String.valueOf(rawTokenData.get("scope")) : "repo,user:email";

            // 2. Fetch authenticated GitHub user profile
            HttpHeaders userHeaders = new HttpHeaders();
            userHeaders.setBearerAuth(accessToken);
            userHeaders.set("User-Agent", "DevPilot-AI-Workspace");
            HttpEntity<Void> userEntity = new HttpEntity<>(userHeaders);

            ResponseEntity<Map> userResponse = restTemplate.exchange(
                    "https://api.github.com/user",
                    HttpMethod.GET,
                    userEntity,
                    Map.class
            );

            Map<?, ?> userData = userResponse.getBody();
            String username = userData != null ? String.valueOf(userData.get("login")) : "GitHub User";
            String githubUserId = userData != null ? String.valueOf(userData.get("id")) : null;

            // 3. Encrypt access token at rest
            String encryptedToken = encryptionService.encrypt(accessToken);

            // 4. Save or update connection
            Optional<GitHubConnection> existing = connectionRepository.findByUserIdAndRevokedAtIsNull(userPrincipal.getId());
            GitHubConnection connection;
            if (existing.isPresent()) {
                connection = existing.get();
                connection.setUsername(username);
                connection.setGithubUserId(githubUserId);
                connection.setAccessTokenEncrypted(encryptedToken);
                connection.setTokenType(tokenType);
                connection.setScopes(scopes);
                connection.setRevokedAt(null);
            } else {
                connection = GitHubConnection.builder()
                        .userId(userPrincipal.getId())
                        .username(username)
                        .githubUserId(githubUserId)
                        .accessTokenEncrypted(encryptedToken)
                        .tokenType(tokenType)
                        .scopes(scopes)
                        .build();
            }

            connectionRepository.save(connection);
            log.info("Successfully connected GitHub account @{} for user {}", username, userPrincipal.getId());

            return new GitHubStatusResponse(true, username, scopes, connection.getCreatedAt());
        } catch (GitHubApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to complete GitHub OAuth exchange: {}", e.getMessage());
            throw new GitHubApiException(500, "GITHUB_OAUTH_ERROR", "Failed to connect to GitHub: " + e.getMessage());
        }
    }

    /**
     * Checks if current user is connected to GitHub.
     */
    public GitHubStatusResponse getStatus(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return new GitHubStatusResponse(false, null, null, null);
        }

        Optional<GitHubConnection> conn = connectionRepository.findByUserIdAndRevokedAtIsNull(userPrincipal.getId());
        if (conn.isPresent() && conn.get().isActive()) {
            GitHubConnection c = conn.get();
            return new GitHubStatusResponse(true, c.getUsername(), c.getScopes(), c.getCreatedAt());
        }

        return new GitHubStatusResponse(false, null, null, null);
    }

    /**
     * Disconnects/revokes GitHub connection for user.
     */
    @Transactional
    public void disconnect(UserPrincipal userPrincipal) {
        if (userPrincipal == null) return;
        Optional<GitHubConnection> conn = connectionRepository.findByUserIdAndRevokedAtIsNull(userPrincipal.getId());
        conn.ifPresent(c -> {
            c.setRevokedAt(Instant.now());
            connectionRepository.save(c);
            log.info("Revoked GitHub connection for user {}", userPrincipal.getId());
        });
    }

    /**
     * Retrieves decrypted token strictly for server-side API calls.
     */
    public String getDecryptedToken(UUID userId) {
        if (userId == null) return null;
        Optional<GitHubConnection> conn = connectionRepository.findByUserIdAndRevokedAtIsNull(userId);
        if (conn.isPresent() && conn.get().getAccessTokenEncrypted() != null) {
            return encryptionService.decrypt(conn.get().getAccessTokenEncrypted());
        }
        return null;
    }
}
