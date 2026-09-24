-- DevPilot AI — Flyway Migration V15: Git Operations Audit Schema

CREATE TABLE git_operations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    operation VARCHAR(50) NOT NULL,
    branch VARCHAR(100),
    commit_hash VARCHAR(100),
    status VARCHAR(50) NOT NULL,
    message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Performance and Query Indexes
CREATE INDEX idx_git_operations_project ON git_operations(project_id, created_at DESC);
CREATE INDEX idx_git_operations_user ON git_operations(user_id);
