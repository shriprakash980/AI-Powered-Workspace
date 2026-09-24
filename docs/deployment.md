# DevPilot AI — Deployment & Production Guide

## 1. Local Development Topology
Run the full local environment with Docker Compose:
```bash
docker compose up --build
```
Services spun up:
1. `devpilot-postgres`: PostgreSQL 16 on port `5432` with health checks.
2. `devpilot-backend`: Spring Boot 3 on port `8080`.
3. `devpilot-frontend`: Nginx serving static HTML/CSS/JS on port `3000`.

---

## 2. Production Cloud Topography
- **Frontend Hosting:** Global CDN / AWS S3 + CloudFront / Vercel static serving.
- **Backend API:** Managed container service (AWS ECS Fargate / GCP Cloud Run / Render).
- **Database:** Managed PostgreSQL (AWS RDS / Supabase / Neon) with automated daily snapshots and SSL connection enforcement (`sslmode=require`).
- **Container Execution Fleet:** Docker-in-Docker or microVM runners (AWS Firecracker / gVisor) for secure execution sandboxing.
