-- Flyway Migration V16: Create Builds Table
CREATE TABLE builds (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL,
    project_type VARCHAR(32) NOT NULL,
    mode VARCHAR(32) NOT NULL DEFAULT 'BUILD',
    command VARCHAR(512),
    commit_hash VARCHAR(64),
    artifact_path VARCHAR(512),
    exit_code INT,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    duration_ms BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_builds_project_id ON builds(project_id);
CREATE INDEX idx_builds_user_id ON builds(user_id);
CREATE INDEX idx_builds_status ON builds(status);
CREATE INDEX idx_builds_created_at ON builds(created_at);
