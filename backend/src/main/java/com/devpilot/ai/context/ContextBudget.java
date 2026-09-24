package com.devpilot.ai.context;

public class ContextBudget {
    private final int maxFiles;
    private final int maxTotalChars;
    private final int maxChunkChars;

    private int usedChars = 0;
    private int filesCount = 0;

    public ContextBudget(int maxFiles, int maxTotalChars, int maxChunkChars) {
        this.maxFiles = maxFiles;
        this.maxTotalChars = maxTotalChars;
        this.maxChunkChars = maxChunkChars;
    }

    public boolean canIncludeFile() {
        return filesCount < maxFiles && usedChars < maxTotalChars;
    }

    public int getRemainingChars() {
        return Math.max(0, maxTotalChars - usedChars);
    }

    public boolean tryConsume(int chars) {
        if (usedChars + chars <= maxTotalChars) {
            usedChars += chars;
            return true;
        }
        return false;
    }

    public void incrementFileCount() {
        this.filesCount++;
    }

    public int getUsedChars() {
        return usedChars;
    }

    public int getFilesCount() {
        return filesCount;
    }

    public int getMaxTotalChars() {
        return maxTotalChars;
    }

    public int getMaxChunkChars() {
        return maxChunkChars;
    }

    public int getEstimatedTokens() {
        return usedChars / 4;
    }
}
