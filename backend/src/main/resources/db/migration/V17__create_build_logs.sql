-- Flyway Migration V17: Create Build Logs Table
CREATE TABLE build_logs (
    id UUID PRIMARY KEY,
    build_id UUID NOT NULL REFERENCES builds(id) ON DELETE CASCADE,
    log_type VARCHAR(16) NOT NULL DEFAULT 'INFO',
    message TEXT NOT NULL,
    sequence_number INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_build_logs_build_id ON build_logs(build_id);
CREATE INDEX idx_build_logs_sequence ON build_logs(build_id, sequence_number);
