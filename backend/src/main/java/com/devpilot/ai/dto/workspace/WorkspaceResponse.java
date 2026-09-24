package com.devpilot.ai.dto.workspace;

import com.devpilot.ai.dto.file.FileNodeResponse;
import com.devpilot.ai.dto.file.ProjectFileResponse;
import com.devpilot.ai.dto.project.ProjectResponse;

import java.util.List;

public class WorkspaceResponse {

    private ProjectResponse project;
    private List<FileNodeResponse> tree;
    private List<ProjectFileResponse> recentFiles;
    private long totalFiles;

    public WorkspaceResponse() {}

    public WorkspaceResponse(ProjectResponse project, List<FileNodeResponse> tree,
                             List<ProjectFileResponse> recentFiles, long totalFiles) {
        this.project = project;
        this.tree = tree;
        this.recentFiles = recentFiles;
        this.totalFiles = totalFiles;
    }

    public static Builder builder() {
        return new Builder();
    }

    public ProjectResponse getProject() { return project; }
    public void setProject(ProjectResponse project) { this.project = project; }

    public List<FileNodeResponse> getTree() { return tree; }
    public void setTree(List<FileNodeResponse> tree) { this.tree = tree; }

    public List<ProjectFileResponse> getRecentFiles() { return recentFiles; }
    public void setRecentFiles(List<ProjectFileResponse> recentFiles) { this.recentFiles = recentFiles; }

    public long getTotalFiles() { return totalFiles; }
    public void setTotalFiles(long totalFiles) { this.totalFiles = totalFiles; }

    public static class Builder {
        private ProjectResponse project;
        private List<FileNodeResponse> tree;
        private List<ProjectFileResponse> recentFiles;
        private long totalFiles;

        public Builder project(ProjectResponse project) { this.project = project; return this; }
        public Builder tree(List<FileNodeResponse> tree) { this.tree = tree; return this; }
        public Builder recentFiles(List<ProjectFileResponse> recentFiles) { this.recentFiles = recentFiles; return this; }
        public Builder totalFiles(long totalFiles) { this.totalFiles = totalFiles; return this; }

        public WorkspaceResponse build() {
            return new WorkspaceResponse(project, tree, recentFiles, totalFiles);
        }
    }
}
