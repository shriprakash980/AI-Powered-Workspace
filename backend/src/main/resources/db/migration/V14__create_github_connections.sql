-- DevPilot AI — Flyway Migration V14: GitHub Connections Schema

CREATE TABLE github_connections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    github_user_id VARCHAR(100),
    username VARCHAR(100),
    access_token_encrypted TEXT NOT NULL,
    token_type VARCHAR(50) DEFAULT 'bearer',
    scopes VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMPTZ
);

-- Performance and Integrity Indexes
CREATE INDEX idx_github_connections_user_id ON github_connections(user_id);
CREATE UNIQUE INDEX idx_github_connections_active_user ON github_connections(user_id) WHERE revoked_at IS NULL;
