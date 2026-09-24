# DevPilot AI — Local Git System Documentation

## 1. Overview
DevPilot AI provides a local Git engine powered by **Eclipse JGit 6.9.0**. Every DevPilot project is mapped to an isolated Git working directory inside `DEV_PILOT_WORKSPACE_ROOT/{projectId}`.

---

## 2. Capabilities
The Git subsystem supports:
- **Repository Initialization**: `POST /api/v1/projects/{projectId}/git/init` initializes a new Git repository if not already present.
- **Status Inspection**: `GET /api/v1/projects/{projectId}/git/status` retrieves working tree state, staged files, untracked files, branch name, and ahead/behind commit counts.
- **Staging & Unstaging**:
  - `POST /api/v1/projects/{projectId}/git/stage` - Stage specific file paths.
  - `POST /api/v1/projects/{projectId}/git/unstage` - Unstage specific file paths.
  - `POST /api/v1/projects/{projectId}/git/stage-all` - Stage all modified and untracked files.
  - `POST /api/v1/projects/{projectId}/git/unstage-all` - Unstage all staged files.
- **Committing**: `POST /api/v1/projects/{projectId}/git/commit` creates a Git commit from staged changes.
- **AI Commit Message Generation**: `POST /api/v1/projects/{projectId}/git/ai-commit-message` inspects the staged diff and generates a conventional commit message via AI.
- **Branch Management**:
  - `GET /api/v1/projects/{projectId}/git/branches` - List local and remote branches.
  - `POST /api/v1/projects/{projectId}/git/branches` - Create a new branch.
  - `POST /api/v1/projects/{projectId}/git/branches/checkout` - Switch to a branch. Returns `409 Conflict` if uncommitted changes exist.
  - `DELETE /api/v1/projects/{projectId}/git/branches/{branchName}` - Delete a branch (protected against deleting current branch).
- **Remotes & Synchronization**:
  - `GET /api/v1/projects/{projectId}/git/remotes` - List configured remotes (credentials masked).
  - `POST /api/v1/projects/{projectId}/git/remotes` - Add a remote URL (`origin`).
  - `POST /api/v1/projects/{projectId}/git/fetch` - Fetch refs from remote.
  - `POST /api/v1/projects/{projectId}/git/pull` - Pull and merge remote changes. Detects merge conflicts.
  - `POST /api/v1/projects/{projectId}/git/push` - Push local branch commits to remote repository.

---

## 3. Architecture & DB File Sync
- **Disk-DB Synchronization**: `GitWorkspaceManager` synchronizes files between PostgreSQL database storage (`ProjectFile`) and local filesystem working copy before Git operations.
- **Diff Generation**: Diff engine uses JGit `DiffFormatter` to stream unified diffs for working copy vs HEAD or staged changes vs HEAD.
