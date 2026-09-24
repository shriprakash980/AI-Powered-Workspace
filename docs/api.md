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

### 2.1 Authentication & Security (Phase 5 Implemented)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Public | Register new user account with BCrypt password hashing and default `ROLE_USER`. |
| `POST` | `/api/v1/auth/login` | Public | Authenticate email and password, issue short-lived JWT access token and refresh token. |
| `POST` | `/api/v1/auth/refresh` | Public | Rotates refresh token and issues a fresh JWT access token. |
| `POST` | `/api/v1/auth/logout` | Public/Auth | Revokes the active refresh token session. |
| `GET` | `/api/v1/auth/me` | Authenticated | Retrieve current authenticated user profile and roles from JWT context. |

#### Request & Response Details:

##### `POST /api/v1/auth/register`
* **Request:**
```json
{
  "fullName": "Jane Developer",
  "email": "jane@devpilot.ai",
  "password": "SecurePassword123!"
}
```
* **Response (201 Created):**
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "fullName": "Jane Developer",
    "email": "jane@devpilot.ai",
    "roles": ["USER"],
    "status": "ACTIVE",
    "createdAt": "2026-09-24T12:00:00Z",
    "updatedAt": "2026-09-24T12:00:00Z"
  }
}
```

##### `POST /api/v1/auth/login`
* **Request:**
```json
{
  "email": "jane@devpilot.ai",
  "password": "SecurePassword123!"
}
```
* **Response (200 OK):**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "refreshToken": "xK8qV_9Pz1b3N...",
    "user": {
      "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "fullName": "Jane Developer",
      "email": "jane@devpilot.ai",
      "roles": ["USER"]
    }
  }
}
```

##### `POST /api/v1/auth/refresh`
* **Request:**
```json
{
  "refreshToken": "xK8qV_9Pz1b3N..."
}
```
* **Response (200 OK):** Returns new `accessToken` and rotated new `refreshToken`.

##### `GET /api/v1/auth/me`
* **Headers:** `Authorization: Bearer <access_token>`
* **Response (200 OK):** Returns current user details.

---

### 2.2 Role-Based Administration (Phase 5 Implemented)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/v1/admin/test` | `ROLE_ADMIN` | Verification endpoint returning 200 for admins and 403 Forbidden for regular users. |

---

### 2.3 Projects Management (Phase 4 & 5 Protected)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/v1/projects` | Authenticated | Lists active projects owned by the requesting user. |
| `POST` | `/api/v1/projects` | Authenticated | Creates a new workspace project, automatically assigning `ownerId`. |
| `GET` | `/api/v1/projects/{id}` | Authenticated | Retrieves project if owned by user (otherwise 403 Forbidden). |
| `PUT` | `/api/v1/projects/{id}` | Authenticated | Updates project configuration if owned by user. |
| `DELETE` | `/api/v1/projects/{id}` | Authenticated | Soft deletes project if owned by user. |

---

### 2.4 Files & Folders (Scheduled for Phase 6)
- `GET    /api/v1/projects/{id}/tree` — Retrieve full hierarchical directory tree.
- `POST   /api/v1/projects/{id}/files` — Create new file in project.
- `GET    /api/v1/files/{fileId}` — Fetch raw file content and metadata.
- `PUT    /api/v1/files/{fileId}` — Save file content.
- `DELETE /api/v1/files/{fileId}` — Remove file from project.

---

### 2.5 Future Phases (AI, Execution & Deployment)
- `POST /api/v1/ai/chat`, `/api/v1/ai/generate`, `/api/v1/ai/explain`, `/api/v1/ai/refactor` (Phase 7)
- `POST /api/v1/execution/run`, `GET /api/v1/execution/status/{jobId}` (Phase 8)
- `POST /api/v1/deployments` (Phase 10)
