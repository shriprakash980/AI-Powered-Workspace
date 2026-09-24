# DevPilot AI — Security Architecture & Guidelines

## 1. Authentication Architecture & Token Management

### 1.1 Password Security
* **Hashing Mechanism:** Passwords are never stored or logged in plaintext. Salting and adaptive hashing is enforced using `BCryptPasswordEncoder` with a work factor of `12`.
* **Registration Validation:**
  * Mandatory full name, valid normalized email (lowercased and trimmed).
  * Minimum 8 characters, maximum 100 characters.
  * Duplicate email uniqueness enforced at both application service and database constraint levels.
* **Zero Credential Leakage:** User passwords, password hashes, and raw refresh tokens are never returned in API responses, logs, or client-side storage.

### 1.2 JWT Access Tokens (Stateless Authentication)
* **Standard:** JSON Web Token (RFC 7519) signed using HMAC-SHA256 with a minimum 256-bit secret key.
* **Claims Structure:**
  ```json
  {
    "sub": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "email": "developer@devpilot.ai",
    "roles": ["USER"],
    "iss": "devpilot-ai",
    "iat": 1727164800,
    "exp": 1727165700
  }
  ```
* **Lifespan:** Short-lived (default 15 minutes / 900,000 ms), configurable via `${JWT_ACCESS_TOKEN_EXPIRATION}`.
* **Key Configuration:** Secret key externalized via `${JWT_SECRET}`.

### 1.3 Refresh Token System & Single-Use Rotation
* **Cryptographic Storage:** Only the **SHA-256 hash** of the raw refresh token is stored in the PostgreSQL `refresh_tokens` table. Raw tokens are never persisted.
* **Rotation Policy:**
  1. On each `/api/v1/auth/refresh` request, the presented refresh token is verified for validity, non-revocation, and non-expiration.
  2. The old token is immediately invalidated (`revoked = true`).
  3. A new raw refresh token is cryptographically generated, hashed, and stored.
  4. Both a new access token and new refresh token are returned to the client.
* **Token Reuse Detection:** If an already-revoked refresh token is presented, access is rejected immediately with `401 Unauthorized`.
* **Lifespan:** Default 7 days / 604,800,000 ms, configurable via `${JWT_REFRESH_TOKEN_EXPIRATION}`.

---

## 2. Authorization & Role-Based Access Control (RBAC)

### 2.1 Role Hierarchy & Permissions
* **`ROLE_USER`:**
  * Can register and authenticate.
  * Can create, read, update, and soft-delete their **own** projects.
  * Can manage project files and virtual filesystem.
  * Denied access to administrative endpoints (`/api/v1/admin/**`).
* **`ROLE_ADMIN`:**
  * Full administrative access across platform resources.
  * Verified via `@PreAuthorize("hasRole('ADMIN')")`.
  * Can inspect and manage system health and cross-workspace resources.

### 2.2 Project Ownership Protection
Project ownership is strictly validated in the **Service Layer** (`ProjectService`), not just on the client:
* `GET /api/v1/projects`: Filters results to only return workspaces owned by the authenticated user (`ownerId == principal.getId()`). Administrators can view all active projects.
* `POST /api/v1/projects`: Project `ownerId` is populated directly from the verified `SecurityContextHolder` `UserPrincipal`. Any `ownerId` sent by client payloads is ignored.
* `GET /api/v1/projects/{id}`: If the requesting user does not own the project and is not an administrator, access is rejected with `403 Forbidden`.
* `PUT /api/v1/projects/{id}`: Updating a project requires verified ownership; otherwise returns `403 Forbidden`.
* `DELETE /api/v1/projects/{id}`: Deleting a project requires verified ownership; otherwise returns `403 Forbidden`.

---

## 3. Spring Security Filter Pipeline

```
Incoming Request
  │
  ▼
CorsFilter (Validates Origin)
  │
  ▼
HeaderWriterFilter (CSP, X-Frame-Options, X-Content-Type-Options)
  │
  ▼
JwtAuthenticationFilter (OncePerRequestFilter)
  ├── Extracts 'Authorization: Bearer <token>'
  ├── Validates JWT signature & expiration via JwtService
  ├── Loads UserDetails via CustomUserDetailsService
  ├── Verifies UserStatus (ACTIVE enabled, SUSPENDED locked)
  └── Sets UsernamePasswordAuthenticationToken in SecurityContext
  │
  ▼
AuthorizationFilter
  ├── Public: /api/v1/auth/register, /login, /refresh, /logout, /health, /swagger-ui/**
  ├── Admin: /api/v1/admin/** (hasRole('ADMIN'))
  └── Protected: /api/v1/auth/me, /api/v1/projects/** (authenticated)
  │
  ▼
REST Controller -> Service -> Database
```

---

## 4. Frontend Security & Automatic Refresh

* **Token Storage:**
  * Access tokens and user profiles are kept in `sessionStorage` (scoped to the browser session and tab).
  * In production deployments, refresh tokens should ideally be transferred via `HttpOnly`, `Secure`, `SameSite=Strict` cookies to mitigate XSS exposure.
* **Controlled Automatic Refresh:**
  * When an authenticated API call receives a `401 Unauthorized`, `apiRequest()` attempts a single refresh request against `/api/v1/auth/refresh`.
  * If successful, the original request is replayed once with the new token.
  * If refresh fails or no refresh token exists, the session is cleared and the user is redirected to `login.html`.
  * Loop prevention flag (`isRetry`) guarantees that requests are never retried infinitely.

---

## 5. Security Headers Configured

* `X-Content-Type-Options: nosniff` (Prevents MIME-sniffing attacks)
* `X-Frame-Options: SAMEORIGIN` (Protects against clickjacking while allowing trusted previews)
* `Referrer-Policy: strict-origin-when-cross-origin`
* `Content-Security-Policy`: Restricts frame ancestors to 'self'.

---

## 6. Brute-Force & Abuse Mitigation (Roadmap)
* The authentication layer is architected to integrate IP-based and username-based rate-limiting filters (Bucket4j / Redis token bucket) on `/api/v1/auth/*` before public cloud deployment.
