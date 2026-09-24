# DevPilot AI — CI/CD Pipeline Architecture

## Overview
DevPilot AI features an automated CI/CD Pipeline engine that orchestrates code checkout, dependency installation, compilation, testing, packaging, artifact creation, container deployment, and health checking.

## Architecture

```
GitHub Push / PR / Manual
           │
           ▼
  GitHubWebhookController
           │
           ▼
  WebhookVerificationService (HMAC SHA-256)
           │
           ▼
  PipelineTriggerService
           │
           ▼
  PipelineQueue (Bounded In-Process Queue)
           │
           ▼
  PipelineWorker (Thread Pool Executor)
           │
           ├── CHECKOUT (GitWorkspaceManager)
           ├── INSTALL / BUILD / TEST / PACKAGE (BuildService / Sandbox)
           ├── ARTIFACT (ArtifactService)
           └── DEPLOY (DeploymentService / LocalDockerDeploymentProvider)
```

## Supported Pipeline Steps
1. **CHECKOUT:** Synchronizes database files to isolated disk workspace.
2. **INSTALL:** Installs project dependencies.
3. **BUILD:** Compiles application source code.
4. **TEST:** Runs unit and integration test suite.
5. **PACKAGE:** Builds output JAR / WAR / bundle.
6. **ARTIFACT:** Stores artifact bundle with SHA-256 checksum in `ArtifactStorage`.
7. **IMAGE_BUILD:** Builds container image in Docker sandbox.
8. **DEPLOY:** Provisions container deployment via `DeploymentProvider`.
9. **HEALTH_CHECK:** Polls active container port.

## API Endpoints
- `GET /api/v1/projects/{projectId}/pipelines`
- `POST /api/v1/projects/{projectId}/pipelines`
- `GET /api/v1/projects/{projectId}/pipelines/{pipelineId}`
- `PUT /api/v1/projects/{projectId}/pipelines/{pipelineId}`
- `DELETE /api/v1/projects/{projectId}/pipelines/{pipelineId}`
- `GET /api/v1/projects/{projectId}/pipelines/{pipelineId}/runs`
- `GET /api/v1/projects/{projectId}/pipelines/{pipelineId}/runs/{runId}`
- `POST /api/v1/projects/{projectId}/pipelines/{pipelineId}/run`
- `POST /api/v1/projects/{projectId}/pipelines/{pipelineId}/runs/{runId}/cancel`
- `POST /api/v1/projects/{projectId}/pipelines/{pipelineId}/runs/{runId}/rerun`
- `GET /api/v1/projects/{projectId}/pipelines/{pipelineId}/runs/{runId}/steps`
- `GET /api/v1/projects/{projectId}/pipelines/{pipelineId}/runs/{runId}/logs`
- `POST /api/v1/projects/{projectId}/pipelines/{pipelineId}/runs/{runId}/diagnose`
