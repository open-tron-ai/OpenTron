package io.opentron.cli.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Telemetry collection and storage for OpenTron CLI.
 * Tracks usage metrics, performance data, and system health.
 */
public class TelemetryStore {
    private static final String TELEMETRY_DB = DataManager.getConfigDir() + "/telemetry.db";
    private List<TelemetryEvent> events;
    private boolean enabled;

    public TelemetryStore() throws IOException {
        DataManager.initializeDirectories();
        this.events = new ArrayList<>();
        this.enabled = true;
        loadTelemetry();
    }

    /**
     * Load telemetry from storage.
     */
    private void loadTelemetry() throws IOException {
        Path telemetryFile = Paths.get(TELEMETRY_DB);
        
        if (Files.exists(telemetryFile)) {
            String json = new String(Files.readAllBytes(telemetryFile), StandardCharsets.UTF_8);
            events = parseTelemetryJson(json);
        }
    }

    /**
     * Record a telemetry event.
     */
    public void recordEvent(String command, String model, String engine, 
                           long durationMs, int tokens, boolean success) throws IOException {
        if (!enabled) return;
        
        TelemetryEvent event = new TelemetryEvent();
        event.id = UUID.randomUUID().toString();
        event.timestamp = System.currentTimeMillis();
        event.command = command;
        event.model = model;
        event.engine = engine;
        event.duration_ms = durationMs;
        event.tokens = tokens;
        event.success = success;
        
        events.add(event);
        
        // Keep only last 10000 events to prevent unbounded growth
        if (events.size() > 10000) {
            events.remove(0);
        }
        
        save();
    }

    /**
     * Save telemetry to storage.
     */
    public void save() throws IOException {
        JsonArray json = new JsonArray();
        
        for (TelemetryEvent event : events) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", event.id);
            obj.addProperty("timestamp", event.timestamp);
            obj.addProperty("command", event.command);
            obj.addProperty("model", event.model);
            obj.addProperty("engine", event.engine);
            obj.addProperty("duration_ms", event.duration_ms);
            obj.addProperty("tokens", event.tokens);
            obj.addProperty("success", event.success);
            json.add(obj);
        }
        
        DataManager.saveJson(Paths.get(TELEMETRY_DB), json.toString());
    }

    /**
     * Get telemetry summary statistics.
     */
    public TelemetrySummary getSummary() {
        TelemetrySummary summary = new TelemetrySummary();
        
        if (events.isEmpty()) return summary;
        
        summary.total_queries = events.size();
        summary.successful_queries = (int) events.stream().filter(e -> e.success).count();
        summary.failed_queries = summary.total_queries - summary.successful_queries;
        
        double totalDuration = events.stream().mapToLong(e -> e.duration_ms).sum();
        summary.average_latency_ms = totalDuration / summary.total_queries;
        summary.total_tokens = events.stream().mapToInt(e -> e.tokens).sum();
        
        if (summary.total_tokens > 0) {
            summary.throughput_tokens_per_sec = (summary.total_tokens * 1000.0) / totalDuration;
        }
        
        // Get most used model
        Map<String, Integer> modelCounts = new HashMap<>();
        for (TelemetryEvent event : events) {
            modelCounts.put(event.model, modelCounts.getOrDefault(event.model, 0) + 1);
        }
        if (!modelCounts.isEmpty()) {
            summary.top_model = Collections.max(modelCounts.entrySet(), 
                Map.Entry.comparingByValue()).getKey();
        }
        
        // Get most used engine
        Map<String, Integer> engineCounts = new HashMap<>();
        for (TelemetryEvent event : events) {
            engineCounts.put(event.engine, engineCounts.getOrDefault(event.engine, 0) + 1);
        }
        if (!engineCounts.isEmpty()) {
            summary.top_engine = Collections.max(engineCounts.entrySet(), 
                Map.Entry.comparingByValue()).getKey();
        }
        
        return summary;
    }

    /**
     * Query events by time range.
     */
    public List<TelemetryEvent> queryByTimeRange(long startTime, long endTime) {
        List<TelemetryEvent> results = new ArrayList<>();
        
        for (TelemetryEvent event : events) {
            if (event.timestamp >= startTime && event.timestamp <= endTime) {
                results.add(event);
            }
        }
        
        return results;
    }

    /**
     * Query events by command.
     */
    public List<TelemetryEvent> queryByCommand(String command) {
        List<TelemetryEvent> results = new ArrayList<>();
        
        for (TelemetryEvent event : events) {
            if (event.command.equalsIgnoreCase(command)) {
                results.add(event);
            }
        }
        
        return results;
    }

    /**
     * Query events by model.
     */
    public List<TelemetryEvent> queryByModel(String model) {
        List<TelemetryEvent> results = new ArrayList<>();
        
        for (TelemetryEvent event : events) {
            if (event.model.equalsIgnoreCase(model)) {
                results.add(event);
            }
        }
        
        return results;
    }

    /**
     * Clear all telemetry.
     */
    public void clear() throws IOException {
        events.clear();
        save();
    }

    /**
     * Export telemetry as CSV.
     */
    public String exportCsv() {
        StringBuilder csv = new StringBuilder();
        csv.append("Timestamp,Command,Model,Engine,Duration(ms),Tokens,Success\n");
        
        for (TelemetryEvent event : events) {
            csv.append(String.format("%d,%s,%s,%s,%d,%d,%s\n",
                event.timestamp, event.command, event.model, event.engine,
                event.duration_ms, event.tokens, event.success));
        }
        
        return csv.toString();
    }

    /**
     * Export telemetry as JSON.
     */
    public String exportJson() {
        JsonArray json = new JsonArray();
        
        for (TelemetryEvent event : events) {
            JsonObject obj = new JsonObject();
            obj.addProperty("timestamp", event.timestamp);
            obj.addProperty("command", event.command);
            obj.addProperty("model", event.model);
            obj.addProperty("engine", event.engine);
            obj.addProperty("duration_ms", event.duration_ms);
            obj.addProperty("tokens", event.tokens);
            obj.addProperty("success", event.success);
            json.add(obj);
        }
        
        return json.toString();
    }

    /**
     * Enable/disable telemetry collection.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Get event count.
     */
    public int getEventCount() {
        return events.size();
    }

    /**
     * Get recent events.
     */
    public List<TelemetryEvent> getRecent(int count) {
        int start = Math.max(0, events.size() - count);
        return new ArrayList<>(events.subList(start, events.size()));
    }

    /**
     * Parse telemetry from JSON.
     */
    private static List<TelemetryEvent> parseTelemetryJson(String json) {
        List<TelemetryEvent> events = new ArrayList<>();
        
        try {
            JsonArray array = com.google.gson.JsonParser.parseString(json).getAsJsonArray();
            
            for (int i = 0; i < array.size(); i++) {
                JsonObject obj = array.get(i).getAsJsonObject();
                TelemetryEvent event = new TelemetryEvent();
                event.id = obj.has("id") ? obj.get("id").getAsString() : UUID.randomUUID().toString();
                event.timestamp = obj.get("timestamp").getAsLong();
                event.command = obj.get("command").getAsString();
                event.model = obj.get("model").getAsString();
                event.engine = obj.get("engine").getAsString();
                event.duration_ms = obj.get("duration_ms").getAsLong();
                event.tokens = obj.get("tokens").getAsInt();
                event.success = obj.get("success").getAsBoolean();
                
                events.add(event);
            }
        } catch (Exception e) {
            // Return empty list on parse error
        }
        
        return events;
    }

    /**
     * Telemetry event.
     */
    public static class TelemetryEvent {
        public String id;
        public long timestamp;
        public String command;
        public String model;
        public String engine;
        public long duration_ms;
        public int tokens;
        public boolean success;

        @Override
        public String toString() {
            return String.format("[%s] %s @ %s (%dms, %d tokens, %s)",
                new Date(timestamp), command, model, duration_ms, tokens,
                success ? "✓" : "✗");
        }
    }

    /**
     * Telemetry summary statistics.
     */
    public static class TelemetrySummary {
        public int total_queries = 0;
        public int successful_queries = 0;
        public int failed_queries = 0;
        public double average_latency_ms = 0;
        public int total_tokens = 0;
        public double throughput_tokens_per_sec = 0;
        public String top_model = "N/A";
        public String top_engine = "N/A";

        @Override
        public String toString() {
            return String.format(
                "Total: %d | Success: %d | Failed: %d | Avg Latency: %.1fms | Tokens: %d | Throughput: %.1f tok/s",
                total_queries, successful_queries, failed_queries, average_latency_ms,
                total_tokens, throughput_tokens_per_sec);
        }
    }
}
