-- DevPilot AI — Flyway Migration V13: Project Git Metadata Schema

ALTER TABLE projects
    ADD COLUMN IF NOT EXISTS git_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS default_branch VARCHAR(100) NOT NULL DEFAULT 'main',
    ADD COLUMN IF NOT EXISTS git_provider VARCHAR(50) DEFAULT 'GIT',
    ADD COLUMN IF NOT EXISTS last_fetched_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_projects_git_enabled ON projects(git_enabled);
