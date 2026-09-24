package com.devpilot.ai.ai.prompt;

import com.devpilot.ai.ai.model.AIContext;

public final class PromptTemplates {

    private PromptTemplates() {}

    public static final String SYSTEM_PROMPT = """
        You are DevPilot AI, an elite, context-aware AI software engineering assistant integrated directly into the DevPilot IDE.
        You assist developers with programming, architecture, debugging, code generation, refactoring, documentation, and test writing.
        Follow these principles:
        1. Produce idiomatic, production-ready, clean, secure, and well-typed code.
        2. Never truncate critical code with lazy comments like '// implement here' unless requested.
        3. When explaining, be direct, technical, and concise.
        4. When offering code solutions, clearly separate explanation and code blocks with markdown language tags.
        """;

    public static String buildChatSystemPrompt(AIContext ctx) {
        StringBuilder sb = new StringBuilder(SYSTEM_PROMPT);
        if (ctx != null) {
            sb.append("\n\n### Current Workspace Context:");
            if (ctx.getProjectName() != null) {
                sb.append("\n- Active Project: ").append(ctx.getProjectName());
            }
            if (ctx.getFilePath() != null) {
                sb.append("\n- Active File: ").append(ctx.getFilePath());
            }
            if (ctx.getLanguage() != null) {
                sb.append("\n- Language: ").append(ctx.getLanguage());
            }
            if (ctx.getSelectedCode() != null && !ctx.getSelectedCode().isBlank()) {
                sb.append("\n- Current Selection:\n```").append(ctx.getLanguage() != null ? ctx.getLanguage() : "")
                  .append("\n").append(ctx.getSelectedCode()).append("\n```");
            }
            if (ctx.getFileContent() != null && !ctx.getFileContent().isBlank()) {
                sb.append("\n- Active File Context:\n```").append(ctx.getLanguage() != null ? ctx.getLanguage() : "")
                  .append("\n").append(ctx.getFileContent()).append("\n```");
            }
            if (ctx.getProjectContext() != null && !ctx.getProjectContext().isBlank()) {
                sb.append("\n\n").append(ctx.getProjectContext());
            }
        }
        return sb.toString();
    }

    public static String buildCodeActionPrompt(String action, AIContext ctx) {
        String lang = ctx.getLanguage() != null ? ctx.getLanguage() : "text";
        String code = ctx.getSelectedCode() != null ? ctx.getSelectedCode() : "";
        String instruction = ctx.getInstruction() != null ? ctx.getInstruction() : "";
        String fileContext = ctx.getFileContent() != null ? ctx.getFileContent() : "";

        return switch (action.toUpperCase()) {
            case "EXPLAIN" -> """
                Analyze and explain the following %s code:
                ```%s
                %s
                ```
                Provide:
                1. High-level Summary: What this code accomplishes.
                2. Key Components & Logic: Step-by-step breakdown.
                3. Complexity & Edge Cases: Time/space considerations and potential edge cases.
                """.formatted(lang, lang, code);

            case "FIX" -> """
                The following %s code has a bug or issue.
                %s
                Code to fix:
                ```%s
                %s
                ```
                Surrounding file context:
                ```%s
                %s
                ```
                Provide:
                1. Root Cause: What causes the issue.
                2. The Fixed Code: Provide ONLY the corrected code replacement for the selection inside a markdown code fence.
                """.formatted(lang, (instruction.isBlank() ? "" : "Reported issue: " + instruction), lang, code, lang, fileContext);

            case "GENERATE" -> """
                Generate %s code based on the following instruction:
                "%s"
                %s
                Provide clean, idiomatic, fully functional code with appropriate comments and error handling.
                """.formatted(lang, instruction, (code.isBlank() ? "" : "Context code:\n```" + lang + "\n" + code + "\n```"));

            case "REFACTOR" -> """
                Refactor and optimize the following %s code.
                %s
                Code:
                ```%s
                %s
                ```
                Provide:
                1. Improvements Made: Readability, performance, modern idioms, or design pattern enhancements.
                2. Refactored Code: Provide the complete refactored replacement code inside a markdown code block.
                """.formatted(lang, (instruction.isBlank() ? "Focus on readability, best practices, and performance." : "Specific instruction: " + instruction), lang, code);

            case "TESTS" -> """
                Write comprehensive automated unit tests for the following %s code:
                ```%s
                %s
                ```
                Instructions:
                - Use the standard test framework for this language (e.g. JUnit 5, Jest, PyTest, Mocha).
                - Include test cases for normal operation, edge cases, boundary conditions, and invalid inputs.
                - Use assertions effectively and provide meaningful test names.
                """.formatted(lang, lang, code);

            case "DEBUG" -> """
                Debug the following %s code and diagnose possible errors, stack traces, or regressions.
                %s
                Code:
                ```%s
                %s
                ```
                Surrounding file context:
                ```%s
                %s
                ```
                Provide diagnostic steps, probable root cause, and the corrected code.
                """.formatted(lang, (instruction.isBlank() ? "" : "Diagnostic notes: " + instruction), lang, code, lang, fileContext);

            case "DOCUMENT" -> """
                Generate comprehensive documentation (docstrings, Javadoc, or TS doc comments) and explanations for the following %s code:
                ```%s
                %s
                ```
                Provide:
                1. Complete code with embedded doc comments.
                2. Summary of parameters, return values, and exceptions thrown.
                """.formatted(lang, lang, code);

            default -> """
                Process the following %s code according to the instruction:
                "%s"
                ```%s
                %s
                ```
                """.formatted(lang, instruction, lang, code);
        };
    }
}
