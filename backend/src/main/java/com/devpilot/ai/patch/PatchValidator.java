package com.devpilot.ai.patch;

import com.devpilot.ai.config.AIProperties;
import com.devpilot.ai.entity.enums.FileChangeOperation;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class PatchValidator {

    private final AIProperties aiProperties;

    private static final Set<String> BLOCKED_FILES = Set.of(
            ".env", ".git", ".gitignore", ".ssh", "id_rsa", "credentials", "secrets.json", "keystore.jks"
    );

    public PatchValidator(AIProperties aiProperties) {
        this.aiProperties = aiProperties;
    }

    public PatchValidationResult validate(ChangeSetProposal proposal) {
        PatchValidationResult result = PatchValidationResult.ok();

        if (proposal == null) {
            return PatchValidationResult.error("ChangeSet proposal cannot be null");
        }

        if (proposal.getChanges() == null || proposal.getChanges().isEmpty()) {
            return PatchValidationResult.error("ChangeSet proposal contains no file changes");
        }

        AIProperties.PatchConfig config = aiProperties.getPatch();
        if (proposal.getChanges().size() > config.getMaxFiles()) {
            result.addError(String.format("ChangeSet exceeds maximum allowed files limit (%d > %d)",
                    proposal.getChanges().size(), config.getMaxFiles()));
        }

        long totalSize = 0;

        for (FileChangeProposal change : proposal.getChanges()) {
            if (change.getOperation() == null) {
                result.addError("Operation must not be null for proposed file change");
                continue;
            }

            String path = change.getFilePath();
            validatePath(path, result);

            if (change.getOperation() == FileChangeOperation.RENAME) {
                validatePath(change.getOldPath(), result);
                validatePath(change.getNewPath(), result);
            }

            if (change.getProposedContent() != null) {
                int size = change.getProposedContent().length();
                totalSize += size;
                if (size > config.getMaxFileSize()) {
                    result.addError(String.format("File '%s' proposed content exceeds size limit (%d > %d bytes)",
                            path, size, config.getMaxFileSize()));
                }
            }

            if (change.getEdits() != null && change.getEdits().size() > config.getMaxEditsPerFile()) {
                result.addError(String.format("File '%s' exceeds maximum edits per file limit (%d > %d)",
                        path, change.getEdits().size(), config.getMaxEditsPerFile()));
            }

            if (change.getEdits() != null) {
                for (TextEdit edit : change.getEdits()) {
                    if (edit.getStartLine() < 1 || edit.getEndLine() < edit.getStartLine()) {
                        result.addError(String.format("Invalid edit line range [%d, %d] in file '%s'",
                                edit.getStartLine(), edit.getEndLine(), path));
                    }
                }
            }
        }

        if (totalSize > config.getMaxTotalSize()) {
            result.addError(String.format("ChangeSet exceeds maximum total payload size (%d > %d bytes)",
                    totalSize, config.getMaxTotalSize()));
        }

        return result;
    }

    public void validatePath(String path, PatchValidationResult result) {
        if (path == null || path.isBlank()) {
            result.addError("File path cannot be empty");
            return;
        }

        if (path.contains("\0")) {
            result.addError("Unsafe path contains null bytes: " + path);
        }

        if (path.contains("..") || path.startsWith("/") || path.startsWith("\\") || path.matches("^[a-zA-Z]:.*")) {
            result.addError("Invalid path traversal sequence detected: " + path);
        }

        String fileName = path;
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash != -1) {
            fileName = path.substring(lastSlash + 1);
        }

        if (BLOCKED_FILES.contains(fileName.toLowerCase()) || fileName.startsWith(".env")) {
            result.addError("Modifications to protected or sensitive configuration file prohibited: " + fileName);
        }
    }
}
