# DevPilot AI — GitHub Integration Documentation

## 1. Overview
DevPilot AI integrates with GitHub via OAuth 2.0 and GitHub REST API v3. Users can link their GitHub account, list public and private repositories, clone/import GitHub repositories into isolated DevPilot projects, and open/manage Pull Requests directly within the DevPilot workspace.

---

## 2. Security & Credentials Strategy
- **AES-256-GCM Encryption**: OAuth access tokens are never saved as plaintext. Tokens are encrypted using AES-256-GCM via `TokenEncryptionService` before persisting to PostgreSQL (`github_connections` table).
- **Zero Client Exposure**: The browser client receives only a boolean status (`connected: true`) and GitHub username. Access tokens are kept strictly server-side.
- **Credential Masking**: When returning Git remote URLs to client endpoints, authentication credentials and tokens embedded in URLs are masked (`https://***@github.com/...`).

---

## 3. Endpoints
- `GET /api/v1/github/status` - Checks if user has connected a GitHub account.
- `GET /api/v1/github/oauth/start` - Generates GitHub OAuth authorization URL with CSRF state token.
- `GET /api/v1/github/oauth/callback` - Handles OAuth callback code exchange.
- `POST /api/v1/github/disconnect` - Deletes stored GitHub connection and revokes session.
- `GET /api/v1/github/repositories` - Lists user's repositories from GitHub REST API.
- `POST /api/v1/github/repositories` - Creates a new GitHub repository.
- `POST /api/v1/github/repositories/import` - Clones a remote GitHub repository into a new DevPilot workspace project.
- `GET /api/v1/github/repositories/{owner}/{repo}/pulls` - Lists Pull Requests.
- `POST /api/v1/github/repositories/{owner}/{repo}/pulls` - Opens a new Pull Request on GitHub.
