package com.devpilot.ai.context;

import com.devpilot.ai.entity.ProjectFile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ContextChunker {

    /**
     * Chunks a file according to context budget and selection.
     */
    public List<ContextChunk> chunkFile(ProjectFile file,
                                        int fileScore,
                                        String selectedCode,
                                        Integer startLine,
                                        Integer endLine,
                                        int maxChunkChars) {
        List<ContextChunk> chunks = new ArrayList<>();
        String content = file.getContent() != null ? file.getContent() : "";

        if (content.isBlank()) {
            return chunks;
        }

        // If specific selected code is provided for this file
        if (selectedCode != null && !selectedCode.isBlank() && content.contains(selectedCode.trim())) {
            int sLine = startLine != null ? startLine : 1;
            int eLine = endLine != null ? endLine : sLine + selectedCode.split("\n").length;
            chunks.add(new ContextChunk(
                    file.getId(),
                    file.getPath(),
                    sLine,
                    eLine,
                    selectedCode.trim(),
                    fileScore + 50,
                    ChunkType.SELECTED
            ));
        }

        // If file content fits into a single chunk
        if (content.length() <= maxChunkChars) {
            int totalLines = content.split("\n").length;
            chunks.add(new ContextChunk(
                    file.getId(),
                    file.getPath(),
                    1,
                    totalLines,
                    content,
                    fileScore,
                    ChunkType.CLASS
            ));
            return chunks;
        }

        // Split into logical line chunks
        String[] lines = content.split("\n", -1);
        StringBuilder currentChunk = new StringBuilder();
        int chunkStartLine = 1;

        // Separate imports header if applicable
        int importEndIndex = -1;
        for (int i = 0; i < Math.min(lines.length, 50); i++) {
            String line = lines[i].trim();
            if (line.startsWith("import ") || line.startsWith("package ") || line.startsWith("from ") || line.startsWith("require(")) {
                importEndIndex = i;
            }
        }

        if (importEndIndex > 0) {
            StringBuilder header = new StringBuilder();
            for (int i = 0; i <= importEndIndex; i++) {
                header.append(lines[i]).append("\n");
            }
            chunks.add(new ContextChunk(
                    file.getId(),
                    file.getPath(),
                    1,
                    importEndIndex + 1,
                    header.toString().trim(),
                    fileScore + 20,
                    ChunkType.IMPORTS
            ));
            chunkStartLine = importEndIndex + 2;
        }

        // Chunk remaining body
        for (int i = chunkStartLine - 1; i < lines.length; i++) {
            currentChunk.append(lines[i]).append("\n");

            if (currentChunk.length() >= maxChunkChars || i == lines.length - 1) {
                int chunkEndLine = i + 1;
                chunks.add(new ContextChunk(
                        file.getId(),
                        file.getPath(),
                        chunkStartLine,
                        chunkEndLine,
                        currentChunk.toString().trim(),
                        fileScore,
                        ChunkType.METHOD
                ));
                currentChunk = new StringBuilder();
                chunkStartLine = chunkEndLine + 1;
            }
        }

        return chunks;
    }
}
