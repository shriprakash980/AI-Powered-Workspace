# Production Deployment Guide — DevPilot AI

## 1. Safety Controls
- **Manual Approval Engine**: Deployments targeting `PRODUCTION` require explicit approval via `DeploymentApprovalService`.
- **Environment Isolation**: Production secrets are stored with master-key AES-256-GCM encryption.
- **Health Validation**: Service pings target URL before marking deployment status as `ACTIVE` / `RUNNING`.

## 2. Ingress & Load Balancing
- Nginx reverse proxy configuration provided in `infrastructure/nginx/nginx.conf`.
- Rate limiting configured to 50 requests/sec with burst buffer of 20.
- Automatic WebSocket connection upgrade for terminal and streaming logs.

## 3. Deployment Approval API
- `POST /api/v1/projects/{projectId}/deployment-targets/{targetId}/approve`
- `POST /api/v1/projects/{projectId}/deployment-targets/{targetId}/reject`
