-- DevPilot AI — Flyway Migration V2: Projects Schema

CREATE TABLE projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(120) NOT NULL,
    description TEXT,
    template VARCHAR(60) NOT NULL DEFAULT 'HTML_CSS_JS',
    language VARCHAR(50) NOT NULL,
    framework VARCHAR(50),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    owner_id UUID REFERENCES users(id) ON DELETE SET NULL,
    repository_url VARCHAR(500),
    deployment_url VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Performance Indexes
CREATE INDEX idx_projects_owner ON projects(owner_id);
CREATE INDEX idx_projects_status ON projects(status);
CREATE INDEX idx_projects_created_at ON projects(created_at);
