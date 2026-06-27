package org.opentron.backend.traces;

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
public class TraceService {

    private final ConcurrentHashMap<String, TraceEntry> store = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Path filePath;

    public TraceService() {
        this("traces.jsonl");
    }

    public TraceService(String filePath) {
        this.filePath = Path.of(filePath);
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        try {
            if (Files.exists(this.filePath)) {
                Files.lines(this.filePath).forEach(line -> {
                    try {
                        TraceEntry e = mapper.readValue(line, TraceEntry.class);
                        store.put(e.getId(), e);
                    } catch (IOException ex) {
                        System.err.println("Failed to parse trace line: " + ex.getMessage());
                    }
                });
            }
        } catch (IOException e) {
            System.err.println("Failed to read traces file: " + e.getMessage());
        }
    }

    public TraceEntry addTrace(String kind, Map<String, Object> payload) {
        String id = UUID.randomUUID().toString();
        TraceEntry e = new TraceEntry(id, kind, payload);
        store.put(id, e);
        persist(e);
        return e;
    }

    public TraceEntry updateTrace(String id, Map<String, Object> updates) {
        TraceEntry existing = store.get(id);
        if (existing == null) {
            return null;
        }
        if (updates != null && !updates.isEmpty()) {
            existing.getPayload().putAll(updates);
            persistAll();
        }
        return existing;
    }

    public List<TraceEntry> listTraces(int limit) {
        return store.values().stream().limit(limit).collect(Collectors.toList());
    }

    public TraceEntry getTrace(String id) {
        return store.get(id);
    }

    public int count() {
        return store.size();
    }

    private synchronized void persist(TraceEntry e) {
        try {
            String json = mapper.writeValueAsString(e);
            Files.writeString(this.filePath, json + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            System.err.println("Failed to persist trace: " + ex.getMessage());
        }
    }

    private synchronized void persistAll() {
        try {
            StringBuilder content = new StringBuilder();
            for (TraceEntry entry : store.values()) {
                content.append(mapper.writeValueAsString(entry)).append(System.lineSeparator());
            }
            Files.writeString(this.filePath, content.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
            System.err.println("Failed to persist traces: " + ex.getMessage());
        }
    }
}
