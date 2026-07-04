-- Flyway Migration V1: Initial PostgreSQL Schema
-- Agent Memory Table
CREATE TABLE IF NOT EXISTS agent_memory (
    id BIGSERIAL PRIMARY KEY,
    agent_name VARCHAR(255) NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    raw_trace TEXT,
    compressed_summary TEXT,
    embedding BYTEA,
    relevance_score DOUBLE PRECISION,
    trace_hash VARCHAR(64) UNIQUE,
    is_archived BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_agent_timestamp ON agent_memory(agent_name, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_agent_name ON agent_memory(agent_name);
CREATE INDEX IF NOT EXISTS idx_trace_hash ON agent_memory(trace_hash);

-- Trace Logs Table
CREATE TABLE IF NOT EXISTS trace_logs (
    id BIGSERIAL PRIMARY KEY,
    agent VARCHAR(255) NOT NULL,
    input TEXT,
    output TEXT,
    tools_used TEXT,
    duration_ms INTEGER,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_compressed BOOLEAN DEFAULT FALSE,
    compressed_data BYTEA
);

CREATE INDEX IF NOT EXISTS idx_trace_agent ON trace_logs(agent);
CREATE INDEX IF NOT EXISTS idx_trace_timestamp ON trace_logs(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_trace_agent_timestamp ON trace_logs(agent, timestamp DESC);

-- Skills Registry Table
CREATE TABLE IF NOT EXISTS skills (
    name VARCHAR(255) PRIMARY KEY,
    version VARCHAR(50) NOT NULL,
    manifest_json TEXT NOT NULL,
    installed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_enabled BOOLEAN DEFAULT TRUE
);

-- Document Index Table
CREATE TABLE IF NOT EXISTS document_index (
    id BIGSERIAL PRIMARY KEY,
    path VARCHAR(1024) NOT NULL,
    chunk TEXT,
    embedding BYTEA,
    chunk_index INTEGER,
    full_text_search TSVECTOR
);

CREATE INDEX IF NOT EXISTS idx_doc_path ON document_index(path);
CREATE INDEX IF NOT EXISTS idx_doc_fts ON document_index USING GIN(full_text_search);

-- Deduplication tracking table
CREATE TABLE IF NOT EXISTS trace_dedup (
    id BIGSERIAL PRIMARY KEY,
    trace_hash VARCHAR(64) UNIQUE NOT NULL,
    count INTEGER DEFAULT 1,
    last_seen TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Archive Tables
CREATE TABLE IF NOT EXISTS agent_memory_archive (
    id BIGSERIAL PRIMARY KEY,
    agent_name VARCHAR(255) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    raw_trace TEXT,
    compressed_summary TEXT,
    embedding BYTEA,
    archived_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_archive_agent_memory ON agent_memory_archive(agent_name);
CREATE INDEX IF NOT EXISTS idx_archive_memory_timestamp ON agent_memory_archive(archived_at DESC);

CREATE TABLE IF NOT EXISTS trace_logs_archive (
    id BIGSERIAL PRIMARY KEY,
    agent VARCHAR(255) NOT NULL,
    input TEXT,
    output TEXT,
    tools_used TEXT,
    duration_ms INTEGER,
    timestamp TIMESTAMP NOT NULL,
    archived_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_archive_agent ON trace_logs_archive(agent);
CREATE INDEX IF NOT EXISTS idx_archive_timestamp ON trace_logs_archive(archived_at DESC);

-- Session/State Management Table
CREATE TABLE IF NOT EXISTS agent_sessions (
    id BIGSERIAL PRIMARY KEY,
    agent_name VARCHAR(255) NOT NULL,
    session_id VARCHAR(64) UNIQUE NOT NULL,
    state_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_session_agent ON agent_sessions(agent_name);
CREATE INDEX IF NOT EXISTS idx_session_id ON agent_sessions(session_id);
CREATE INDEX IF NOT EXISTS idx_session_active ON agent_sessions(is_active) WHERE is_active = TRUE;

-- Metrics/Statistics Table
CREATE TABLE IF NOT EXISTS storage_metrics (
    id BIGSERIAL PRIMARY KEY,
    metric_name VARCHAR(255) NOT NULL,
    metric_value DOUBLE PRECISION,
    agent_name VARCHAR(255),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_metrics_name ON storage_metrics(metric_name);
CREATE INDEX IF NOT EXISTS idx_metrics_agent ON storage_metrics(agent_name);
CREATE INDEX IF NOT EXISTS idx_metrics_timestamp ON storage_metrics(timestamp DESC);

-- Additional composite index
CREATE INDEX IF NOT EXISTS idx_memory_agent_archived ON agent_memory(agent_name, is_archived) WHERE is_archived = FALSE;

-- PostgreSQL extensions (IF NOT EXISTS supported)
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS btree_gin;
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- Analytics Views (using CREATE OR REPLACE)
CREATE OR REPLACE VIEW agent_statistics AS
SELECT 
    agent_name,
    COUNT(*) as total_entries,
    COUNT(*) FILTER (WHERE is_archived = FALSE) as active_entries,
    COUNT(*) FILTER (WHERE is_archived = TRUE) as archived_entries,
    MAX(timestamp) as last_update,
    ROUND(AVG(COALESCE(relevance_score, 0))::numeric, 2) as avg_relevance
FROM agent_memory
GROUP BY agent_name;

CREATE OR REPLACE VIEW trace_statistics AS
SELECT 
    agent,
    COUNT(*) as total_traces,
    COUNT(*) FILTER (WHERE is_compressed = TRUE) as compressed_count,
    COUNT(*) FILTER (WHERE is_compressed = FALSE) as uncompressed_count,
    AVG(duration_ms) as avg_duration_ms,
    MAX(duration_ms) as max_duration_ms,
    MIN(duration_ms) as min_duration_ms,
    MAX(timestamp) as last_trace
FROM trace_logs
GROUP BY agent;

CREATE OR REPLACE VIEW compression_analysis AS
SELECT 
    agent,
    COUNT(*) as total_traces,
    COUNT(*) FILTER (WHERE is_compressed = TRUE) as compressed_traces,
    ROUND(
        (COUNT(*) FILTER (WHERE is_compressed = TRUE)::numeric / NULLIF(COUNT(*), 0) * 100)::numeric, 
        2
    ) as compression_ratio_percent,
    AVG(LENGTH(COALESCE(compressed_data, ''))) as avg_compressed_size_bytes
FROM trace_logs
WHERE is_compressed = TRUE
GROUP BY agent;
