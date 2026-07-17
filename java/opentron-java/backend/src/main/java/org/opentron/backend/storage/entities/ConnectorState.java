package org.opentron.backend.storage.entities;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Persists the connected/disconnected state and credential token for each connector.
 * One row per connector_id. Upserted on connect, cleared on disconnect.
 */
@Entity
@Table(name = "connector_states")
public class ConnectorState {

    @Id
    @Column(name = "connector_id", length = 64)
    private String connectorId;

    @Column(name = "display_name", length = 128)
    private String displayName;

    @Column(name = "connected", nullable = false)
    private boolean connected = false;

    /** Encrypted or plain credential token — app-password, API key, OAuth access token, path */
    @Column(name = "credential_token", length = 2048)
    private String credentialToken;

    /** Number of memory chunks currently indexed from this connector */
    @Column(name = "chunks_indexed", nullable = false)
    private int chunksIndexed = 0;

    @Column(name = "last_sync_at")
    private Instant lastSyncAt;

    @Column(name = "last_sync_job_id", length = 64)
    private String lastSyncJobId;

    @Column(name = "sync_state", length = 32)
    private String syncState = "idle";   // idle | syncing | error

    @Column(name = "sync_error", length = 1024)
    private String syncError;

    @Column(name = "items_synced", nullable = false)
    private int itemsSynced = 0;

    @Column(name = "oldest_item_date")
    private Instant oldestItemDate;

    @Column(name = "pending_client_id", length = 512)
    private String pendingClientId;

    @Column(name = "pending_client_secret", length = 512)
    private String pendingClientSecret;

    @Column(name = "pending_token_url", length = 512)
    private String pendingTokenUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public ConnectorState() {}

    public ConnectorState(String connectorId, String displayName) {
        this.connectorId = connectorId;
        this.displayName = displayName;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // ── Getters / Setters ─────────────────────────────────────────────────

    public String getConnectorId() { return connectorId; }
    public void setConnectorId(String connectorId) { this.connectorId = connectorId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public boolean isConnected() { return connected; }
    public void setConnected(boolean connected) { this.connected = connected; }

    public String getCredentialToken() { return credentialToken; }
    public void setCredentialToken(String credentialToken) { this.credentialToken = credentialToken; }

    public int getChunksIndexed() { return chunksIndexed; }
    public void setChunksIndexed(int chunksIndexed) { this.chunksIndexed = chunksIndexed; }

    public Instant getLastSyncAt() { return lastSyncAt; }
    public void setLastSyncAt(Instant lastSyncAt) { this.lastSyncAt = lastSyncAt; }

    public String getLastSyncJobId() { return lastSyncJobId; }
    public void setLastSyncJobId(String lastSyncJobId) { this.lastSyncJobId = lastSyncJobId; }

    public String getSyncState() { return syncState; }
    public void setSyncState(String syncState) { this.syncState = syncState; }

    public String getSyncError() { return syncError; }
    public void setSyncError(String syncError) { this.syncError = syncError; }

    public int getItemsSynced() { return itemsSynced; }
    public void setItemsSynced(int itemsSynced) { this.itemsSynced = itemsSynced; }

    public Instant getOldestItemDate() { return oldestItemDate; }
    public void setOldestItemDate(Instant oldestItemDate) { this.oldestItemDate = oldestItemDate; }

    public String getPendingClientId() { return pendingClientId; }
    public void setPendingClientId(String v) { this.pendingClientId = v; }

    public String getPendingClientSecret() { return pendingClientSecret; }
    public void setPendingClientSecret(String v) { this.pendingClientSecret = v; }

    public String getPendingTokenUrl() { return pendingTokenUrl; }
    public void setPendingTokenUrl(String v) { this.pendingTokenUrl = v; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
