# DevPilot AI — Deployment & Environment Security Model

## Overview

DevPilot AI enforces isolation and credential encryption to protect user applications and host system infrastructure.

---

## 1. Environment Variable Encryption

- **Encryption Standard**: AES-256-GCM symmetric key encryption with unique Initialization Vectors (IVs) per variable.
- **Masking**: Values are masked in API responses (`••••••••`) and logs to prevent credential leakage.
- **Isolation**: Environment variables are partitioned by project and environment scope (`DEVELOPMENT`, `STAGING`, `PRODUCTION`).

---

## 2. Secret Injection Protection

User environment configuration is isolated from system secrets:
- **Blocked System Keys**: Users cannot set or override system secret keys such as `JWT_SECRET`, `SPRING_DATASOURCE_PASSWORD`, `OPENAI_API_KEY`, `ANTHROPIC_API_KEY`, or `GEMINI_API_KEY`.
- **System Secret Isolation**: System secrets used by DevPilot's backend are never injected into user container environments.

---

## 3. Container & Network Isolation

- **Docker Socket Guard**: Host Docker socket (`/var/run/docker.sock`) is never mounted inside user containers.
- **Port Bounding**: Container ports are strictly bound to localhost interface (`127.0.0.1:PORT`) within the range 10000–11000.
- **Resource Constraints**: User execution containers run with CPU memory limits and non-root execution profiles.
