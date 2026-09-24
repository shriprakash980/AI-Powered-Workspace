-- DevPilot AI — Flyway Migration V8: AI Requests and Audit Logs Schema

CREATE TABLE ai_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    project_id UUID REFERENCES projects(id) ON DELETE SET NULL,
    conversation_id UUID REFERENCES ai_conversations(id) ON DELETE SET NULL,
    provider VARCHAR(50) NOT NULL,
    model VARCHAR(100) NOT NULL,
    request_type VARCHAR(50) NOT NULL,
    prompt_tokens INT DEFAULT 0,
    completion_tokens INT DEFAULT 0,
    total_tokens INT DEFAULT 0,
    latency_ms BIGINT DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'SUCCESS',
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance & auditing
CREATE INDEX idx_ai_requests_user ON ai_requests(user_id);
CREATE INDEX idx_ai_requests_project ON ai_requests(project_id);
CREATE INDEX idx_ai_requests_created ON ai_requests(created_at DESC);
CREATE INDEX idx_ai_requests_status ON ai_requests(status);
