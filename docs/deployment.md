# DevPilot AI — Deployment Engine Architecture

## Overview

The DevPilot AI Deployment Platform enables dynamic container sandbox provisioning, dynamic host port mapping (10000–11000), deployment lifecycle management (Start, Stop, Restart, Rollback), and real-time container log streaming.

---

## 1. Dynamic Port Allocation

Ports are dynamically allocated for user environments:
- **Port Range**: `10000` to `11000` (1,000 available container ports).
- **Concurrency Control**: Thread-safe memory tracking (`ConcurrentHashMap`) backed by host socket reuse address checks (`ServerSocket.setReuseAddress(true)`).
- **Auto-Release**: Ports are immediately returned to the free pool upon container termination or deployment cleanup.

---

## 2. Deployment Environments & Quotas

Supports multiple deployment environments per project:
- **Environments**: `DEVELOPMENT`, `STAGING`, `PRODUCTION`.
- **User Limits**: Enforces `MAX_RUNNING_DEPLOYMENTS_PER_USER = 2` to prevent host resource exhaustion.
- **Project Limits**: Enforces `MAX_DEPLOYMENTS_PER_PROJECT = 10` historical deployments.

---

## 3. Rollback & Health Monitoring

- **Rollback Engine**: Instantly points environment traffic back to the latest successful deployment version and associated artifact.
- **Health Checks**: Polls container endpoint status every 5 seconds up to a 30-second initialization timeout threshold.
- **Automatic Cleanup**: Background cleanup service (`DeploymentCleanupService`) auto-reclaims abandoned or orphaned containers and network resources.

---

## 4. REST API Endpoints

- `POST /api/v1/projects/{projectId}/deployments`: Triggers container deployment.
- `GET /api/v1/projects/{projectId}/deployments`: Lists deployment history.
- `GET /api/v1/projects/{projectId}/deployments/{deploymentId}`: Gets deployment status and live URL.
- `GET /api/v1/projects/{projectId}/deployments/{deploymentId}/logs`: Retrieves container STDOUT/STDERR logs.
- `POST /api/v1/projects/{projectId}/deployments/{deploymentId}/stop`: Stops running deployment.
- `POST /api/v1/projects/{projectId}/deployments/{deploymentId}/restart`: Restarts deployment container.
- `POST /api/v1/projects/{projectId}/deployments/{deploymentId}/rollback`: Rolls back deployment to prior stable build.
- `POST /api/v1/projects/{projectId}/deployments/{deploymentId}/diagnose`: AI failure diagnosis for deployment crashes.
