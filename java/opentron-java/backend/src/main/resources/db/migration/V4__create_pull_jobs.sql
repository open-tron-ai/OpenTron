-- Flyway migration: create pull_jobs table
-- Adds the table backing the PullJob JPA entity
CREATE TABLE IF NOT EXISTS pull_jobs (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(255) NOT NULL UNIQUE,
    model_name VARCHAR(255),
    status VARCHAR(100),
    message VARCHAR(1024),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_pull_jobs_created_at ON pull_jobs(created_at);
