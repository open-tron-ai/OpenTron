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
 * In-memory database for OpenTron knowledge management.
 * Handles memory entries, search, and persistence.
 */
public class MemoryStore {
    private static final String MEMORY_DB = DataManager.getConfigDir() + "/memory.db";
    private List<MemoryEntry> entries;

    public MemoryStore() throws IOException {
        DataManager.initializeDirectories();
        this.entries = new ArrayList<>();
        loadMemory();
    }

    /**
     * Load memory from persistent storage.
     */
    private void loadMemory() throws IOException {
        Path memoryFile = Paths.get(MEMORY_DB);
        
        if (Files.exists(memoryFile)) {
            String json = new String(Files.readAllBytes(memoryFile), StandardCharsets.UTF_8);
            entries = parseMemoryJson(json);
        }
    }

    /**
     * Save memory to persistent storage.
     */
    public void save() throws IOException {
        JsonArray json = new JsonArray();
        
        for (MemoryEntry entry : entries) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", entry.id);
            obj.addProperty("content", entry.content);
            obj.addProperty("timestamp", entry.timestamp);
            obj.addProperty("tags", String.join(",", entry.tags));
            obj.addProperty("category", entry.category);
            json.add(obj);
        }
        
        DataManager.saveJson(Paths.get(MEMORY_DB), json.toString());
        DataManager.appendLog(DataManager.getConfigDir() + "/memory.log", 
            "Memory saved: " + entries.size() + " entries");
    }

    /**
     * Add a memory entry.
     */
    public void addEntry(String content, String category, List<String> tags) throws IOException {
        MemoryEntry entry = new MemoryEntry();
        entry.id = UUID.randomUUID().toString();
        entry.content = content;
        entry.timestamp = System.currentTimeMillis();
        entry.category = category != null ? category : "general";
        entry.tags = tags != null ? tags : new ArrayList<>();
        
        entries.add(entry);
        save();
    }

    /**
     * Search memory entries.
     */
    public List<MemoryEntry> search(String query) {
        List<MemoryEntry> results = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        
        for (MemoryEntry entry : entries) {
            if (entry.content.toLowerCase().contains(lowerQuery) ||
                entry.tags.stream().anyMatch(t -> t.toLowerCase().contains(lowerQuery)) ||
                entry.category.toLowerCase().contains(lowerQuery)) {
                results.add(entry);
            }
        }
        
        return results;
    }

    /**
     * Get all entries by category.
     */
    public List<MemoryEntry> getByCategory(String category) {
        List<MemoryEntry> results = new ArrayList<>();
        
        for (MemoryEntry entry : entries) {
            if (entry.category.equalsIgnoreCase(category)) {
                results.add(entry);
            }
        }
        
        return results;
    }

    /**
     * Delete memory entry by ID.
     */
    public boolean deleteEntry(String id) throws IOException {
        boolean removed = entries.removeIf(e -> e.id.equals(id));
        if (removed) {
            save();
        }
        return removed;
    }

    /**
     * Clear all memory.
     */
    public void clear() throws IOException {
        entries.clear();
        save();
    }

    /**
     * Get entry count.
     */
    public int getCount() {
        return entries.size();
    }

    /**
     * Get all entries.
     */
    public List<MemoryEntry> getAll() {
        return new ArrayList<>(entries);
    }

    /**
     * Export memory to JSON.
     */
    public String exportJson() {
        JsonArray json = new JsonArray();
        
        for (MemoryEntry entry : entries) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", entry.id);
            obj.addProperty("content", entry.content);
            obj.addProperty("timestamp", entry.timestamp);
            obj.addProperty("tags", String.join(",", entry.tags));
            obj.addProperty("category", entry.category);
            json.add(obj);
        }
        
        return json.toString();
    }

    /**
     * Export memory to CSV.
     */
    public String exportCsv() {
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Content,Timestamp,Tags,Category\n");
        
        for (MemoryEntry entry : entries) {
            String content = entry.content.replace("\"", "\"\"");
            String tags = String.join(";", entry.tags);
            csv.append(String.format("\"%s\",\"%s\",%d,\"%s\",\"%s\"\n",
                entry.id, content, entry.timestamp, tags, entry.category));
        }
        
        return csv.toString();
    }

    /**
     * Parse memory from JSON format.
     */
    private static List<MemoryEntry> parseMemoryJson(String json) {
        List<MemoryEntry> entries = new ArrayList<>();
        
        try {
            JsonArray array = com.google.gson.JsonParser.parseString(json).getAsJsonArray();
            
            for (int i = 0; i < array.size(); i++) {
                JsonObject obj = array.get(i).getAsJsonObject();
                MemoryEntry entry = new MemoryEntry();
                entry.id = obj.get("id").getAsString();
                entry.content = obj.get("content").getAsString();
                entry.timestamp = obj.get("timestamp").getAsLong();
                entry.category = obj.get("category").getAsString();
                
                String tags = obj.get("tags").getAsString();
                entry.tags = Arrays.asList(tags.split(","));
                
                entries.add(entry);
            }
        } catch (Exception e) {
            // Return empty list on parse error
        }
        
        return entries;
    }

    public static class MemoryEntry {
        public String id;
        public String content;
        public long timestamp;
        public String category;
        public List<String> tags;

        @Override
        public String toString() {
            return String.format("[%s] %s (tags: %s)", 
                new java.util.Date(timestamp), content, String.join(", ", tags));
        }
    }
}
