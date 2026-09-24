-- Flyway Migration V28: Create Service Health Checks Table
CREATE TABLE IF NOT EXISTS service_health_checks (
    id UUID PRIMARY KEY,
    deployment_id UUID NOT NULL REFERENCES deployments(id) ON DELETE CASCADE,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL,
    response_time_ms LONG,
    health_check_url VARCHAR(512),
    details TEXT,
    checked_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_service_health_checks_deployment_id ON service_health_checks(deployment_id);
CREATE INDEX IF NOT EXISTS idx_service_health_checks_project_id ON service_health_checks(project_id);
