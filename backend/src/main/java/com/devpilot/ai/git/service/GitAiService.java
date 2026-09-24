package com.devpilot.ai.git.service;

import com.devpilot.ai.ai.model.AIRequest;
import com.devpilot.ai.ai.model.AIResponse;
import com.devpilot.ai.ai.model.ChatMessage;
import com.devpilot.ai.ai.provider.AIProvider;
import com.devpilot.ai.ai.provider.AIProviderFactory;
import com.devpilot.ai.git.dto.AiCommitMessageResponse;
import com.devpilot.ai.git.dto.GitDiffFileDto;
import com.devpilot.ai.git.dto.GitDiffResponse;
import com.devpilot.ai.git.exception.GitException;
import com.devpilot.ai.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class GitAiService {

    private static final Logger log = LoggerFactory.getLogger(GitAiService.class);

    private final GitCommitService gitCommitService;
    private final AIProviderFactory aiProviderFactory;

    public GitAiService(GitCommitService gitCommitService, AIProviderFactory aiProviderFactory) {
        this.gitCommitService = gitCommitService;
        this.aiProviderFactory = aiProviderFactory;
    }

    /**
     * Analyzes staged (or working) git diff and generates a suggested conventional commit message using DevPilot AI.
     */
    public AiCommitMessageResponse generateCommitMessage(UUID projectId, UserPrincipal userPrincipal) {
        GitDiffResponse diffResponse = gitCommitService.getWorkingDiff(projectId, null, true, null, userPrincipal);
        if (diffResponse.files().isEmpty()) {
            // Fallback to unstaged diff if nothing is staged yet
            diffResponse = gitCommitService.getWorkingDiff(projectId, null, false, null, userPrincipal);
        }

        if (diffResponse.files().isEmpty()) {
            throw new GitException("No changed files found in workspace to generate a commit message for.");
        }

        StringBuilder diffSummary = new StringBuilder();
        diffSummary.append("Git Diff Changes Summary:\n");
        for (GitDiffFileDto file : diffResponse.files()) {
            diffSummary.append(String.format("- File: %s (%s, +%d/-%d)\n", file.path(), file.changeType(), file.additions(), file.deletions()));
            if (file.diff() != null && !file.diff().isBlank()) {
                String snippet = file.diff().length() > 600 ? file.diff().substring(0, 600) + "\n...[truncated]" : file.diff();
                diffSummary.append(snippet).append("\n");
            }
            if (diffSummary.length() > 3000) break;
        }

        String prompt = "Based on the following Git diff changes, generate a concise, professional Git commit message following conventional commits format (e.g. feat: ..., fix: ..., refactor: ...). Output ONLY the commit message with no markdown formatting or extra conversational fluff:\n\n"
                + diffSummary;

        try {
            AIProvider provider = aiProviderFactory.getDefaultProvider();
            AIRequest aiReq = new AIRequest(
                    null,
                    List.of(
                            new ChatMessage("system", "You are an expert Git assistant. Generate a concise, professional conventional commit message (e.g. feat: ..., fix: ..., chore: ...). Return ONLY the commit message string, nothing else."),
                            new ChatMessage("user", prompt)
                    ),
                    0.2,
                    150,
                    false
            );
            AIResponse resp = provider.generate(aiReq);
            String aiResult = resp.getContent();

            String cleaned = aiResult != null ? aiResult.trim() : "chore: update workspace files";
            if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length() > 2) {
                cleaned = cleaned.substring(1, cleaned.length() - 1);
            }
            if (cleaned.startsWith("`") && cleaned.endsWith("`") && cleaned.length() > 2) {
                cleaned = cleaned.substring(1, cleaned.length() - 1);
            }

            String firstLine = cleaned.lines().findFirst().orElse(cleaned);
            return new AiCommitMessageResponse(cleaned, firstLine);
        } catch (Exception e) {
            log.warn("AI generation failed for commit message, providing fallback: {}", e.getMessage());
            String fallback = "feat: update " + diffResponse.files().get(0).path();
            return new AiCommitMessageResponse(fallback, fallback);
        }
    }
}
