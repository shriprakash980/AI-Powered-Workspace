-- DevPilot AI — Flyway Migration V3: Virtual Project Filesystem Schema

CREATE TABLE project_files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    parent_id UUID REFERENCES project_files(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    path VARCHAR(500) NOT NULL,
    file_type VARCHAR(50),
    content TEXT,
    is_directory BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_project_file_path UNIQUE (project_id, path)
);

-- Performance Indexes
CREATE INDEX idx_project_files_project ON project_files(project_id);
CREATE INDEX idx_project_files_path ON project_files(path);
CREATE INDEX idx_project_files_parent ON project_files(parent_id);
