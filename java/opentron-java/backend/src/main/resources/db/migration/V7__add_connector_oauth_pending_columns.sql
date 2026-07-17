-- V7: Add pending OAuth columns to connector_states
-- These survive server restarts so the OAuth callback can still exchange
-- the code even if the server was restarted between /connect and /oauth/callback.
ALTER TABLE connector_states
    ADD COLUMN IF NOT EXISTS pending_client_id     VARCHAR(512),
    ADD COLUMN IF NOT EXISTS pending_client_secret VARCHAR(512),
    ADD COLUMN IF NOT EXISTS pending_token_url     VARCHAR(512);
