package org.opentron.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/v1/connectors")
public class ConnectorsController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> listConnectors() {
        List<Map<String, Object>> connectors = new ArrayList<>();
        
        String[] sources = {
            "Gmail IMAP", "Slack", "Outlook", "Google Drive", 
            "Google Calendar", "Google Contacts", "Dropbox", 
            "Notion", "Obsidian", "Granola", "WhatsApp"
        };
        
        for (String source : sources) {
            Map<String, Object> connector = new HashMap<>();
            connector.put("connector_id", source.toLowerCase().replace(" ", "-"));
            connector.put("display_name", source);
            connector.put("connected", Math.random() > 0.5);
            connector.put("chunks", (int)(Math.random() * 1000));
            connectors.add(connector);
        }
        
        return ResponseEntity.ok(Map.of("connectors", connectors));
    }

    @PostMapping("/{connectorId}/connect")
    public ResponseEntity<Map<String, Object>> connectConnector(@PathVariable String connectorId) {
        return ResponseEntity.ok(Map.of(
            "status", "connected",
            "connector_id", connectorId,
            "timestamp", System.currentTimeMillis()
        ));
    }

    @PostMapping("/{connectorId}/disconnect")
    public ResponseEntity<Map<String, Object>> disconnectConnector(@PathVariable String connectorId) {
        return ResponseEntity.ok(Map.of(
            "status", "disconnected",
            "connector_id", connectorId
        ));
    }
}
