-- Flyway Migration V21: Create Deployment Logs Table
CREATE TABLE deployment_logs (
    id UUID PRIMARY KEY,
    deployment_id UUID NOT NULL REFERENCES deployments(id) ON DELETE CASCADE,
    log_type VARCHAR(16) NOT NULL DEFAULT 'INFO',
    message TEXT NOT NULL,
    sequence_number INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_deployment_logs_deployment_id ON deployment_logs(deployment_id);
CREATE INDEX idx_deployment_logs_sequence ON deployment_logs(deployment_id, sequence_number);
