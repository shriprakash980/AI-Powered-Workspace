-- Flyway Migration V20: Create Project Environment Variables Table
CREATE TABLE project_environment_variables (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name VARCHAR(128) NOT NULL,
    encrypted_value TEXT NOT NULL,
    environment VARCHAR(32) NOT NULL DEFAULT 'DEVELOPMENT',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_proj_env_var UNIQUE (project_id, name, environment)
);

CREATE INDEX idx_proj_env_vars_project_id ON project_environment_variables(project_id);
