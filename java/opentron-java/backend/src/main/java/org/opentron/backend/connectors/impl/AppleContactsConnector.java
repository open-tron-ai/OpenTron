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
 * Apple Contacts connector — reads from the macOS AddressBook SQLite database.
 *
 * Default location: ~/Library/Application Support/AddressBook/Sources/<uuid>/AddressBook-v22.abcddb
 * We scan the entire ~/Library/Application Support/AddressBook tree for the first
 * .abcddb file we find.
 *
 * credentialToken = absolute path to AddressBook-v22.abcddb, or empty to auto-discover.
 *
 * Requires Full Disk Access for the running process.
 */
@Component
public class AppleContactsConnector implements DataConnector {

    private static final Logger logger = LoggerFactory.getLogger(AppleContactsConnector.class);
    private static final String AB_ROOT =
        System.getProperty("user.home") + "/Library/Application Support/AddressBook";

    private String lastError;

    @Override public String id()        { return "apple_contacts"; }
    @Override public String name()      { return "Apple Contacts"; }
    @Override public String lastError() { return lastError; }

    @Override
    public boolean connect(String credentialToken) {
        String path = resolvePath(credentialToken);
        if (path == null) {
            lastError = "AddressBook database not found under " + AB_ROOT
                + ". Grant Full Disk Access to this process.";
            logger.warn("[AppleContacts] {}", lastError);
            return false;
        }
        try (Connection conn = openDb(path);
             Statement st = conn.createStatement()) {
            st.executeQuery("SELECT count(*) FROM ZABCDRECORD LIMIT 1").close();
            logger.info("[AppleContacts] Connected to {}", path);
            return true;
        } catch (Exception e) {
            lastError = "Cannot read AddressBook database: " + e.getMessage();
            logger.warn("[AppleContacts] {}", lastError);
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> fetchDocuments(String credentialToken, String since) {
        List<Map<String, Object>> docs = new ArrayList<>();
        String path = resolvePath(credentialToken);
        if (path == null) return docs;

        // Core Data timestamp: seconds since 2001-01-01
        double sinceTs = since != null
            ? (double)(Instant.parse(since).getEpochSecond() - 978307200L) : 0.0;

        /*
         * ZABCDRECORD  — person/company record
         * ZABCDEMAILADDRESS  — email
         * ZABCDPHONENUMBER   — phone
         * ZABCDPOSTALADDRESS — address
         * ZABCDNOTE          — freeform note
         * ZABCDORGANIZATION  — org name (on the record directly as ZORGANIZATION)
         */
        String contactSql = """
            SELECT
                r.Z_PK,
                r.ZFIRSTNAME,
                r.ZLASTNAME,
                r.ZORGANIZATION,
                r.ZNICKNAME,
                r.ZJOBTITLE,
                r.ZTHUMBNAILIMAGEDATA,
                r.ZMODIFICATIONDATE
            FROM ZABCDRECORD r
            WHERE r.ZMODIFICATIONDATE >= ?
              AND (r.ZFIRSTNAME IS NOT NULL OR r.ZLASTNAME IS NOT NULL OR r.ZORGANIZATION IS NOT NULL)
            ORDER BY r.ZMODIFICATIONDATE DESC
            LIMIT 5000
            """;

        String emailSql  = "SELECT ZADDRESSNORMALIZED FROM ZABCDEMAILADDRESS WHERE ZOWNER = ?";
        String phoneSql  = "SELECT ZFULLNUMBER FROM ZABCDPHONENUMBER WHERE ZOWNER = ?";
        String noteSql   = "SELECT ZTEXT FROM ZABCDNOTE WHERE ZOWNER = ?";

        try (Connection conn = openDb(path);
             PreparedStatement ps   = conn.prepareStatement(contactSql);
             PreparedStatement eps  = conn.prepareStatement(emailSql);
             PreparedStatement pps  = conn.prepareStatement(phoneSql);
             PreparedStatement nps  = conn.prepareStatement(noteSql)) {

            ps.setDouble(1, sinceTs);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                try {
                    int    pk        = rs.getInt("Z_PK");
                    String first     = nvl(rs.getString("ZFIRSTNAME"));
                    String last      = nvl(rs.getString("ZLASTNAME"));
                    String org       = nvl(rs.getString("ZORGANIZATION"));
                    String nickname  = nvl(rs.getString("ZNICKNAME"));
                    String jobTitle  = nvl(rs.getString("ZJOBTITLE"));
                    double modified  = rs.getDouble("ZMODIFICATIONDATE");

                    String fullName = (first + " " + last).trim();
                    if (fullName.isBlank()) fullName = org.isBlank() ? "Unknown" : org;

                    // emails
                    eps.setInt(1, pk);
                    List<String> emails = collectStrings(eps.executeQuery(), "ZADDRESSNORMALIZED");

                    // phones
                    pps.setInt(1, pk);
                    List<String> phones = collectStrings(pps.executeQuery(), "ZFULLNUMBER");

                    // notes
                    nps.setInt(1, pk);
                    List<String> notes = collectStrings(nps.executeQuery(), "ZTEXT");

                    StringBuilder content = new StringBuilder("Name: ").append(fullName);
                    if (!org.isBlank())      content.append("\nOrganization: ").append(org);
                    if (!jobTitle.isBlank()) content.append("\nTitle: ").append(jobTitle);
                    if (!nickname.isBlank()) content.append("\nNickname: ").append(nickname);
                    if (!emails.isEmpty())   content.append("\nEmail: ").append(String.join(", ", emails));
                    if (!phones.isEmpty())   content.append("\nPhone: ").append(String.join(", ", phones));
                    if (!notes.isEmpty())    content.append("\nNotes: ").append(String.join(" | ", notes));

                    String ts = Instant.ofEpochSecond((long) modified + 978307200L).toString();

                    Map<String, Object> doc = new HashMap<>();
                    doc.put("id",      "applecontacts:" + pk);
                    doc.put("title",   fullName);
                    doc.put("content", content.toString());
                    doc.put("url",     "");
                    doc.put("ts",      ts);
                    docs.add(doc);
                } catch (Exception e) {
                    logger.debug("[AppleContacts] Skipping row: {}", e.getMessage());
                }
            }
            logger.info("[AppleContacts] Fetched {} contacts", docs.size());
        } catch (Exception e) {
            lastError = e.getMessage();
            logger.error("[AppleContacts] fetchDocuments failed", e);
        }
        return docs;
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private List<String> collectStrings(ResultSet rs, String col) throws SQLException {
        List<String> out = new ArrayList<>();
        while (rs.next()) {
            String v = rs.getString(col);
            if (v != null && !v.isBlank()) out.add(v.trim());
        }
        rs.close();
        return out;
    }

    private String nvl(String s) { return s != null ? s.trim() : ""; }

    /** Resolve the database path, auto-discovering under AB_ROOT if needed. */
    private String resolvePath(String credentialToken) {
        if (credentialToken != null && !credentialToken.isBlank()) {
            return new File(credentialToken.trim()).exists() ? credentialToken.trim() : null;
        }
        // Scan Sources/<uuid>/*.abcddb
        File root = new File(AB_ROOT);
        if (!root.exists()) return null;
        try {
            return java.nio.file.Files.walk(root.toPath(), 3)
                .filter(p -> p.toString().endsWith(".abcddb"))
                .map(p -> p.toString())
                .findFirst()
                .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private Connection openDb(String path) throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + path + "?open_mode=1");
    }
}
