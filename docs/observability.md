# System Observability & Monitoring — DevPilot AI

## 1. Metrics & Instrumentation
- **Micrometer + Prometheus**: Exposed at `/actuator/prometheus`.
- **Custom Application Metrics**: Latency, active deployments, memory usage, CPU load.
- **Request Correlation**: MDC `requestId` propagated across HTTP headers (`X-Correlation-ID`) and log statements.

## 2. Health Monitoring Engine
- Automated background check executed every 60 seconds (`HealthCheckService`).
- Health states: `HEALTHY`, `DEGRADED`, `UNHEALTHY`, `UNKNOWN`.

## 3. Grafana Dashboard
- Pre-built dashboard file: `infrastructure/monitoring/grafana/dashboards/devpilot-overview.json`.
- Visualizes HTTP throughput, JVM heap memory, system CPU, and deployment target availability.
