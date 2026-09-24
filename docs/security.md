# DevPilot AI — Security Architecture & Guidelines

## 1. Authentication & Token Management
- **Password Protection:** Passwords are never stored in plaintext; salted and hashed using `BCryptPasswordEncoder` with strength factor 12.
- **Stateless Tokens:**
  - Access Token: Short-lived (15 minutes), signed with HMAC-SHA256 (minimum 256-bit secret key).
  - Refresh Token: Long-lived (7 days), stored in the database with revocation tracking and single-use rotation.
- **Zero Frontend Credential Leakage:** No database passwords, JWT signing secrets, GitHub OAuth client secrets, or AI API keys are exposed to the client.

---

## 2. API & Network Hardening
- **Cross-Origin Resource Sharing (CORS):** Explicitly restricted to trusted origin whitelist (`http://localhost:3000`, `https://app.devpilot.ai`).
- **Security Headers:** Spring Security configured with:
  - `Content-Security-Policy (CSP)`
  - `X-Frame-Options: SAMEORIGIN` (or `DENY` except for sandboxed live previews using `<iframe sandbox="...">`)
  - `X-Content-Type-Options: nosniff`
  - `Strict-Transport-Security (HSTS)`
- **Rate Limiting:** IP and user-based token bucket rate limiting (Bucket4j) on authentication (`/api/v1/auth/*`) and AI endpoints (`/api/v1/ai/*`).

---

## 3. Sandboxed Code Execution Safeguards
- **Host System Isolation:** Execution jobs run in ephemeral, unprivileged Docker containers (`--user 1000:1000`, `--cap-drop=ALL`).
- **Resource Constraints:** Hard limits enforced via Docker cgroups:
  - Max Memory: `512MB`
  - CPU Quota: `1.0` (1 core)
  - Execution Wall-clock Timeout: `30 seconds`
  - Max Output Buffer: `100KB`
- **Whitelisted Commands:** Dangerous system binaries (`rm -rf /`, `curl`, `wget`, `nc`, `mkfs`) are intercepted or restricted.
