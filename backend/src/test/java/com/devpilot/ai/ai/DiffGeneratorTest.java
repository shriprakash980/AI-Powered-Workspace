package com.devpilot.ai.ai;

import com.devpilot.ai.ai.util.DiffGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DiffGeneratorTest {

    @Test
    @DisplayName("Should extract raw code from markdown code fences")
    void testExtractCodeFromMarkdown() {
        String fenced = "```java\nint x = 10;\nSystem.out.println(x);\n```";
        String extracted = DiffGenerator.extractCodeFromMarkdown(fenced);
        assertEquals("int x = 10;\nSystem.out.println(x);", extracted);

        String plain = "int x = 10;";
        assertEquals("int x = 10;", DiffGenerator.extractCodeFromMarkdown(plain));
    }

    @Test
    @DisplayName("Should generate unified diff with addition and deletion markers")
    void testGenerateUnifiedDiff() {
        String original = "int a = 1;\nint b = 2;\nreturn a + b;";
        String modified = "int a = 1;\nint b = 3;\nreturn a + b;";

        String diff = DiffGenerator.generateUnifiedDiff(original, modified, "calc.java");
        assertNotNull(diff);
        assertTrue(diff.contains("--- a/calc.java"));
        assertTrue(diff.contains("+++ b/calc.java"));
        assertTrue(diff.contains("-int b = 2;"));
        assertTrue(diff.contains("+int b = 3;"));
    }
}
