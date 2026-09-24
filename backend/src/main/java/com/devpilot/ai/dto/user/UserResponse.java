package com.devpilot.ai.dto.user;

import com.devpilot.ai.entity.enums.UserStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserResponse {
    private UUID id;
    private String fullName;
    private String email;
    private List<String> roles = new ArrayList<>();
    private UserStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public UserResponse() {}

    public UserResponse(UUID id, String fullName, String email, List<String> roles, UserStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.roles = roles != null ? roles : new ArrayList<>();
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public static class Builder {
        private UUID id;
        private String fullName;
        private String email;
        private List<String> roles = new ArrayList<>();
        private UserStatus status;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder fullName(String fullName) { this.fullName = fullName; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder roles(List<String> roles) { this.roles = roles; return this; }
        public Builder status(UserStatus status) { this.status = status; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public UserResponse build() {
            return new UserResponse(id, fullName, email, roles, status, createdAt, updatedAt);
        }
    }
}
