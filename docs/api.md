# DevPilot AI — REST API Specification

## 1. Global API Standards
- **Base URI:** `/api/v1`
- **Format:** JSON (`application/json; charset=UTF-8`)
- **Authentication:** Bearer JWT in `Authorization: Bearer <access_token>`
- **Response Format:**
```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": {},
  "timestamp": "2026-09-24T12:00:00Z"
}
```

---

## 2. API Endpoints Catalog

### 2.1 Authentication & Security
- `POST /api/v1/auth/register` — Create new user account.
- `POST /api/v1/auth/login` — Authenticate credentials and issue JWT + Refresh Token.
- `POST /api/v1/auth/refresh` — Issue fresh access token from valid refresh token.
- `POST /api/v1/auth/logout` — Revoke active refresh token.
- `GET  /api/v1/auth/me` — Retrieve current authenticated user profile.

### 2.2 Projects Management
- `GET    /api/v1/projects` — List user's projects with filtering and pagination.
- `POST   /api/v1/projects` — Initialize a new project with starter template.
- `GET    /api/v1/projects/{id}` — Get project metadata by ID.
- `PUT    /api/v1/projects/{id}` — Update project name or description.
- `DELETE /api/v1/projects/{id}` — Soft delete / archive project.

### 2.3 Files & Folders
- `GET    /api/v1/projects/{id}/tree` — Retrieve full hierarchical directory tree.
- `POST   /api/v1/projects/{id}/files` — Create new file in project.
- `GET    /api/v1/files/{fileId}` — Fetch raw file content and metadata.
- `PUT    /api/v1/files/{fileId}` — Save file content.
- `DELETE /api/v1/files/{fileId}` — Remove file from project.
- `POST   /api/v1/projects/{id}/folders` — Create new folder in project.
- `DELETE /api/v1/folders/{folderId}` — Delete folder and nested items.

### 2.4 AI Assistant & Context
- `POST /api/v1/ai/chat` — Send conversational query with contextual code metadata.
- `POST /api/v1/ai/generate` — Generate boilerplate or target function.
- `POST /api/v1/ai/explain` — Return natural language breakdown of highlighted code.
- `POST /api/v1/ai/debug` — Detect logical/syntax errors and propose diff fixes.
- `POST /api/v1/ai/refactor` — Suggest clean-code improvements and diffs.
- `POST /api/v1/ai/tests` — Auto-generate unit test suite for selected code.

### 2.5 Sandboxed Execution
- `POST /api/v1/execution/run` — Execute code or controlled script in isolated Docker container.
- `GET  /api/v1/execution/status/{jobId}` — Poll execution status and streaming logs.
- `POST /api/v1/execution/stop/{jobId}` — Abort active execution container.

### 2.6 Deployments & GitHub
- `POST /api/v1/deployments` — Trigger build & deploy sequence for project.
- `GET  /api/v1/deployments/project/{id}` — List deployment history with logs and URLs.
- `GET  /api/v1/github/repos` — List authenticated GitHub repositories.
- `POST /api/v1/github/import` — Clone repository into DevPilot project.
- `POST /api/v1/github/commit` — Commit and push workspace changes.

### 2.7 Administration (ROLE_ADMIN)
- `GET /api/v1/admin/users` — List platform users and statuses.
- `PUT /api/v1/admin/users/{id}/status` — Enable or suspend user account.
- `GET /api/v1/admin/metrics` — Aggregate system telemetry, AI token usage, and container health.
