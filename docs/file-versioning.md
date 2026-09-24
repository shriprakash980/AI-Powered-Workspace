# DevPilot AI — File Versioning & Snapshot System (Phase 9)

## 1. Overview
DevPilot AI features an immutable file versioning system that records historical snapshots of virtual files across manual saves, AI patch operations, and user-initiated rollbacks.

File versions are stored in the PostgreSQL table `file_versions` and allow full point-in-time diffing and non-destructive rollbacks.

---

## 2. Invariant Properties

1. **Immutability**: File version records are insert-only. Once written, a version snapshot is never modified or deleted during standard operations.
2. **Sequential Version Numbers**: Each file maintains a contiguous sequence of version numbers ($v_1, v_2, v_3, \dots$) managed via `UNIQUE (file_id, version_number)`.
3. **Non-Destructive Restore**: Restoring an older version (e.g. restoring $v_2$ when current is $v_5$) does not overwrite or delete historical records. Instead, it creates a new snapshot ($v_6$) with `source = 'ROLLBACK'`.
4. **Content Verification**: Every version snapshot stores a SHA-256 cryptographic digest of the file content (`content_hash`) for tamper detection and concurrency checking.

---

## 3. Version Provenance (Source Enum)

Each version record captures who or what caused the snapshot:
- `MANUAL`: Generated when a developer explicitly saves code or creates a file in the IDE.
- `AI`: Generated automatically when an AI changeset or code action is applied.
- `ROLLBACK`: Generated when a file is restored to an earlier historical version or when an applied changeset is rolled back.

---

## 4. REST API Reference

### List File Version History
`GET /api/v1/projects/{projectId}/files/{fileId}/versions`

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": "e43b17c2-f12b-4d43-9828-403487d7b003",
      "fileId": "8b087093-68d2-43f1-a1b7-b08bc8d91c5e",
      "versionNumber": 3,
      "content": "...",
      "contentHash": "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8",
      "source": "AI",
      "createdAt": "2026-09-24T18:45:00Z"
    },
    {
      "id": "c12a8742-b13a-4a21-9988-512098b1a002",
      "fileId": "8b087093-68d2-43f1-a1b7-b08bc8d91c5e",
      "versionNumber": 2,
      "content": "...",
      "contentHash": "4b227777d4dd1fc61c6f884f48641d02b4d121d3fd328cb08b5531fcacdabf8a",
      "source": "MANUAL",
      "createdAt": "2026-09-24T18:30:00Z"
    }
  ],
  "message": "File versions retrieved successfully"
}
```

### Get Specific Version Snapshot
`GET /api/v1/projects/{projectId}/files/{fileId}/versions/{versionId}`

### Restore File Version
`POST /api/v1/projects/{projectId}/files/{fileId}/versions/{versionId}/restore`

Restores the file's current content to the snapshot's content and creates a new `ROLLBACK` version record.
