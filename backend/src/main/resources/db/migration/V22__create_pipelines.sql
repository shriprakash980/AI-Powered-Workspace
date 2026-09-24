-- Flyway Migration V22: Create Pipelines Table
CREATE TABLE IF NOT EXISTS pipelines (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    config_path VARCHAR(255) NOT NULL DEFAULT 'devpilot-ci.yml',
    default_branch VARCHAR(100) NOT NULL DEFAULT 'main',
    trigger_on_push BOOLEAN NOT NULL DEFAULT TRUE,
    trigger_on_pull_request BOOLEAN NOT NULL DEFAULT TRUE,
    allow_manual_trigger BOOLEAN NOT NULL DEFAULT TRUE,
    auto_deploy_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    deployment_environment VARCHAR(32) NOT NULL DEFAULT 'DEVELOPMENT',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_pipelines_project_id ON pipelines(project_id);
