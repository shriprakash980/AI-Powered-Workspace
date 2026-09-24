-- Flyway Migration V27: Create Deployment Events Table
CREATE TABLE IF NOT EXISTS deployment_events (
    id UUID PRIMARY KEY,
    deployment_id UUID NOT NULL REFERENCES deployments(id) ON DELETE CASCADE,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    event_type VARCHAR(32) NOT NULL,
    message TEXT NOT NULL,
    details TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_deployment_events_deployment_id ON deployment_events(deployment_id);
CREATE INDEX IF NOT EXISTS idx_deployment_events_project_id ON deployment_events(project_id);
