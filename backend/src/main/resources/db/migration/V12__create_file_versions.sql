-- DevPilot AI — Flyway Migration V12: File Versions Schema

CREATE TABLE file_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_id UUID NOT NULL REFERENCES project_files(id) ON DELETE CASCADE,
    version_number INTEGER NOT NULL,
    content TEXT,
    content_hash VARCHAR(64) NOT NULL,
    source VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_file_version_number UNIQUE (file_id, version_number)
);

-- Indexes for performance
CREATE INDEX idx_file_versions_file ON file_versions(file_id);
CREATE INDEX idx_file_versions_created ON file_versions(created_at DESC);
CREATE INDEX idx_file_versions_creator ON file_versions(created_by);
