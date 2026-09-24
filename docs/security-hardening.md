# Security Hardening Guidelines — DevPilot AI

## 1. Cloud Secrets Management
- All cloud credentials (AWS Secret Key, Azure Tokens, GCP Service Account keys) are encrypted using AES-256-GCM (`SecretManager`).
- Master encryption key passed via environment variable `DEV_PILOT_MASTER_KEY`.

## 2. Request Correlation & MDC Traceability
- `RequestCorrelationFilter` injects `requestId` into MDC context for every request.
- Prevents un-tracked requests and facilitates log auditing.

## 3. Database Security
- Non-root database connections in PostgreSQL.
- Database access restricted to local application subnet or Docker bridge network.
