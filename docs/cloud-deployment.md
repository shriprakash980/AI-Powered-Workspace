# Cloud Deployment Guide — DevPilot AI

## 1. Overview
DevPilot AI supports multi-cloud container deployment target management across **Local Docker**, **AWS ECS/App Runner**, **Azure Container Apps**, and **GCP Cloud Run**.

## 2. Supported Cloud Providers
- **Local Docker Engine**: Development, local testing & sandbox execution.
- **AWS**: AWS ECS Fargate, ECR registry, AWS App Runner.
- **Azure**: Azure Container Apps (ACA), Azure Container Registry (ACR).
- **GCP**: Google Cloud Run, Artifact Registry.

## 3. Deployment Workflow
```text
Source Code
   ↓
CI/CD Pipeline / Build Sandbox
   ↓
Docker Image Packaged
   ↓
Secret Manager (AES-256-GCM Credential Verification)
   ↓
Production Approval Gate (Manual approval required for PRODUCTION)
   ↓
Cloud Deployment Target Execution
   ↓
Health Check Engine & Observability Monitoring
```

## 4. API Endpoints
- `GET /api/v1/cloud/providers`: List available cloud providers.
- `POST /api/v1/cloud/credentials`: Store AES-256-GCM encrypted cloud credentials.
- `GET /api/v1/projects/{projectId}/deployment-targets`: List active deployment targets.
- `POST /api/v1/projects/{projectId}/deployment-targets`: Register new deployment target.
- `POST /api/v1/projects/{projectId}/deployment-targets/{targetId}/approve`: Approve production deployment.
- `POST /api/v1/projects/{projectId}/deployment-targets/{targetId}/health-check`: Trigger on-demand health check.
