# GitHub Webhooks & Security Verification

## Webhook Endpoint
`POST /api/v1/webhooks/github`

## Security Verification
1. **HMAC SHA-256 Signature:** Webhook payloads are verified against `GITHUB_WEBHOOK_SECRET` using `X-Hub-Signature-256`.
2. **Constant-Time Comparison:** Uses `MessageDigest.isEqual(...)` to prevent timing side-channel attacks.
3. **Delivery Deduplication:** Tracks `X-GitHub-Delivery` IDs to prevent replay attacks and duplicate executions.
4. **Fork Pull Request Restrictions:** PRs from fork repositories execute with restricted privileges (No deployment, secrets withheld).
