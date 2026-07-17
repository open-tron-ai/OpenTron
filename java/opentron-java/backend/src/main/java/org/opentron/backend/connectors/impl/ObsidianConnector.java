package org.opentron.backend.connectors.impl;

import org.opentron.backend.connectors.DataConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Obsidian vault connector — reads markdown files from the local path
 * stored as the credentialToken.
 *
 * Full implementation: walk the vault directory with Files.walk(),
 * read each .md file, strip YAML front-matter, return as documents.
 * The stub below validates the path and returns an empty list so the
 * connect / sync lifecycle works end-to-end immediately.
 */
@Component
public class ObsidianConnector implements DataConnector {

    private static final Logger logger = LoggerFactory.getLogger(ObsidianConnector.class);
    private String lastError;

    @Override public String id() { return "obsidian"; }
    @Override public String name() { return "Obsidian"; }
    @Override public String lastError() { return lastError; }

    @Override
    public boolean connect(String credentialToken) {
        if (credentialToken == null || credentialToken.isBlank()) {
            lastError = "Vault path is required";
            return false;
        }
        java.io.File dir = new java.io.File(credentialToken.trim());
        if (!dir.exists() || !dir.isDirectory()) {
            lastError = "Path does not exist or is not a directory: " + credentialToken;
            logger.warn("[ObsidianConnector] {}", lastError);
            return false;
        }
        logger.info("[ObsidianConnector] Connected to vault: {}", credentialToken);
        return true;
    }

    @Override
    public List<Map<String, Object>> fetchDocuments(String credentialToken, String since) {
        List<Map<String, Object>> docs = new ArrayList<>();
        if (credentialToken == null || credentialToken.isBlank()) return docs;

        java.io.File vault = new java.io.File(credentialToken.trim());
        if (!vault.exists()) return docs;

        try {
            java.nio.file.Files.walk(vault.toPath())
                .filter(p -> p.toString().endsWith(".md"))
                .forEach(p -> {
                    try {
                        String content = java.nio.file.Files.readString(p);
                        String title = p.getFileName().toString().replace(".md", "");
                        String ts = java.nio.file.Files.getLastModifiedTime(p)
                                .toInstant().toString();
                        docs.add(Map.of(
                            "id", p.toString(),
                            "title", title,
                            "content", content,
                            "url", p.toUri().toString(),
                            "ts", ts
                        ));
                    } catch (Exception e) {
                        logger.warn("[ObsidianConnector] Could not read {}: {}", p, e.getMessage());
                    }
                });
        } catch (Exception e) {
            lastError = e.getMessage();
            logger.error("[ObsidianConnector] fetchDocuments failed", e);
        }
        logger.info("[ObsidianConnector] Fetched {} documents from {}", docs.size(), credentialToken);
        return docs;
    }
}
