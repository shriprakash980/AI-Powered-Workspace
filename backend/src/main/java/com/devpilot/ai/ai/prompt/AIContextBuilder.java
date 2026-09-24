package com.devpilot.ai.ai.prompt;

import com.devpilot.ai.ai.model.AIContext;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AIContextBuilder {

    public static final int MAX_FILE_CONTEXT_CHARS = 12000;
    public static final int MAX_SELECTION_CHARS = 6000;

    public AIContext build(UUID projectId, String projectName, UUID fileId, String filePath, String rawFileContent, String rawSelectedCode, String language, String instruction, String userPrompt) {
        AIContext ctx = new AIContext();
        ctx.setProjectId(projectId);
        ctx.setProjectName(projectName);
        ctx.setFileId(fileId);
        ctx.setFilePath(filePath);
        ctx.setLanguage(detectLanguage(filePath, language));

        if (rawSelectedCode != null) {
            String selected = rawSelectedCode.trim();
            if (selected.length() > MAX_SELECTION_CHARS) {
                selected = selected.substring(0, MAX_SELECTION_CHARS) + "\n... [truncated]";
            }
            ctx.setSelectedCode(selected);
        }

        if (rawFileContent != null) {
            String content = rawFileContent.trim();
            if (content.length() > MAX_FILE_CONTEXT_CHARS) {
                content = content.substring(0, MAX_FILE_CONTEXT_CHARS) + "\n... [context truncated to stay within limits]";
            }
            ctx.setFileContent(content);
        }

        ctx.setInstruction(instruction != null ? instruction.trim() : "");
        ctx.setUserPrompt(userPrompt != null ? userPrompt.trim() : "");

        return ctx;
    }

    public static String detectLanguage(String filePath, String fallbackLanguage) {
        if (filePath != null && filePath.contains(".")) {
            String ext = filePath.substring(filePath.lastIndexOf('.') + 1).toLowerCase();
            return switch (ext) {
                case "java" -> "java";
                case "js", "mjs", "cjs" -> "javascript";
                case "ts" -> "typescript";
                case "py" -> "python";
                case "html", "htm" -> "html";
                case "css" -> "css";
                case "json" -> "json";
                case "xml" -> "xml";
                case "sql" -> "sql";
                case "md" -> "markdown";
                case "sh", "bash" -> "bash";
                case "yaml", "yml" -> "yaml";
                case "cpp", "cc", "cxx" -> "cpp";
                case "c", "h" -> "c";
                case "rs" -> "rust";
                case "go" -> "go";
                case "php" -> "php";
                default -> (fallbackLanguage != null && !fallbackLanguage.isBlank()) ? fallbackLanguage : "plaintext";
            };
        }
        return (fallbackLanguage != null && !fallbackLanguage.isBlank()) ? fallbackLanguage : "plaintext";
    }
}
