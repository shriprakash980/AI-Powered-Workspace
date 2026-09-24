-- Flyway Migration V24: Create Pipeline Steps Table
CREATE TABLE IF NOT EXISTS pipeline_steps (
    id UUID PRIMARY KEY,
    pipeline_run_id UUID NOT NULL REFERENCES pipeline_runs(id) ON DELETE CASCADE,
    step_name VARCHAR(120) NOT NULL,
    step_type VARCHAR(32) NOT NULL,
    step_order INT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'QUEUED',
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    duration_ms BIGINT,
    exit_code INT,
    log_reference VARCHAR(255),
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_pipeline_steps_pipeline_run_id ON pipeline_steps(pipeline_run_id);
