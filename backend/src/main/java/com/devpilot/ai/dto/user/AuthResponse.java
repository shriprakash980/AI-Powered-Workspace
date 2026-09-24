package com.devpilot.ai.dto.user;

public class AuthResponse {

    private String accessToken;
    private String tokenType = "Bearer";
    private long expiresIn;
    private String refreshToken;
    private UserResponse user;

    public AuthResponse() {}

    public AuthResponse(String accessToken, String tokenType, long expiresIn, String refreshToken, UserResponse user) {
        this.accessToken = accessToken;
        this.tokenType = tokenType != null ? tokenType : "Bearer";
        this.expiresIn = expiresIn;
        this.refreshToken = refreshToken;
        this.user = user;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public UserResponse getUser() { return user; }
    public void setUser(UserResponse user) { this.user = user; }

    public static class Builder {
        private String accessToken;
        private String tokenType = "Bearer";
        private long expiresIn;
        private String refreshToken;
        private UserResponse user;

        public Builder accessToken(String accessToken) { this.accessToken = accessToken; return this; }
        public Builder tokenType(String tokenType) { this.tokenType = tokenType; return this; }
        public Builder expiresIn(long expiresIn) { this.expiresIn = expiresIn; return this; }
        public Builder refreshToken(String refreshToken) { this.refreshToken = refreshToken; return this; }
        public Builder user(UserResponse user) { this.user = user; return this; }

        public AuthResponse build() {
            return new AuthResponse(accessToken, tokenType, expiresIn, refreshToken, user);
        }
    }
}
