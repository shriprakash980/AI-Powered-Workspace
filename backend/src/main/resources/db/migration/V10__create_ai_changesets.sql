-- DevPilot AI — Flyway Migration V10: AI Changesets Schema

CREATE TABLE ai_changesets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    conversation_id UUID REFERENCES ai_conversations(id) ON DELETE SET NULL,
    summary TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PROPOSED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    applied_at TIMESTAMPTZ,
    rejected_at TIMESTAMPTZ
);

-- Indexes for performance & query filtering
CREATE INDEX idx_ai_changesets_project ON ai_changesets(project_id);
CREATE INDEX idx_ai_changesets_user ON ai_changesets(user_id);
CREATE INDEX idx_ai_changesets_status ON ai_changesets(status);
CREATE INDEX idx_ai_changesets_created ON ai_changesets(created_at DESC);
