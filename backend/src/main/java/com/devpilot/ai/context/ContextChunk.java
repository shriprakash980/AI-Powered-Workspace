package com.devpilot.ai.context;

import java.util.UUID;

public class ContextChunk {
    private UUID fileId;
    private String path;
    private int startLine;
    private int endLine;
    private String content;
    private int relevanceScore;
    private ChunkType chunkType;

    public ContextChunk() {}

    public ContextChunk(UUID fileId, String path, int startLine, int endLine, String content, int relevanceScore, ChunkType chunkType) {
        this.fileId = fileId;
        this.path = path;
        this.startLine = startLine;
        this.endLine = endLine;
        this.content = content;
        this.relevanceScore = relevanceScore;
        this.chunkType = chunkType;
    }

    public UUID getFileId() {
        return fileId;
    }

    public void setFileId(UUID fileId) {
        this.fileId = fileId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getRelevanceScore() {
        return relevanceScore;
    }

    public void setRelevanceScore(int relevanceScore) {
        this.relevanceScore = relevanceScore;
    }

    public ChunkType getChunkType() {
        return chunkType;
    }

    public void setChunkType(ChunkType chunkType) {
        this.chunkType = chunkType;
    }
}
