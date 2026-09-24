package com.devpilot.ai.patch;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class PatchApplier {

    /**
     * Applies line edits or returns direct proposed content for a file change.
     */
    public String computeProposedContent(String originalContent, FileChangeProposal proposal) {
        if (proposal.getProposedContent() != null && !proposal.getProposedContent().isBlank()) {
            return proposal.getProposedContent();
        }

        if (proposal.getEdits() == null || proposal.getEdits().isEmpty()) {
            return originalContent != null ? originalContent : "";
        }

        String orig = originalContent != null ? originalContent : "";
        List<String> lines = new ArrayList<>(Arrays.asList(orig.split("\n", -1)));

        // Sort edits in descending order of startLine so line index shifts don't affect previous lines
        List<TextEdit> sortedEdits = new ArrayList<>(proposal.getEdits());
        sortedEdits.sort((a, b) -> Integer.compare(b.getStartLine(), a.getStartLine()));

        for (TextEdit edit : sortedEdits) {
            int s = Math.max(1, edit.getStartLine()) - 1;
            int e = Math.min(lines.size(), Math.max(s + 1, edit.getEndLine()));

            String[] replacementLines = edit.getReplacement().split("\n", -1);
            List<String> newSegment = Arrays.asList(replacementLines);

            // Remove old lines in range [s, e)
            for (int i = 0; i < (e - s); i++) {
                if (s < lines.size()) {
                    lines.remove(s);
                }
            }

            // Insert new lines at index s
            lines.addAll(s, newSegment);
        }

        return String.join("\n", lines);
    }
}
