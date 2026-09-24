-- Flyway Migration V25: Create Deployment Targets Table
CREATE TABLE IF NOT EXISTS deployment_targets (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    environment VARCHAR(32) NOT NULL DEFAULT 'DEVELOPMENT',
    credential_id UUID,
    region VARCHAR(64),
    service_name VARCHAR(120),
    custom_domain VARCHAR(255),
    status VARCHAR(32) NOT NULL DEFAULT 'INACTIVE',
    approval_required BOOLEAN NOT NULL DEFAULT FALSE,
    approved BOOLEAN NOT NULL DEFAULT TRUE,
    configuration TEXT,
    last_health_check_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_deployment_targets_project_id ON deployment_targets(project_id);
CREATE INDEX IF NOT EXISTS idx_deployment_targets_provider ON deployment_targets(provider);
