package org.opentron.backend.connectors;

import java.util.List;
import java.util.Map;

/**
 * Common contract every data-source connector must implement.
 *
 * The connector is responsible only for fetching raw documents from its
 * upstream service.  Chunking, embedding, and storing into memory are
 * handled by IngestService so the connector stays thin and testable.
 */
public interface DataConnector {

    /** Stable identifier that matches ConnectorState.connectorId (e.g. "notion", "gdrive"). */
    String id();

    /** Human-readable name used in logs. */
    String name();

    /**
     * Called once when the user clicks "Connect".
     * Validates the credential (token, path, OAuth code …) and returns true if valid.
     * Implementations should NOT throw — return false + set an error message instead.
     */
    boolean connect(String credentialToken);

    /**
     * Fetch all documents since the last sync.
     * Each document is a map with at minimum:
     *   "id"      – unique string within this connector
     *   "title"   – human-readable label
     *   "content" – plain text body
     *   "url"     – optional deep-link
     *   "ts"      – ISO-8601 timestamp of the source item
     *
     * @param credentialToken  stored token / path retrieved from ConnectorState
     * @param since            ISO-8601 timestamp of the last successful sync (null = full sync)
     */
    List<Map<String, Object>> fetchDocuments(String credentialToken, String since);

    /**
     * Optional: human-readable error from the last connect() or fetchDocuments() call.
     */
    default String lastError() { return null; }
}
