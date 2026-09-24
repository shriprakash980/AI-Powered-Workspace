package com.devpilot.ai.service;

import com.devpilot.ai.dto.file.*;
import com.devpilot.ai.entity.Project;
import com.devpilot.ai.entity.ProjectFile;
import com.devpilot.ai.entity.enums.ProjectStatus;
import com.devpilot.ai.entity.enums.ProjectTemplate;
import com.devpilot.ai.entity.enums.UserStatus;
import com.devpilot.ai.exception.BadRequestException;
import com.devpilot.ai.exception.ConflictException;
import com.devpilot.ai.exception.ForbiddenException;
import com.devpilot.ai.exception.ResourceNotFoundException;
import com.devpilot.ai.repository.ProjectFileRepository;
import com.devpilot.ai.repository.ProjectRepository;
import com.devpilot.ai.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectFileServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectFileRepository projectFileRepository;

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private ProjectFileService projectFileService;

    private UUID projectId;
    private UUID ownerId;
    private Project testProject;
    private UserPrincipal ownerPrincipal;
    private UserPrincipal otherPrincipal;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        ownerId = UUID.randomUUID();

        testProject = Project.builder()
                .id(projectId)
                .name("cloud-compiler-service")
                .description("Compiles Java code securely in Docker")
                .template(ProjectTemplate.JAVA_SPRING)
                .language("Java")
                .status(ProjectStatus.ACTIVE)
                .ownerId(ownerId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        ownerPrincipal = new UserPrincipal(
                ownerId,
                "Project Owner",
                "owner@devpilot.ai",
                "Password123!",
                UserStatus.ACTIVE,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        otherPrincipal = new UserPrincipal(
                UUID.randomUUID(),
                "Other User",
                "other@devpilot.ai",
                "Password123!",
                UserStatus.ACTIVE,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    @DisplayName("Should create root folder successfully")
    void shouldCreateRootFolder() {
        when(projectRepository.findByIdAndStatusNot(projectId, ProjectStatus.DELETED))
                .thenReturn(Optional.of(testProject));
        when(projectFileRepository.existsByProjectIdAndPath(projectId, "src"))
                .thenReturn(false);
        when(projectFileRepository.save(any(ProjectFile.class)))
                .thenAnswer(inv -> {
                    ProjectFile f = inv.getArgument(0);
                    f.setId(UUID.randomUUID());
                    return f;
                });

        CreateFolderRequest request = new CreateFolderRequest("src", null);
        ProjectFileResponse response = projectFileService.createFolder(projectId, request, ownerPrincipal);

        assertNotNull(response);
        assertEquals("src", response.getName());
        assertEquals("src", response.getPath());
        assertTrue(response.isDirectory());
        verify(activityLogService, times(1)).logActivity(eq(ownerId), eq(projectId), eq("FOLDER_CREATED"), anyString());
    }

    @Test
    @DisplayName("Should create nested file under parent directory")
    void shouldCreateNestedFile() {
        UUID parentFolderId = UUID.randomUUID();
        ProjectFile parentFolder = ProjectFile.builder()
                .id(parentFolderId)
                .projectId(projectId)
                .name("src")
                .path("src")
                .isDirectory(true)
                .build();

        when(projectRepository.findByIdAndStatusNot(projectId, ProjectStatus.DELETED))
                .thenReturn(Optional.of(testProject));
        when(projectFileRepository.findByProjectIdAndId(projectId, parentFolderId))
                .thenReturn(Optional.of(parentFolder));
        when(projectFileRepository.existsByProjectIdAndPath(projectId, "src/Main.java"))
                .thenReturn(false);
        when(projectFileRepository.save(any(ProjectFile.class)))
                .thenAnswer(inv -> {
                    ProjectFile f = inv.getArgument(0);
                    f.setId(UUID.randomUUID());
                    return f;
                });

        CreateFileRequest request = new CreateFileRequest("Main.java", parentFolderId, "public class Main {}");
        ProjectFileResponse response = projectFileService.createFile(projectId, request, ownerPrincipal);

        assertNotNull(response);
        assertEquals("Main.java", response.getName());
        assertEquals("src/Main.java", response.getPath());
        assertEquals("java", response.getFileType());
        assertFalse(response.isDirectory());
        assertEquals("public class Main {}", response.getContent());
    }

    @Test
    @DisplayName("Should reject file creation with path traversal attack")
    void shouldRejectPathTraversalInFileName() {
        when(projectRepository.findByIdAndStatusNot(projectId, ProjectStatus.DELETED))
                .thenReturn(Optional.of(testProject));

        CreateFileRequest request = new CreateFileRequest("../../etc/passwd", null, "malicious");

        assertThrows(BadRequestException.class, () ->
                projectFileService.createFile(projectId, request, ownerPrincipal));
        verify(projectFileRepository, never()).save(any(ProjectFile.class));
    }

    @Test
    @DisplayName("Should reject file exceeding 1MB limit")
    void shouldRejectFileExceedingSizeLimit() {
        when(projectRepository.findByIdAndStatusNot(projectId, ProjectStatus.DELETED))
                .thenReturn(Optional.of(testProject));

        String hugeContent = "A".repeat(ProjectFileService.MAX_FILE_SIZE_BYTES + 50);
        CreateFileRequest request = new CreateFileRequest("huge.txt", null, hugeContent);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                projectFileService.createFile(projectId, request, ownerPrincipal));
        assertTrue(ex.getMessage().contains("exceeds maximum allowed size"));
    }

    @Test
    @DisplayName("Should throw ConflictException on duplicate file path")
    void shouldThrowConflictOnDuplicatePath() {
        when(projectRepository.findByIdAndStatusNot(projectId, ProjectStatus.DELETED))
                .thenReturn(Optional.of(testProject));
        when(projectFileRepository.existsByProjectIdAndPath(projectId, "app.js"))
                .thenReturn(true);

        CreateFileRequest request = new CreateFileRequest("app.js", null, "console.log();");

        assertThrows(ConflictException.class, () ->
                projectFileService.createFile(projectId, request, ownerPrincipal));
    }

    @Test
    @DisplayName("Should update file content successfully")
    void shouldUpdateFileContent() {
        UUID fileId = UUID.randomUUID();
        ProjectFile file = ProjectFile.builder()
                .id(fileId)
                .projectId(projectId)
                .name("app.js")
                .path("app.js")
                .isDirectory(false)
                .content("console.log('old');")
                .build();

        when(projectRepository.findByIdAndStatusNot(projectId, ProjectStatus.DELETED))
                .thenReturn(Optional.of(testProject));
        when(projectFileRepository.findByProjectIdAndId(projectId, fileId))
                .thenReturn(Optional.of(file));
        when(projectFileRepository.save(any(ProjectFile.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateFileContentRequest request = new UpdateFileContentRequest("console.log('new');");
        ProjectFileResponse response = projectFileService.updateFileContent(projectId, fileId, request, ownerPrincipal);

        assertEquals("console.log('new');", response.getContent());
        verify(activityLogService, times(1)).logActivity(eq(ownerId), eq(projectId), eq("FILE_UPDATED"), anyString());
    }

    @Test
    @DisplayName("Should rename folder and cascade path updates to all descendants")
    void shouldRenameFolderAndCascadeDescendantPaths() {
        UUID folderId = UUID.randomUUID();
        ProjectFile folder = ProjectFile.builder()
                .id(folderId)
                .projectId(projectId)
                .name("src")
                .path("src")
                .isDirectory(true)
                .build();

        ProjectFile child1 = ProjectFile.builder()
                .id(UUID.randomUUID())
                .projectId(projectId)
                .name("Main.java")
                .path("src/Main.java")
                .parentId(folderId)
                .isDirectory(false)
                .build();

        ProjectFile child2 = ProjectFile.builder()
                .id(UUID.randomUUID())
                .projectId(projectId)
                .name("utils")
                .path("src/utils")
                .parentId(folderId)
                .isDirectory(true)
                .build();

        when(projectRepository.findByIdAndStatusNot(projectId, ProjectStatus.DELETED))
                .thenReturn(Optional.of(testProject));
        when(projectFileRepository.findByProjectIdAndId(projectId, folderId))
                .thenReturn(Optional.of(folder));
        when(projectFileRepository.existsByProjectIdAndPath(projectId, "source"))
                .thenReturn(false);
        when(projectFileRepository.findByProjectIdAndPathStartingWith(projectId, "src/"))
                .thenReturn(List.of(child1, child2));
        when(projectFileRepository.save(any(ProjectFile.class))).thenAnswer(inv -> inv.getArgument(0));

        RenameFileRequest request = new RenameFileRequest("source");
        ProjectFileResponse response = projectFileService.renameFile(projectId, folderId, request, ownerPrincipal);

        assertEquals("source", response.getName());
        assertEquals("source", response.getPath());
        assertEquals("source/Main.java", child1.getPath());
        assertEquals("source/utils", child2.getPath());
        verify(projectFileRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("Should reject moving folder into itself")
    void shouldRejectMovingFolderIntoItself() {
        UUID folderId = UUID.randomUUID();
        ProjectFile folder = ProjectFile.builder()
                .id(folderId)
                .projectId(projectId)
                .name("src")
                .path("src")
                .isDirectory(true)
                .build();

        when(projectRepository.findByIdAndStatusNot(projectId, ProjectStatus.DELETED))
                .thenReturn(Optional.of(testProject));
        when(projectFileRepository.findByProjectIdAndId(projectId, folderId))
                .thenReturn(Optional.of(folder));

        MoveFileRequest request = new MoveFileRequest(folderId);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                projectFileService.moveFile(projectId, folderId, request, ownerPrincipal));
        assertTrue(ex.getMessage().contains("Cannot move a folder into itself"));
    }

    @Test
    @DisplayName("Should reject moving folder into its own subdirectory")
    void shouldRejectMovingFolderIntoSubdirectory() {
        UUID folderId = UUID.randomUUID();
        ProjectFile folder = ProjectFile.builder()
                .id(folderId)
                .projectId(projectId)
                .name("src")
                .path("src")
                .isDirectory(true)
                .build();

        UUID subFolderId = UUID.randomUUID();
        ProjectFile subFolder = ProjectFile.builder()
                .id(subFolderId)
                .projectId(projectId)
                .name("components")
                .path("src/components")
                .parentId(folderId)
                .isDirectory(true)
                .build();

        when(projectRepository.findByIdAndStatusNot(projectId, ProjectStatus.DELETED))
                .thenReturn(Optional.of(testProject));
        when(projectFileRepository.findByProjectIdAndId(projectId, folderId))
                .thenReturn(Optional.of(folder));
        when(projectFileRepository.findByProjectIdAndId(projectId, subFolderId))
                .thenReturn(Optional.of(subFolder));

        MoveFileRequest request = new MoveFileRequest(subFolderId);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                projectFileService.moveFile(projectId, folderId, request, ownerPrincipal));
        assertTrue(ex.getMessage().contains("Cannot move a folder into one of its subdirectories"));
    }

    @Test
    @DisplayName("Should delete folder and all its descendants")
    void shouldDeleteFolderAndDescendants() {
        UUID folderId = UUID.randomUUID();
        ProjectFile folder = ProjectFile.builder()
                .id(folderId)
                .projectId(projectId)
                .name("src")
                .path("src")
                .isDirectory(true)
                .build();

        ProjectFile child = ProjectFile.builder()
                .id(UUID.randomUUID())
                .projectId(projectId)
                .name("app.js")
                .path("src/app.js")
                .build();

        when(projectRepository.findByIdAndStatusNot(projectId, ProjectStatus.DELETED))
                .thenReturn(Optional.of(testProject));
        when(projectFileRepository.findByProjectIdAndId(projectId, folderId))
                .thenReturn(Optional.of(folder));
        when(projectFileRepository.findByProjectIdAndPathStartingWith(projectId, "src/"))
                .thenReturn(List.of(child));

        projectFileService.deleteFile(projectId, folderId, ownerPrincipal);

        verify(projectFileRepository, times(1)).deleteAll(List.of(child));
        verify(projectFileRepository, times(1)).delete(folder);
        verify(activityLogService, times(1)).logActivity(eq(ownerId), eq(projectId), eq("FOLDER_DELETED"), anyString());
    }

    @Test
    @DisplayName("Should build hierarchical tree sorted with directories first and alphabetical")
    void shouldBuildHierarchicalTreeSorted() {
        UUID folder1Id = UUID.randomUUID();
        ProjectFile folder1 = ProjectFile.builder().id(folder1Id).name("src").path("src").isDirectory(true).build();

        UUID folder2Id = UUID.randomUUID();
        ProjectFile folder2 = ProjectFile.builder().id(folder2Id).name("docs").path("docs").isDirectory(true).build();

        ProjectFile file1 = ProjectFile.builder().id(UUID.randomUUID()).name("README.md").path("README.md").isDirectory(false).build();
        ProjectFile file2 = ProjectFile.builder().id(UUID.randomUUID()).name("package.json").path("package.json").isDirectory(false).build();

        ProjectFile child1 = ProjectFile.builder().id(UUID.randomUUID()).name("index.js").path("src/index.js").parentId(folder1Id).isDirectory(false).build();
        ProjectFile child2 = ProjectFile.builder().id(UUID.randomUUID()).name("components").path("src/components").parentId(folder1Id).isDirectory(true).build();

        List<ProjectFile> allFiles = List.of(file1, folder1, file2, folder2, child1, child2);

        List<FileNodeResponse> tree = projectFileService.buildFileTree(allFiles);

        // Root level should have 4 nodes: docs, src (folders), package.json, README.md (files)
        assertEquals(4, tree.size());
        assertEquals("docs", tree.get(0).getName());
        assertTrue(tree.get(0).isDirectory());
        assertEquals("src", tree.get(1).getName());
        assertTrue(tree.get(1).isDirectory());
        assertEquals("package.json", tree.get(2).getName());
        assertFalse(tree.get(2).isDirectory());
        assertEquals("README.md", tree.get(3).getName());
        assertFalse(tree.get(3).isDirectory());

        // Check src children: components (folder), index.js (file)
        FileNodeResponse srcNode = tree.get(1);
        assertEquals(2, srcNode.getChildren().size());
        assertEquals("components", srcNode.getChildren().get(0).getName());
        assertTrue(srcNode.getChildren().get(0).isDirectory());
        assertEquals("index.js", srcNode.getChildren().get(1).getName());
        assertFalse(srcNode.getChildren().get(1).isDirectory());
    }

    @Test
    @DisplayName("Should throw ForbiddenException when user is not owner or admin")
    void shouldThrowForbiddenWhenNotOwner() {
        when(projectRepository.findByIdAndStatusNot(projectId, ProjectStatus.DELETED))
                .thenReturn(Optional.of(testProject));

        CreateFileRequest request = new CreateFileRequest("file.txt", null, "content");

        assertThrows(ForbiddenException.class, () ->
                projectFileService.createFile(projectId, request, otherPrincipal));
    }
}
