-- Flyway Migration V26: Create Cloud Credentials Table
CREATE TABLE IF NOT EXISTS cloud_credentials (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL DEFAULT 'Default Cloud Credential',
    provider VARCHAR(32) NOT NULL,
    access_key TEXT,
    secret_key TEXT,
    encrypted_credentials TEXT,
    region VARCHAR(64),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    metadata_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_cloud_credentials_user_id ON cloud_credentials(user_id);
