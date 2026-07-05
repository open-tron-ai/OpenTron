-- Flyway migration: create oauth_tokens table (idempotent)
CREATE TABLE IF NOT EXISTS oauth_tokens (
  id BIGSERIAL PRIMARY KEY,
  connector_id VARCHAR(255),
  provider VARCHAR(255),
  access_token TEXT,
  refresh_token TEXT,
  expires_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
