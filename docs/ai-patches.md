# DevPilot AI — Multi-File Patch Engine & Human-in-the-Loop Review (Phase 9)

## 1. Overview
DevPilot AI rejects unsafe, silent code alterations. AI-suggested code changes are represented as structured **ChangeSets** containing individual **FileChanges**.

Developers review unified diffs side-by-side inside the Monaco DiffEditor, select specific changes to apply, and commit them with transactional atomicity and optimistic concurrency protection.

---

## 2. Key Architecture Principles

1. **Human-in-the-Loop Safety**: No AI modification is written to disk or the database without explicit user inspection and approval.
2. **Deterministic Optimistic Concurrency**: Every `FileChange` stores the SHA-256 hash of the file content at proposal time (`old_content_hash`). If the file is modified concurrently before the patch is applied, the transaction aborts with `409 Conflict`.
3. **Atomic Execution**: Multi-file patches execute inside a single `@Transactional` boundary. If any step fails, all operations roll back completely.
4. **Non-Destructive Rollback**: Applied changesets can be rolled back at any time, restoring pre-change states and capturing explicit `ROLLBACK` audit snapshots.

---

## 3. Supported Operations

| Operation | Description | Concurrency Guard |
| :--- | :--- | :--- |
| `CREATE` | Creates a new file along with any necessary parent directories. | Rejects if destination path already exists. |
| `UPDATE` | Updates lines or full contents of an existing file. | Requires `sha256(current) == old_content_hash`. |
| `DELETE` | Deletes an existing file while retaining snapshot history. | Requires `sha256(current) == old_content_hash`. |
| `RENAME` | Moves or renames an existing file node. | Requires `sha256(current) == old_content_hash`. |

---

## 4. ChangeSet Lifecycle

```
[ AI Generates Proposal ]
           │
           ▼
     ┌───────────┐
     │ PROPOSED  │
     └─────┬─────┘
           │
    ┌──────┴──────────────┐
    ▼                     ▼
┌─────────┐         ┌───────────┐
│ REJECTED│         │  APPLIED  │ (or PARTIALLY_APPLIED)
└─────────┘         └─────┬─────┘
                          │
                          ▼
                    ┌───────────┐
                    │ROLLED_BACK│
                    └───────────┘
```

---

## 5. Security & Path Traversal Guards

`PatchValidator` enforces strict defense-in-depth:
- Path traversal sequences (`..`, leading `/` or `\`, drive letters `C:`) are rejected.
- Null byte poisoning (`\0`) is blocked.
- Protected configuration files (`.env`, `.git`, `.gitignore`, `.ssh`, `id_rsa`, `keystore.jks`) cannot be targeted by AI proposals.
- Limits enforced:
  - Max files per changeset: 10
  - Max file size: 500 KB
  - Max total changeset payload: 2 MB
  - Max line edits per file: 100

---

## 6. REST API Reference

### Propose a ChangeSet
`POST /api/v1/projects/{projectId}/changesets`

### List Project ChangeSets
`GET /api/v1/projects/{projectId}/changesets`

### Get ChangeSet with Diffs
`GET /api/v1/projects/{projectId}/changesets/{changeSetId}`

### Apply ChangeSet (Full or Partial)
`POST /api/v1/projects/{projectId}/changesets/{changeSetId}/apply`
```json
{
  "fileChangeIds": ["uuid-1", "uuid-2"]
}
```

### Reject ChangeSet
`POST /api/v1/projects/{projectId}/changesets/{changeSetId}/reject`

### Rollback ChangeSet
`POST /api/v1/projects/{projectId}/changesets/{changeSetId}/rollback`
