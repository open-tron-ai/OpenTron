package org.opentron.backend.storage.service;

import org.opentron.backend.connectors.ConnectorRegistry;
import org.opentron.backend.connectors.DataConnector;
import org.opentron.backend.memory.MemoryService;
import org.opentron.backend.memory.MemoryStoreRequest;
import org.opentron.backend.services.IngestService;
import org.opentron.backend.storage.entities.ConnectorState;
import org.opentron.backend.storage.repositories.ConnectorStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Single source of truth for connector lifecycle:
 *   connect → validate credential → persist ConnectorState
 *   sync    → delegate to DataConnector.fetchDocuments → chunk → MemoryService
 *   status  → read ConnectorState + last IngestJob
 *   disconnect → clear ConnectorState
 *
 * All state is persisted in the connector_states table so it survives restarts.
 */
@Service
public class ConnectorStateService {

    private static final Logger logger = LoggerFactory.getLogger(ConnectorStateService.class);

    /** All known connector IDs in display order — drives listConnectors(). */
    private static final List<String> KNOWN_IDS = List.of(
        "gmail_imap", "slack", "outlook", "gdrive", "gcalendar",
        "gcontacts", "dropbox", "notion", "obsidian", "granola",
        "whatsapp", "imessage", "apple_notes", "apple_contacts"
    );

    private static final Map<String, String> DISPLAY_NAMES = Map.ofEntries(
        Map.entry("gmail_imap",      "Gmail"),
        Map.entry("slack",           "Slack"),
        Map.entry("outlook",         "Outlook"),
        Map.entry("gdrive",          "Google Drive"),
        Map.entry("gcalendar",       "Google Calendar"),
        Map.entry("gcontacts",       "Google Contacts"),
        Map.entry("dropbox",         "Dropbox"),
        Map.entry("notion",          "Notion"),
        Map.entry("obsidian",        "Obsidian"),
        Map.entry("granola",         "Granola"),
        Map.entry("whatsapp",        "WhatsApp"),
        Map.entry("imessage",        "iMessage"),
        Map.entry("apple_notes",     "Apple Notes"),
        Map.entry("apple_contacts",  "Apple Contacts")
    );

    private final ConnectorStateRepository repo;
    private final ConnectorRegistry registry;
    private final IngestService ingestService;
    private final MemoryService memoryService;

    public ConnectorStateService(ConnectorStateRepository repo,
                                 ConnectorRegistry registry,
                                 IngestService ingestService,
                                 MemoryService memoryService) {
        this.repo = repo;
        this.registry = registry;
        this.ingestService = ingestService;
        this.memoryService = memoryService;
    }

    // ── List ──────────────────────────────────────────────────────────────

    /**
     * Returns all known connectors with their current state.
     * Connectors not yet in the DB are returned as disconnected.
     */
    public List<Map<String, Object>> listConnectors() {
        Map<String, ConnectorState> byId = new HashMap<>();
        repo.findAll().forEach(s -> byId.put(s.getConnectorId(), s));

        List<Map<String, Object>> result = new ArrayList<>();
        for (String id : KNOWN_IDS) {
            ConnectorState state = byId.get(id);
            Map<String, Object> row = new HashMap<>();
            row.put("connector_id", id);
            row.put("display_name", DISPLAY_NAMES.getOrDefault(id, id));
            row.put("connected", state != null && state.isConnected());
            row.put("chunks", state != null ? state.getChunksIndexed() : 0);
            row.put("auth_type", "oauth");
            result.add(row);
        }
        return result;
    }

    // ── Connect ───────────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> connect(String connectorId, String credentialToken) {
        Optional<DataConnector> opt = registry.get(connectorId);
        if (opt.isEmpty()) {
            return Map.of("connected", false, "status", "error",
                          "detail", "Unknown connector: " + connectorId);
        }

        DataConnector connector = opt.get();
        boolean ok = connector.connect(credentialToken);
        if (!ok) {
            String err = connector.lastError() != null ? connector.lastError() : "Credential validation failed";
            return Map.of("connected", false, "status", "error", "detail", err);
        }

        ConnectorState state = repo.findById(connectorId)
                .orElse(new ConnectorState(connectorId, DISPLAY_NAMES.getOrDefault(connectorId, connectorId)));
        state.setConnected(true);
        state.setCredentialToken(credentialToken);
        state.setSyncState("idle");
        state.setSyncError(null);
        repo.save(state);

        logger.info("[ConnectorStateService] {} connected", connectorId);
        return Map.of("connector_id", connectorId, "connected", true, "status", "connected");
    }

    // ── Disconnect ────────────────────────────────────────────────────────

    @Transactional
    public void disconnect(String connectorId) {
        repo.findById(connectorId).ifPresent(state -> {
            state.setConnected(false);
            state.setCredentialToken(null);
            state.setSyncState("idle");
            state.setSyncError(null);
            repo.save(state);
            logger.info("[ConnectorStateService] {} disconnected", connectorId);
        });
    }

    // ── Sync status ───────────────────────────────────────────────────────

    public Map<String, Object> getSyncStatus(String connectorId) {
        Optional<ConnectorState> opt = repo.findById(connectorId);
        Map<String, Object> status = new HashMap<>();

        if (opt.isEmpty()) {
            status.put("state", "idle");
            status.put("items_synced", 0);
            status.put("items_total", 0);
            status.put("last_sync", null);
            status.put("error", null);
            status.put("oldest_item_date", null);
            status.put("new_items_synced", null);
            return status;
        }

        ConnectorState s = opt.get();
        // If a sync job is in flight, derive state from IngestJob
        String syncState = s.getSyncState();
        if ("syncing".equals(syncState) && s.getLastSyncJobId() != null) {
            String jobState = ingestService.getJobState(s.getLastSyncJobId());
            if ("completed".equals(jobState) || "failed".equals(jobState)) {
                // Job finished — update the ConnectorState
                Map<String, Object> details = ingestService.getJobDetails(s.getLastSyncJobId());
                int items = details.get("items_processed") instanceof Number n ? n.intValue() : 0;
                s.setItemsSynced(s.getItemsSynced() + items);
                s.setChunksIndexed(s.getChunksIndexed() + (details.get("chunks_created") instanceof Number n ? n.intValue() : 0));
                s.setLastSyncAt(Instant.now());
                s.setSyncState("failed".equals(jobState) ? "error" : "idle");
                if ("failed".equals(jobState)) {
                    s.setSyncError(details.getOrDefault("message", "Sync failed").toString());
                } else {
                    s.setSyncError(null);
                }
                repo.save(s);
                syncState = s.getSyncState();
            }
        }

        status.put("state", syncState);
        status.put("items_synced", s.getItemsSynced());
        status.put("items_total", 0);
        status.put("last_sync", s.getLastSyncAt() != null ? s.getLastSyncAt().toString() : null);
        status.put("error", s.getSyncError());
        status.put("oldest_item_date", s.getOldestItemDate() != null ? s.getOldestItemDate().toString() : null);
        status.put("new_items_synced", null);
        return status;
    }

    // ── Trigger sync ──────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> triggerSync(String connectorId) {
        Optional<ConnectorState> opt = repo.findById(connectorId);
        if (opt.isEmpty() || !opt.get().isConnected()) {
            return Map.of("status", "error", "detail", "Connector not connected: " + connectorId);
        }

        ConnectorState state = opt.get();
        if ("syncing".equals(state.getSyncState())) {
            return Map.of("connector_id", connectorId, "status", "syncing",
                          "chunks_indexed", state.getChunksIndexed(),
                          "detail", "Sync already in progress");
        }

        Optional<DataConnector> connOpt = registry.get(connectorId);
        if (connOpt.isEmpty()) {
            return Map.of("status", "error", "detail", "No connector implementation for: " + connectorId);
        }

        String jobId = UUID.randomUUID().toString();
        ingestService.createJob(jobId, connectorId);

        state.setLastSyncJobId(jobId);
        state.setSyncState("syncing");
        state.setSyncError(null);
        repo.save(state);

        // Run the actual fetch+ingest asynchronously
        String credential = state.getCredentialToken();
        String since = state.getLastSyncAt() != null ? state.getLastSyncAt().toString() : null;
        DataConnector connector = connOpt.get();

        ingestService.processIngestWithConnector(jobId, connectorId, () -> {
            List<Map<String, Object>> docs = connector.fetchDocuments(credential, since);
            for (Map<String, Object> doc : docs) {
                String content = String.valueOf(doc.getOrDefault("content", ""));
                String title   = String.valueOf(doc.getOrDefault("title", ""));
                if (!content.isBlank()) {
                    MemoryStoreRequest req = new MemoryStoreRequest();
                    req.setText("[" + connectorId + "] " + title + "\n" + content);
                    req.setMetadata(Map.of(
                        "source", connectorId,
                        "doc_id", String.valueOf(doc.getOrDefault("id", "")),
                        "url",    String.valueOf(doc.getOrDefault("url", "")),
                        "ts",     String.valueOf(doc.getOrDefault("ts", ""))
                    ));
                    memoryService.store(req);
                }
            }
            return docs.size();
        });

        logger.info("[ConnectorStateService] Sync triggered for {} (jobId={})", connectorId, jobId);
        return Map.of("connector_id", connectorId, "status", "syncing",
                      "job_id", jobId, "chunks_indexed", state.getChunksIndexed());
    }

    // ── Scheduled: refresh syncing state every 30 s ───────────────────────

    @Scheduled(fixedDelay = 30_000)
    public void refreshSyncingConnectors() {
        repo.findActivelySyncing().forEach(s -> getSyncStatus(s.getConnectorId()));
    }
}
