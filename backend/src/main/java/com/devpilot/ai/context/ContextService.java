package com.devpilot.ai.context;

import com.devpilot.ai.config.AIProperties;
import com.devpilot.ai.context.ContextResponse.SelectedFileInfo;
import com.devpilot.ai.entity.Project;
import com.devpilot.ai.entity.ProjectFile;
import com.devpilot.ai.exception.ForbiddenException;
import com.devpilot.ai.exception.ResourceNotFoundException;
import com.devpilot.ai.repository.ProjectFileRepository;
import com.devpilot.ai.repository.ProjectRepository;
import com.devpilot.ai.security.UserPrincipal;
import com.devpilot.ai.service.ActivityLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class ContextService {

    private final ProjectRepository projectRepository;
    private final ProjectFileRepository projectFileRepository;
    private final ContextRanker contextRanker;
    private final ContextChunker contextChunker;
    private final AIProperties aiProperties;
    private final ActivityLogService activityLogService;

    public ContextService(ProjectRepository projectRepository,
                          ProjectFileRepository projectFileRepository,
                          ContextRanker contextRanker,
                          ContextChunker contextChunker,
                          AIProperties aiProperties,
                          ActivityLogService activityLogService) {
        this.projectRepository = projectRepository;
        this.projectFileRepository = projectFileRepository;
        this.contextRanker = contextRanker;
        this.contextChunker = contextChunker;
        this.aiProperties = aiProperties;
        this.activityLogService = activityLogService;
    }

    public ContextResponse previewContext(ContextRequest request, UserPrincipal userPrincipal) {
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with ID: " + request.getProjectId()));

        if (project.getOwnerId() != null && userPrincipal != null && !project.getOwnerId().equals(userPrincipal.getId())) {
            throw new ForbiddenException("Access denied: You do not own this project.");
        }

        List<ProjectFile> allFiles = projectFileRepository.findByProjectIdAndIsDirectoryFalse(project.getId());
        ProjectFile activeFile = null;
        if (request.getFileId() != null) {
            activeFile = projectFileRepository.findById(request.getFileId()).orElse(null);
        }

        int maxFiles = request.getMaxFiles() != null ? request.getMaxFiles() : aiProperties.getContext().getMaxFiles();
        int maxChars = request.getMaxChars() != null ? request.getMaxChars() : aiProperties.getContext().getMaxTotalChars();
        int maxChunkChars = aiProperties.getContext().getMaxChunkChars();

        ContextBudget budget = new ContextBudget(maxFiles, maxChars, maxChunkChars);
        List<ContextRanker.RankedFile> rankedFiles = contextRanker.rankFiles(allFiles, activeFile, request.getSelectedCode(), request.getQuery());

        List<SelectedFileInfo> selectedFilesInfo = new ArrayList<>();
        List<ContextChunk> finalChunks = new ArrayList<>();

        for (ContextRanker.RankedFile rf : rankedFiles) {
            if (!budget.canIncludeFile()) break;

            ProjectFile file = rf.getFile();
            boolean isTarget = (activeFile != null && file.getId().equals(activeFile.getId()));

            List<ContextChunk> fileChunks = contextChunker.chunkFile(
                    file,
                    rf.getScore(),
                    isTarget ? request.getSelectedCode() : null,
                    isTarget ? request.getStartLine() : null,
                    isTarget ? request.getEndLine() : null,
                    budget.getMaxChunkChars()
            );

            boolean fileIncluded = false;
            for (ContextChunk chunk : fileChunks) {
                if (budget.tryConsume(chunk.getContent().length())) {
                    finalChunks.add(chunk);
                    fileIncluded = true;
                }
            }

            if (fileIncluded) {
                budget.incrementFileCount();
                selectedFilesInfo.add(new SelectedFileInfo(file.getId(), file.getPath(), rf.getScore(), rf.getReason()));
            }
        }

        if (userPrincipal != null) {
            activityLogService.logActivity(userPrincipal.getId(), project.getId(), "AI_CONTEXT_PREVIEW",
                    String.format("Previewed context for query '%s' across %d files", request.getQuery() != null ? request.getQuery() : "", selectedFilesInfo.size()));
        }

        return new ContextResponse(
                project.getId(),
                allFiles.size(),
                selectedFilesInfo,
                finalChunks,
                budget.getUsedChars(),
                budget.getEstimatedTokens()
        );
    }

    public String buildContextForPrompt(UUID projectId, UUID fileId, String selectedCode, Integer startLine, Integer endLine, String query, UserPrincipal userPrincipal) {
        ContextRequest req = new ContextRequest(projectId, fileId, selectedCode, startLine, endLine, query);
        ContextResponse resp = previewContext(req, userPrincipal);

        StringBuilder sb = new StringBuilder();
        sb.append("### Project Context Summary (Files: ").append(resp.getSelectedFiles().size())
          .append(", Est. Tokens: ").append(resp.getEstimatedTokens()).append(")\n\n");

        for (SelectedFileInfo sf : resp.getSelectedFiles()) {
            sb.append("- File: `").append(sf.getPath()).append("` (Relevance: ").append(sf.getScore())
              .append(", Reason: ").append(sf.getReason()).append(")\n");
        }
        sb.append("\n");

        for (ContextChunk chunk : resp.getChunks()) {
            sb.append("#### Context: `").append(chunk.getPath()).append("` [Lines ")
              .append(chunk.getStartLine()).append("-").append(chunk.getEndLine()).append("] (").append(chunk.getChunkType()).append(")\n");
            sb.append("```\n").append(chunk.getContent()).append("\n```\n\n");
        }

        return sb.toString();
    }
}
