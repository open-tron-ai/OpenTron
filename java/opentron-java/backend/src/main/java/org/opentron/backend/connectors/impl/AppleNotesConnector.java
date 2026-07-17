package org.opentron.backend.connectors.impl;

import org.opentron.backend.connectors.DataConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.util.*;

/**
 * Apple Notes connector — reads notes from the macOS NoteStore SQLite database.
 *
 * Default location: ~/Library/Group Containers/group.com.apple.notes/NoteStore.sqlite
 * credentialToken = absolute path to NoteStore.sqlite, or empty to use the default.
 *
 * Note bodies are stored as gzipped protobuf blobs (ZDATA column).
 * We fall back to the plaintext ZSNIPPET column which Apple keeps for Spotlight,
 * avoiding any protobuf dependency.  For richer content, replace extractContent()
 * with a proper protobuf decoder once the schema is reverse-engineered.
 *
 * Requires Full Disk Access for the running process.
 */
@Component
public class AppleNotesConnector implements DataConnector {

    private static final Logger logger = LoggerFactory.getLogger(AppleNotesConnector.class);
    private static final String DEFAULT_PATH =
        System.getProperty("user.home")
        + "/Library/Group Containers/group.com.apple.notes/NoteStore.sqlite";

    private String lastError;

    @Override public String id()        { return "apple_notes"; }
    @Override public String name()      { return "Apple Notes"; }
    @Override public String lastError() { return lastError; }

    @Override
    public boolean connect(String credentialToken) {
        String path = resolvePath(credentialToken);
        if (!new File(path).exists()) {
            lastError = "NoteStore.sqlite not found at " + path
                + ". Grant Full Disk Access to this process.";
            logger.warn("[AppleNotes] {}", lastError);
            return false;
        }
        try (Connection conn = openDb(path);
             Statement st = conn.createStatement()) {
            st.executeQuery("SELECT count(*) FROM ZICNOTEDATA LIMIT 1").close();
            logger.info("[AppleNotes] Connected to {}", path);
            return true;
        } catch (Exception e) {
            lastError = "Cannot read NoteStore.sqlite: " + e.getMessage();
            logger.warn("[AppleNotes] {}", lastError);
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> fetchDocuments(String credentialToken, String since) {
        List<Map<String, Object>> docs = new ArrayList<>();
        String path = resolvePath(credentialToken);
        if (!new File(path).exists()) return docs;

        // Apple Core Data timestamps: seconds since 2001-01-01 (same epoch as iMessage)
        double sinceTs = since != null
            ? (double)(Instant.parse(since).getEpochSecond() - 978307200L) : 0.0;

        // ZICCLOUDSYNCINGOBJECT holds the note metadata.
        // ZICNOTEDATA holds the body blob; ZSNIPPET is the plain-text preview.
        String sql = """
            SELECT
                n.Z_PK,
                n.ZTITLE1          AS title,
                n.ZSNIPPET         AS snippet,
                n.ZMODIFICATIONDATE1 AS modified,
                n.ZCREATIONDATE1   AS created,
                d.ZDATA            AS body_blob
            FROM ZICCLOUDSYNCINGOBJECT n
            LEFT JOIN ZICNOTEDATA d ON d.ZNOTE = n.Z_PK
            WHERE n.ZTITLE1 IS NOT NULL
              AND n.ZMARKEDFORDELETION = 0
              AND n.ZMODIFICATIONDATE1 >= ?
            ORDER BY n.ZMODIFICATIONDATE1 DESC
            LIMIT 2000
            """;

        try (Connection conn = openDb(path);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, sinceTs);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                try {
                    String title    = rs.getString("title");
                    String snippet  = rs.getString("snippet");
                    double modified = rs.getDouble("modified");
                    double created  = rs.getDouble("created");
                    byte[] blob     = rs.getBytes("body_blob");

                    // Use decoded blob text if available, otherwise snippet
                    String content = extractContent(blob, snippet);
                    if (content == null || content.isBlank()) continue;

                    double tsD = modified > 0 ? modified : created;
                    String ts = Instant.ofEpochSecond((long) tsD + 978307200L).toString();

                    Map<String, Object> doc = new HashMap<>();
                    doc.put("id",      "applenotes:" + rs.getInt("Z_PK"));
                    doc.put("title",   title != null ? title : "Untitled");
                    doc.put("content", (title != null ? title + "\n\n" : "") + content);
                    doc.put("url",     "");
                    doc.put("ts",      ts);
                    docs.add(doc);
                } catch (Exception e) {
                    logger.debug("[AppleNotes] Skipping row: {}", e.getMessage());
                }
            }
            logger.info("[AppleNotes] Fetched {} notes", docs.size());
        } catch (Exception e) {
            lastError = e.getMessage();
            logger.error("[AppleNotes] fetchDocuments failed", e);
        }
        return docs;
    }

    /**
     * Attempts to extract readable text from the gzip+protobuf ZDATA blob.
     * Falls back to the ZSNIPPET plain-text preview when decoding is not possible.
     *
     * The NoteStore protobuf schema is undocumented. We do a best-effort extraction:
     * gunzip the blob and pull out every UTF-8 string field we can find by scanning
     * for length-prefixed strings (protobuf field wire type 2).
     */
    private String extractContent(byte[] blob, String fallback) {
        if (blob == null || blob.length == 0) return fallback;
        try {
            // NoteStore body blobs start with a custom 4-byte header before the gzip magic.
            // Skip bytes until we hit the gzip magic 0x1F 0x8B.
            int gzipStart = -1;
            for (int i = 0; i < Math.min(blob.length - 1, 16); i++) {
                if ((blob[i] & 0xFF) == 0x1F && (blob[i + 1] & 0xFF) == 0x8B) {
                    gzipStart = i;
                    break;
                }
            }
            if (gzipStart < 0) return fallback;

            byte[] gzipped = Arrays.copyOfRange(blob, gzipStart, blob.length);
            byte[] raw;
            try (java.util.zip.GZIPInputStream gz =
                     new java.util.zip.GZIPInputStream(new java.io.ByteArrayInputStream(gzipped))) {
                raw = gz.readAllBytes();
            }

            // Scan the decompressed protobuf bytes for UTF-8 string fields (wire type 2).
            // We collect any string >= 10 chars that look like natural language.
            StringBuilder sb = new StringBuilder();
            int i = 0;
            while (i < raw.length - 2) {
                // Skip the field tag byte
                i++;
                // Try reading a varint length
                if ((raw[i] & 0x80) == 0) {
                    int len = raw[i] & 0x7F;
                    i++;
                    if (len >= 10 && i + len <= raw.length) {
                        String candidate = new String(raw, i, len, java.nio.charset.StandardCharsets.UTF_8);
                        // Filter: keep strings that are mostly printable ASCII / Unicode text
                        long printable = candidate.chars()
                            .filter(c -> c >= 0x20 && c != 0xFFFD).count();
                        if (printable > len * 0.80) {
                            if (sb.length() > 0) sb.append("\n");
                            sb.append(candidate.trim());
                        }
                        i += len;
                        continue;
                    }
                }
                // Fallback — advance one byte
            }

            String result = sb.toString().trim();
            return result.length() >= 20 ? result : (fallback != null ? fallback : result);
        } catch (Exception e) {
            return fallback;
        }
    }

    private String resolvePath(String credentialToken) {
        return (credentialToken == null || credentialToken.isBlank())
            ? DEFAULT_PATH : credentialToken.trim();
    }

    private Connection openDb(String path) throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + path + "?open_mode=1");
    }
}
