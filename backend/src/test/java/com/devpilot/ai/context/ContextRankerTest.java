package com.devpilot.ai.context;

import com.devpilot.ai.entity.ProjectFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ContextRankerTest {

    private ContextRanker contextRanker;
    private DependencyAnalyzer dependencyAnalyzer;

    @BeforeEach
    void setUp() {
        dependencyAnalyzer = new DependencyAnalyzer();
        contextRanker = new ContextRanker(dependencyAnalyzer);
    }

    @Test
    @DisplayName("Active file should receive highest base score of at least 100")
    void testActiveFileHighestRank() {
        UUID projectId = UUID.randomUUID();
        UUID activeFileId = UUID.randomUUID();

        ProjectFile activeFile = ProjectFile.builder()
                .id(activeFileId)
                .projectId(projectId)
                .path("src/main/App.java")
                .name("App.java")
                .content("public class App { }")
                .isDirectory(false)
                .build();

        ProjectFile otherFile = ProjectFile.builder()
                .id(UUID.randomUUID())
                .projectId(projectId)
                .path("docs/README.md")
                .name("README.md")
                .content("# Readme")
                .isDirectory(false)
                .build();

        List<ContextRanker.RankedFile> ranked = contextRanker.rankFiles(
                List.of(otherFile, activeFile),
                activeFile,
                "find App class",
                "App"
        );

        assertFalse(ranked.isEmpty());
        assertEquals(activeFileId, ranked.get(0).getFile().getId());
        assertTrue(ranked.get(0).getScore() >= 100);
    }

    @Test
    @DisplayName("Directly imported dependency should rank higher than unrelated file")
    void testDirectDependencyRanking() {
        UUID projectId = UUID.randomUUID();
        ProjectFile activeFile = ProjectFile.builder()
                .id(UUID.randomUUID())
                .projectId(projectId)
                .path("src/controllers/UserController.js")
                .name("UserController.js")
                .content("import UserService from '../services/UserService.js';\nexport class UserController {}")
                .isDirectory(false)
                .build();

        ProjectFile importedService = ProjectFile.builder()
                .id(UUID.randomUUID())
                .projectId(projectId)
                .path("src/services/UserService.js")
                .name("UserService.js")
                .content("export default class UserService {}")
                .isDirectory(false)
                .build();

        ProjectFile unrelatedFile = ProjectFile.builder()
                .id(UUID.randomUUID())
                .projectId(projectId)
                .path("legacy/OldHelper.txt")
                .name("OldHelper.txt")
                .content("random text")
                .isDirectory(false)
                .build();

        List<ContextRanker.RankedFile> ranked = contextRanker.rankFiles(
                List.of(unrelatedFile, importedService, activeFile),
                activeFile,
                "auth login",
                null
        );

        int importedIndex = -1;
        int unrelatedIndex = -1;
        for (int i = 0; i < ranked.size(); i++) {
            if (ranked.get(i).getFile().getId().equals(importedService.getId())) importedIndex = i;
            if (ranked.get(i).getFile().getId().equals(unrelatedFile.getId())) unrelatedIndex = i;
        }

        assertTrue(importedIndex != -1, "Imported file should be included in ranked files");
        assertEquals(-1, unrelatedIndex, "Unrelated file with 0 score should not be included in ranked files");
    }
}
