package com.devpilot.ai.git.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "github_connections")
public class GitHubConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "github_user_id", length = 100)
    private String githubUserId;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "access_token_encrypted", nullable = false, columnDefinition = "TEXT")
    private String accessTokenEncrypted;

    @Column(name = "token_type", length = 50)
    private String tokenType = "bearer";

    @Column(name = "scopes", length = 255)
    private String scopes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public GitHubConnection() {}

    public GitHubConnection(UUID id, UUID userId, String githubUserId, String username,
                            String accessTokenEncrypted, String tokenType, String scopes,
                            Instant createdAt, Instant updatedAt, Instant revokedAt) {
        this.id = id;
        this.userId = userId;
        this.githubUserId = githubUserId;
        this.username = username;
        this.accessTokenEncrypted = accessTokenEncrypted;
        this.tokenType = tokenType;
        this.scopes = scopes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.revokedAt = revokedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isActive() {
        return revokedAt == null;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getGithubUserId() { return githubUserId; }
    public void setGithubUserId(String githubUserId) { this.githubUserId = githubUserId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAccessTokenEncrypted() { return accessTokenEncrypted; }
    public void setAccessTokenEncrypted(String accessTokenEncrypted) { this.accessTokenEncrypted = accessTokenEncrypted; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public String getScopes() { return scopes; }
    public void setScopes(String scopes) { this.scopes = scopes; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }

    public static class Builder {
        private UUID id;
        private UUID userId;
        private String githubUserId;
        private String username;
        private String accessTokenEncrypted;
        private String tokenType = "bearer";
        private String scopes;
        private Instant createdAt;
        private Instant updatedAt;
        private Instant revokedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder userId(UUID userId) { this.userId = userId; return this; }
        public Builder githubUserId(String githubUserId) { this.githubUserId = githubUserId; return this; }
        public Builder username(String username) { this.username = username; return this; }
        public Builder accessTokenEncrypted(String accessTokenEncrypted) { this.accessTokenEncrypted = accessTokenEncrypted; return this; }
        public Builder tokenType(String tokenType) { this.tokenType = tokenType; return this; }
        public Builder scopes(String scopes) { this.scopes = scopes; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder revokedAt(Instant revokedAt) { this.revokedAt = revokedAt; return this; }

        public GitHubConnection build() {
            return new GitHubConnection(id, userId, githubUserId, username, accessTokenEncrypted, tokenType, scopes, createdAt, updatedAt, revokedAt);
        }
    }
}
