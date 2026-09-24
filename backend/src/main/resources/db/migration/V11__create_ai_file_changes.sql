-- DevPilot AI — Flyway Migration V11: AI File Changes Schema

CREATE TABLE ai_file_changes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    changeset_id UUID NOT NULL REFERENCES ai_changesets(id) ON DELETE CASCADE,
    file_id UUID REFERENCES project_files(id) ON DELETE SET NULL,
    operation VARCHAR(30) NOT NULL,
    old_path VARCHAR(500),
    new_path VARCHAR(500),
    old_content_hash VARCHAR(64),
    proposed_content_hash VARCHAR(64),
    original_content TEXT,
    proposed_content TEXT,
    diff TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'PROPOSED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    applied_at TIMESTAMPTZ
);

-- Indexes for performance & foreign key lookups
CREATE INDEX idx_ai_file_changes_changeset ON ai_file_changes(changeset_id);
CREATE INDEX idx_ai_file_changes_file ON ai_file_changes(file_id);
CREATE INDEX idx_ai_file_changes_status ON ai_file_changes(status);
