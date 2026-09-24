-- Flyway Migration V19: Create Deployments Table
CREATE TABLE deployments (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    build_id UUID REFERENCES builds(id) ON DELETE SET NULL,
    artifact_id UUID REFERENCES artifacts(id) ON DELETE SET NULL,
    environment VARCHAR(32) NOT NULL DEFAULT 'DEVELOPMENT',
    status VARCHAR(32) NOT NULL DEFAULT 'QUEUED',
    deployment_url VARCHAR(512),
    runtime VARCHAR(64),
    version VARCHAR(64),
    allocated_port INT,
    container_id VARCHAR(128),
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_deployments_project_id ON deployments(project_id);
CREATE INDEX idx_deployments_user_id ON deployments(user_id);
CREATE INDEX idx_deployments_status ON deployments(status);
CREATE INDEX idx_deployments_env ON deployments(environment);
