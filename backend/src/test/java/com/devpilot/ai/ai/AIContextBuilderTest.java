package com.devpilot.ai.ai;

import com.devpilot.ai.ai.model.AIContext;
import com.devpilot.ai.ai.prompt.AIContextBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AIContextBuilderTest {

    private final AIContextBuilder contextBuilder = new AIContextBuilder();

    @Test
    @DisplayName("Should accurately detect languages from file extensions")
    void testDetectLanguage() {
        assertEquals("java", AIContextBuilder.detectLanguage("src/App.java", null));
        assertEquals("javascript", AIContextBuilder.detectLanguage("src/main.js", null));
        assertEquals("typescript", AIContextBuilder.detectLanguage("src/main.ts", null));
        assertEquals("python", AIContextBuilder.detectLanguage("backend/server.py", null));
        assertEquals("html", AIContextBuilder.detectLanguage("index.html", null));
        assertEquals("css", AIContextBuilder.detectLanguage("styles.css", null));
        assertEquals("json", AIContextBuilder.detectLanguage("package.json", null));
        assertEquals("sql", AIContextBuilder.detectLanguage("schema.sql", null));
        assertEquals("plaintext", AIContextBuilder.detectLanguage("README", null));
    }

    @Test
    @DisplayName("Should truncate overly large files to avoid context payload explosion")
    void testTruncateLargeFile() {
        String largeContent = "A".repeat(20000);
        AIContext ctx = contextBuilder.build(
                UUID.randomUUID(), "TestProject", UUID.randomUUID(), "Test.java",
                largeContent, "System.out.println(1);", "java", "instruction", "prompt"
        );

        assertTrue(ctx.getFileContent().length() < largeContent.length());
        assertTrue(ctx.getFileContent().contains("[context truncated to stay within limits]"));
    }

    @Test
    @DisplayName("Should truncate selected code if exceeding maximum selection threshold")
    void testTruncateSelection() {
        String largeSelection = "B".repeat(10000);
        AIContext ctx = contextBuilder.build(
                UUID.randomUUID(), "TestProject", UUID.randomUUID(), "Test.java",
                "content", largeSelection, "java", "instruction", "prompt"
        );

        assertTrue(ctx.getSelectedCode().length() < largeSelection.length());
        assertTrue(ctx.getSelectedCode().contains("[truncated]"));
    }
}
