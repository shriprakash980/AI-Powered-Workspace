package com.devpilot.ai.patch;

import com.devpilot.ai.config.AIProperties;
import com.devpilot.ai.entity.enums.FileChangeOperation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PatchValidatorTest {

    private PatchValidator validator;

    @BeforeEach
    void setUp() {
        AIProperties props = new AIProperties();
        validator = new PatchValidator(props);
    }

    @Test
    @DisplayName("Should block path traversal attempts")
    void testBlockPathTraversal() {
        ChangeSetProposal proposal = new ChangeSetProposal(
                null,
                "Malicious proposal",
                List.of(new FileChangeProposal(FileChangeOperation.UPDATE, "../../../etc/passwd", "evil content", "hack"))
        );

        PatchValidationResult result = validator.validate(proposal);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("traversal")));
    }

    @Test
    @DisplayName("Should block forbidden sensitive files like .env or .git")
    void testBlockSensitiveFiles() {
        ChangeSetProposal proposal = new ChangeSetProposal(
                null,
                "Attempt to modify .env",
                List.of(new FileChangeProposal(FileChangeOperation.UPDATE, ".env", "SECRET=123", "leak"))
        );

        PatchValidationResult result = validator.validate(proposal);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("sensitive") || e.contains("prohibited")));
    }

    @Test
    @DisplayName("Should accept valid safe proposals")
    void testValidProposal() {
        ChangeSetProposal proposal = new ChangeSetProposal(
                null,
                "Add App.js feature",
                List.of(new FileChangeProposal(FileChangeOperation.CREATE, "src/components/Button.js", "export function Button() {}", "new component"))
        );

        PatchValidationResult result = validator.validate(proposal);
        assertTrue(result.isValid(), "Valid proposal should pass validation");
    }
}
