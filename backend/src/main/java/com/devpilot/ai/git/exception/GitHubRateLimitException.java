package com.devpilot.ai.git.exception;

public class GitHubRateLimitException extends GitHubApiException {
    public GitHubRateLimitException(String message) {
        super(429, "GITHUB_RATE_LIMITED", message != null ? message : "GitHub API rate limit exceeded. Please wait a few moments before trying again.");
    }
}
