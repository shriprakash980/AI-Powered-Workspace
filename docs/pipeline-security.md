# CI/CD Pipeline Security Specification

## Security Enforcement Rules
1. **No Host Execution:** Arbitrary shell execution on host machine is strictly blocked.
2. **Secret Masking:** Environment variables, API keys, tokens, and secrets are automatically regex-masked in all log streams.
3. **Controlled Step Allowlist:** Only `CHECKOUT`, `INSTALL`, `BUILD`, `TEST`, `PACKAGE`, `ARTIFACT`, `IMAGE_BUILD`, `DEPLOY`, `HEALTH_CHECK` step types are permitted.
4. **Fork PR Security:** Pull requests from forks cannot automatically trigger container deployments or access production secrets.
