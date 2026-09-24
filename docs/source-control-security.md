# DevPilot AI — Source Control Security & Isolation Model

## 1. Directory Traversal & Path Validation
All Git file operations are enforced by `GitWorkspaceManager.validatePathInsideWorkspace()`.
- Validates that normalized target paths fall strictly under `DEV_PILOT_WORKSPACE_ROOT/{projectId}`.
- Protects against directory traversal attacks (`../`, symlink attacks, path escapes).

---

## 2. Execution Containment & Git Hooks Security
Untrusted repositories cloned or imported from external sources could contain malicious Git hooks (`.git/hooks/pre-commit`, `.git/hooks/post-checkout`, etc.).
- **Disabled Git Hooks**: `GitWorkspaceManager.secureGitConfig()` explicitly configures JGit repositories with `core.hooksPath = /dev/null`.
- Prevents execution of arbitrary shell commands or malicious scripts upon checkout, commit, or pull operations.

---

## 3. Storage & Isolation Limits
- **Workspace Isolation**: Projects are stored in isolated directory structures. Project A cannot access or query Project B's repository directory.
- **Size Limits**: Repositories are bounded by configuration settings (`app.git.max-repo-size-mb=100`, `app.git.max-files-per-commit=500`) to prevent denial-of-service (DoS) attempts via disk exhaustion or payload explosion.
