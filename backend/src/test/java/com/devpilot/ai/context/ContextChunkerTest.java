package com.devpilot.ai.context;

import com.devpilot.ai.entity.ProjectFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ContextChunkerTest {

    private ContextChunker contextChunker;

    @BeforeEach
    void setUp() {
        contextChunker = new ContextChunker();
    }

    @Test
    @DisplayName("Should extract selected lines verbatim as ChunkType.SELECTED")
    void testExtractSelectionVerbatim() {
        UUID fileId = UUID.randomUUID();
        ProjectFile file = ProjectFile.builder()
                .id(fileId)
                .path("src/Main.java")
                .content("line 1\nline 2\nline 3\nline 4\nline 5")
                .build();

        List<ContextChunk> chunks = contextChunker.chunkFile(file, 100, "line 2\nline 3", 2, 3, 1000);

        assertFalse(chunks.isEmpty());
        ContextChunk selectedChunk = chunks.stream()
                .filter(c -> c.getChunkType() == ChunkType.SELECTED)
                .findFirst()
                .orElse(null);

        assertNotNull(selectedChunk);
        assertEquals("line 2\nline 3", selectedChunk.getContent().trim());
        assertEquals(2, selectedChunk.getStartLine());
        assertEquals(3, selectedChunk.getEndLine());
    }

    @Test
    @DisplayName("Should respect maxChunkChars limit and create multiple chunks")
    void testRespectMaxChunkChars() {
        UUID fileId = UUID.randomUUID();
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 50; i++) {
            sb.append("This is line number ").append(i).append(" with lots of text to test chunking.\n");
        }

        ProjectFile file = ProjectFile.builder()
                .id(fileId)
                .path("LargeFile.txt")
                .content(sb.toString())
                .build();

        int maxChunkChars = 200;
        List<ContextChunk> chunks = contextChunker.chunkFile(file, 50, null, null, null, maxChunkChars);

        assertTrue(chunks.size() > 1, "Should create multiple chunks for large content");
        for (ContextChunk chunk : chunks) {
            assertTrue(chunk.getContent().length() <= maxChunkChars + 100, "Each chunk should be within or near limit");
        }
    }
}
