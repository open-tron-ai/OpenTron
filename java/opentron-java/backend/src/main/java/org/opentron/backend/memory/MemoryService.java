package org.opentron.backend.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class MemoryService {

    private final ConcurrentHashMap<String, MemoryEntry> store = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Path filePath;

    public MemoryService() {
        this("memory.jsonl");
    }

    public MemoryService(String filePath) {
        this.filePath = Path.of(filePath);
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        try {
            if (Files.exists(this.filePath)) {
                Files.lines(this.filePath).forEach(line -> {
                    try {
                        MemoryEntry e = mapper.readValue(line, MemoryEntry.class);
                        store.put(e.getId(), e);
                    } catch (IOException ex) {
                        System.err.println("Failed to parse memory line: " + ex.getMessage());
                    }
                });
            }
        } catch (IOException e) {
            System.err.println("Failed to read memory file: " + e.getMessage());
        }
    }

    public MemoryEntry store(MemoryStoreRequest req) {
        String id = UUID.randomUUID().toString();
        MemoryEntry e = new MemoryEntry(id, req.getText(), req.getMetadata());
        store.put(id, e);
        persist(e);
        return e;
    }

    public List<MemoryEntry> search(MemorySearchRequest req) {
        String q = req.getQuery() == null ? "" : req.getQuery().toLowerCase();
        return store.values().stream()
                .filter(e -> e.getText() != null && e.getText().toLowerCase().contains(q))
                .limit(req.getLimit())
                .collect(Collectors.toList());
    }

    public int count() {
        return store.size();
    }

    public Map<String, Object> config() {
        return Map.of("engine", "memory-inproc", "version", "0.1");
    }

    public Map<String, Object> index(MemoryStoreRequest req) {
        // no-op: return simple status
        return Map.of("status", "ok");
    }

    private synchronized void persist(MemoryEntry e) {
        try {
            String json = mapper.writeValueAsString(e);
            Files.writeString(this.filePath, json + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            System.err.println("Failed to persist memory entry: " + ex.getMessage());
        }
    }
}
