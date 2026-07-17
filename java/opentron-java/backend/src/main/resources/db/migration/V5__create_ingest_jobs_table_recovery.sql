-- V5: Create IngestJobs table 
DROP TABLE IF EXISTS ingest_jobs CASCADE;
-- Create the ingest_jobs table for tracking data ingestion jobs
CREATE TABLE IF NOT EXISTS ingest_jobs (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(255) NOT NULL UNIQUE,
    connector_type VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'queued',
    message VARCHAR(2048),
    items_processed INTEGER DEFAULT 0,
    items_failed INTEGER DEFAULT 0,
    bytes_ingested BIGINT DEFAULT 0,
    chunks_created INTEGER DEFAULT 0,
    source VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP
);

-- Create indexes for efficient querying
CREATE INDEX IF NOT EXISTS idx_ingest_status ON ingest_jobs(status);
CREATE INDEX IF NOT EXISTS idx_ingest_connector_type ON ingest_jobs(connector_type);
CREATE INDEX IF NOT EXISTS idx_ingest_created_at ON ingest_jobs(created_at);
CREATE INDEX IF NOT EXISTS idx_ingest_job_id ON ingest_jobs(job_id);

-- Composite indexes for common queries
CREATE INDEX IF NOT EXISTS idx_ingest_connector_status ON ingest_jobs(connector_type, status);
CREATE INDEX IF NOT EXISTS idx_ingest_created_status ON ingest_jobs(created_at DESC, status);
