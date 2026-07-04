-- Add generated_projects table to existing migration
CREATE TABLE IF NOT EXISTS generated_projects (
    id BIGSERIAL PRIMARY KEY,
    project_id VARCHAR(255) NOT NULL UNIQUE,
    project_name VARCHAR(255) NOT NULL,
    project_type VARCHAR(100) NOT NULL,
    framework VARCHAR(100) NOT NULL,
    language VARCHAR(100) NOT NULL,
    description TEXT,
    file_count INTEGER NOT NULL DEFAULT 0,
    size_bytes BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    file_list TEXT,
    files_json TEXT
);

CREATE INDEX IF NOT EXISTS idx_project_id ON generated_projects(project_id);
CREATE INDEX IF NOT EXISTS idx_project_created ON generated_projects(created_at DESC);
