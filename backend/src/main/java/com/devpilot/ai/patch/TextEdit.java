package com.devpilot.ai.patch;

public class TextEdit {
    private int startLine;
    private int endLine;
    private String replacement;

    public TextEdit() {}

    public TextEdit(int startLine, int endLine, String replacement) {
        this.startLine = startLine;
        this.endLine = endLine;
        this.replacement = replacement != null ? replacement : "";
    }

    public int getStartLine() {
        return startLine;
    }

    public void setStartLine(int startLine) {
        this.startLine = startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public void setEndLine(int endLine) {
        this.endLine = endLine;
    }

    public String getReplacement() {
        return replacement;
    }

    public void setReplacement(String replacement) {
        this.replacement = replacement;
    }
}
