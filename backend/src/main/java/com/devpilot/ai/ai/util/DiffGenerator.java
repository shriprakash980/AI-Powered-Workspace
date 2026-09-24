package com.devpilot.ai.ai.util;

public final class DiffGenerator {

    private DiffGenerator() {}

    /**
     * Generates a simple unified diff representation between original and modified strings.
     */
    public static String generateUnifiedDiff(String original, String modified, String filePath) {
        if (original == null) original = "";
        if (modified == null) modified = "";

        String path = (filePath != null && !filePath.isBlank()) ? filePath : "editor-buffer";
        String[] originalLines = original.split("\\r?\\n", -1);
        String[] modifiedLines = modified.split("\\r?\\n", -1);

        StringBuilder diff = new StringBuilder();
        diff.append("--- a/").append(path).append("\n");
        diff.append("+++ b/").append(path).append("\n");

        int max = Math.max(originalLines.length, modifiedLines.length);
        for (int i = 0; i < max; i++) {
            String orig = i < originalLines.length ? originalLines[i] : null;
            String mod = i < modifiedLines.length ? modifiedLines[i] : null;

            if (orig != null && mod != null) {
                if (orig.equals(mod)) {
                    diff.append(" ").append(orig).append("\n");
                } else {
                    diff.append("-").append(orig).append("\n");
                    diff.append("+").append(mod).append("\n");
                }
            } else if (orig != null) {
                diff.append("-").append(orig).append("\n");
            } else if (mod != null) {
                diff.append("+").append(mod).append("\n");
            }
        }

        return diff.toString();
    }

    /**
     * Helper to extract clean code from markdown code fences anywhere in the AI response text.
     */
    public static String extractCodeFromMarkdown(String text) {
        if (text == null) return "";
        String trimmed = text.trim();
        int firstFence = trimmed.indexOf("```");
        if (firstFence != -1) {
            int newlineAfterFirstFence = trimmed.indexOf('\n', firstFence);
            if (newlineAfterFirstFence != -1) {
                int closingFence = trimmed.indexOf("```", newlineAfterFirstFence);
                if (closingFence != -1) {
                    return trimmed.substring(newlineAfterFirstFence + 1, closingFence).trim();
                }
            }
        }
        return trimmed;
    }
}
