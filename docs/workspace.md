# DevPilot AI — Workspace & Virtual Filesystem Architecture (Phase 6)

## 1. Overview & Objective

Phase 6 implements the complete virtual filesystem, hierarchical tree rendering, and developer workspace backend for **DevPilot AI**. It enables developers to scaffold, edit, navigate, and manage project code structures securely inside the browser, backed by persistent PostgreSQL storage.

```
┌─────────────────────────────────────────────────────────────────┐
│                 DevPilot Web Workspace UI                       │
│    (File Explorer 📁  |  Editor Tabs 📑  |  Live Canvas 💻)     │
└────────────────────────────────┬────────────────────────────────┘
                                 │ REST (Bearer JWT)
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Spring Boot 3 API                           │
│   ├── WorkspaceController     (/api/v1/projects/{id}/workspace) │
│   └── ProjectFileController   (/api/v1/projects/{id}/files)     │
└────────────────────────────────┬────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Service & Security                         │
│   ├── ProjectFileService (Path Validation, Tree Builder, CRUD)  │
│   ├── WorkspaceService   (Workspace state & recent files)       │
│   └── ActivityLogService (Audit trailing for file actions)      │
└────────────────────────────────┬────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                     PostgreSQL Database                         │
│   ├── project_files (Hierarchical recursive parent_id)          │
│   ├── projects      (Project boundaries & ownership)            │
│   └── activity_logs (Audit records)                             │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Database Schema (`project_files`)

The virtual filesystem uses an adjacency list schema with a self-referencing foreign key:

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `UUID` | Primary Key, Default `gen_random_uuid()` | Unique identifier for file/folder |
| `project_id` | `UUID` | Foreign Key `projects(id) ON DELETE CASCADE` | Scoping project boundary |
| `parent_id` | `UUID` | Foreign Key `project_files(id) ON DELETE CASCADE` | Nullable parent directory ID |
| `name` | `VARCHAR(120)` | `NOT NULL` | Base filename or folder name |
| `path` | `VARCHAR(500)` | `NOT NULL` | Canonical project-relative path |
| `file_type` | `VARCHAR(50)` | Nullable | Inferred type (`html`, `javascript`, etc.) |
| `content` | `TEXT` | Nullable | UTF-8 encoded text source content |
| `is_directory` | `BOOLEAN` | `NOT NULL DEFAULT FALSE` | Flag distinguishing files vs folders |
| `created_at` | `TIMESTAMPTZ`| `NOT NULL DEFAULT CURRENT_TIMESTAMP` | Creation timestamp |
| `updated_at` | `TIMESTAMPTZ`| `NOT NULL DEFAULT CURRENT_TIMESTAMP` | Last modified timestamp |

### Indexes & Integrity
- Unique constraint `uq_project_file_path (project_id, path)` prevents conflicting duplicates within the same project.
- B-Tree indexes on `idx_project_files_project`, `idx_project_files_path`, and `idx_project_files_parent` guarantee sub-millisecond path lookups and folder listings.

---

## 3. Security & Validation Rules

1. **Path Traversal Defense**:
   - Filenames and folder names cannot contain path separators (`/`, `\`), traversal sequences (`..`), null bytes (`\0`), or Windows drive letters (`C:`).
   - Canonical virtual paths are computed exclusively on the server as `parent.getPath() + "/" + name`.
2. **File Size Limit**:
   - Enforces a maximum content size of `1,000,000` bytes (1 MB) per file to prevent denial-of-service and database bloat.
3. **Circular Hierarchy Prevention (Cycle Detection)**:
   - When moving a folder, the target destination cannot be the folder itself or any of its descendants (`targetParent.getPath().startsWith(folder.getPath() + "/")`).
4. **Project Ownership Isolation**:
   - Every file operation verifies project ownership via the authenticated `UserPrincipal`. Users cannot view or modify files belonging to another developer unless granted the `ADMIN` role.
5. **Cascading Path Updates**:
   - Renaming or moving a directory recursively updates the canonical path prefixes of all child and descendant nodes in a single database transaction.

---

## 4. REST API Reference

All workspace endpoints require a valid JWT Bearer token: `Authorization: Bearer <access_token>`.

### 1. Load Workspace
`GET /api/v1/projects/{projectId}/workspace`
- **Response**: Project metadata, full hierarchical tree, 10 most recent files, and total file count.

### 2. Get Hierarchical File Tree
`GET /api/v1/projects/{projectId}/files/tree`
- **Response**: Array of root `FileNodeResponse` objects with recursively nested `children`, sorted folders-first then alphabetically.

### 3. Create Folder
`POST /api/v1/projects/{projectId}/files/folders`
- **Request**:
```json
{
  "name": "components",
  "parentId": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
}
```

### 4. Create File
`POST /api/v1/projects/{projectId}/files`
- **Request**:
```json
{
  "name": "Button.jsx",
  "parentId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "content": "export function Button() { return <button>Click</button>; }"
}
```

### 5. Read File Content
`GET /api/v1/projects/{projectId}/files/{fileId}`
- **Response**: File metadata, line count, byte size, and full text content.

### 6. Update File Content
`PUT /api/v1/projects/{projectId}/files/{fileId}`
- **Request**:
```json
{
  "content": "// Updated code content"
}
```

### 7. Rename File or Folder
`PATCH /api/v1/projects/{projectId}/files/{fileId}`
- **Request**:
```json
{
  "name": "UpdatedButton.jsx"
}
```

### 8. Move File or Folder
`PATCH /api/v1/projects/{projectId}/files/{fileId}/move`
- **Request**:
```json
{
  "parentId": "7bc299e5-9032-48f6-9606-161da60296b7"
}
```

### 9. Delete File or Folder
`DELETE /api/v1/projects/{projectId}/files/{fileId}`
- Deletes file or directory and recursively cascades deletion to all descendants.

### 10. Search Files
`GET /api/v1/projects/{projectId}/files/search?query=button`
- Returns files matching query in name or path.

---

## 5. Tree Sorting Algorithm

The file explorer sorts nodes consistently:
```java
Comparator<FileNodeResponse> comparator = (a, b) -> {
    if (a.isDirectory() != b.isDirectory()) {
        return a.isDirectory() ? -1 : 1; // Folders first
    }
    return a.getName().compareToIgnoreCase(b.getName()); // Alphabetical
};
```
This mirrors professional IDE environments (e.g. VS Code, IntelliJ IDEA).

---

## 6. Frontend Integration

- **File Explorer**: Dynamically renders nested folder hierarchies, handles expand/collapse chevrons, and updates folder selection.
- **Code Canvas**: Textarea code editor with real-time line numbering, tab indentation support (2 spaces), and unsaved change detection (`dirty-indicator`).
- **Shortcuts**: `Ctrl+S` / `Cmd+S` triggers immediate backend synchronization via `PUT /files/{fileId}` with user-facing toasts.
- **Creation & Deletion**: Modals and prompts for creating new files/folders and deleting items with full recursive confirmation.

---

## 7. Viva & Examination Discussion Points

1. **Why use an adjacency list (`parent_id`) rather than flat paths?**
   - Enables fast tree construction, parent lookup, and relational database integrity constraints (`ON DELETE CASCADE`). Flat paths require expensive prefix string parsing for hierarchy queries.
2. **How do you prevent cyclic folder movement?**
   - The server validates that target parent `is_directory == true`, `targetParent != folder`, and `!targetParent.getPath().startsWith(folder.getPath() + "/")` before updating paths.
3. **How is security ensured against path traversal attacks?**
   - Absolute paths and directory navigation characters (`..`, `/`, `\`) are rejected at the DTO and service layer, ensuring virtual files remain completely sandboxed.
