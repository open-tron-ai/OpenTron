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
 * iMessage connector — reads macOS Messages from the local SQLite chat.db.
 * credentialToken = absolute path to chat.db, or empty to use the default
 *                   ~/Library/Messages/chat.db location.
 *
 * Requires Full Disk Access for the process (Terminal / the app itself).
 */
@Component
public class IMessageConnector implements DataConnector {

    private static final Logger logger = LoggerFactory.getLogger(IMessageConnector.class);
    private static final String DEFAULT_PATH =
        System.getProperty("user.home") + "/Library/Messages/chat.db";
    private static final int MAX_MESSAGES = 2000;

    private String lastError;

    @Override public String id()        { return "imessage"; }
    @Override public String name()      { return "iMessage"; }
    @Override public String lastError() { return lastError; }

    @Override
    public boolean connect(String credentialToken) {
        String path = resolvePath(credentialToken);
        if (!new File(path).exists()) {
            lastError = "chat.db not found at " + path + ". Grant Full Disk Access to this process.";
            logger.warn("[iMessage] {}", lastError);
            return false;
        }
        try (Connection conn = openDb(path);
             Statement st = conn.createStatement()) {
            st.executeQuery("SELECT count(*) FROM message LIMIT 1").close();
            logger.info("[iMessage] Connected to {}", path);
            return true;
        } catch (Exception e) {
            lastError = "Cannot read chat.db: " + e.getMessage();
            logger.warn("[iMessage] {}", lastError);
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> fetchDocuments(String credentialToken, String since) {
        List<Map<String, Object>> docs = new ArrayList<>();
        String path = resolvePath(credentialToken);

        if (!new File(path).exists()) return docs;

        // iMessage stores timestamps as nanoseconds since 2001-01-01
        // epoch offset: 978307200 seconds
        long sinceNano = since != null
            ? (Instant.parse(since).getEpochSecond() - 978307200L) * 1_000_000_000L
            : 0L;

        String sql = """
            SELECT
                m.rowid,
                m.text,
                m.date,
                m.is_from_me,
                h.id   AS handle,
                c.chat_identifier
            FROM message m
            LEFT JOIN handle h       ON h.rowid = m.handle_id
            LEFT JOIN chat_message_join cmj ON cmj.message_id = m.rowid
            LEFT JOIN chat c         ON c.rowid = cmj.chat_id
            WHERE m.text IS NOT NULL
              AND m.text != ''
              AND m.date >= ?
            ORDER BY m.date DESC
            LIMIT ?
            """;

        try (Connection conn = openDb(path);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, sinceNano);
            ps.setInt(2, MAX_MESSAGES);
            ResultSet rs = ps.executeQuery();

            // Group by chat_identifier for context
            Map<String, List<String>> byChat = new LinkedHashMap<>();
            Map<String, String> chatTs = new HashMap<>();

            while (rs.next()) {
                String text    = rs.getString("text");
                long   dateNs  = rs.getLong("date");
                String handle  = rs.getString("handle");
                String chatId  = rs.getString("chat_identifier");
                boolean fromMe = rs.getInt("is_from_me") == 1;

                if (chatId == null) chatId = handle != null ? handle : "unknown";

                long epochSec = (dateNs / 1_000_000_000L) + 978307200L;
                String ts = Instant.ofEpochSecond(epochSec).toString();

                String speaker = fromMe ? "Me" : (handle != null ? handle : "them");
                byChat.computeIfAbsent(chatId, k -> new ArrayList<>())
                      .add(speaker + ": " + text);
                chatTs.putIfAbsent(chatId, ts);
            }

            for (Map.Entry<String, List<String>> e : byChat.entrySet()) {
                String chatId = e.getKey();
                List<String> messages = e.getValue();
                // Reverse so oldest appears first
                Collections.reverse(messages);
                String content = String.join("\n", messages);

                Map<String, Object> doc = new HashMap<>();
                doc.put("id",      "imessage:" + chatId);
                doc.put("title",   "iMessage with " + chatId);
                doc.put("content", content);
                doc.put("url",     "");
                doc.put("ts",      chatTs.getOrDefault(chatId, Instant.now().toString()));
                docs.add(doc);
            }

            logger.info("[iMessage] Fetched {} conversations", docs.size());
        } catch (Exception e) {
            lastError = e.getMessage();
            logger.error("[iMessage] fetchDocuments failed", e);
        }
        return docs;
    }

    private String resolvePath(String credentialToken) {
        return (credentialToken == null || credentialToken.isBlank())
            ? DEFAULT_PATH : credentialToken.trim();
    }

    private Connection openDb(String path) throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + path + "?open_mode=1"); // read-only
    }
}
