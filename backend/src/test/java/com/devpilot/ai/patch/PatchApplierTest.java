package com.devpilot.ai.patch;

import com.devpilot.ai.entity.enums.FileChangeOperation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PatchApplierTest {

    private PatchApplier patchApplier;

    @BeforeEach
    void setUp() {
        patchApplier = new PatchApplier();
    }

    @Test
    @DisplayName("Should return proposedContent directly when provided")
    void testProposedContentDirect() {
        FileChangeProposal proposal = new FileChangeProposal(
                FileChangeOperation.UPDATE,
                "test.js",
                "const x = 42;",
                "update"
        );

        String result = patchApplier.computeProposedContent("const x = 1;", proposal);
        assertEquals("const x = 42;", result);
    }

    @Test
    @DisplayName("Should apply line edits in descending order properly")
    void testApplyLineEdits() {
        String original = "line 1\nline 2\nline 3\nline 4";
        FileChangeProposal proposal = new FileChangeProposal();
        proposal.setFilePath("test.txt");
        proposal.setOperation(FileChangeOperation.UPDATE);

        // Edit line 2 and line 4
        TextEdit edit1 = new TextEdit(2, 2, "REPLACED 2");
        TextEdit edit2 = new TextEdit(4, 4, "REPLACED 4");
        proposal.setEdits(List.of(edit1, edit2));

        String result = patchApplier.computeProposedContent(original, proposal);
        String[] lines = result.split("\n");
        assertEquals("line 1", lines[0]);
        assertEquals("REPLACED 2", lines[1]);
        assertEquals("line 3", lines[2]);
        assertEquals("REPLACED 4", lines[3]);
    }
}
