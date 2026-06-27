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
 * Model registry and catalog management.
 * Tracks available models, their specifications, and capabilities.
 */
public class ModelRegistry {
    private static final String REGISTRY_FILE = DataManager.getConfigDir() + "/models.json";
    private Map<String, ModelSpec> models;
    private static ModelRegistry instance;

    public ModelRegistry() throws IOException {
        DataManager.initializeDirectories();
        this.models = new HashMap<>();
        loadRegistry();
        registerBuiltinModels();
    }

    public static ModelRegistry getInstance() throws IOException {
        if (instance == null) {
            instance = new ModelRegistry();
        }
        return instance;
    }

    /**
     * Load model registry from storage.
     */
    private void loadRegistry() throws IOException {
        Path registryPath = Paths.get(REGISTRY_FILE);
        
        if (Files.exists(registryPath)) {
            String json = new String(Files.readAllBytes(registryPath), StandardCharsets.UTF_8);
            models = parseRegistryJson(json);
        }
    }

    /**
     * Save registry to storage.
     */
    public void save() throws IOException {
        JsonArray json = new JsonArray();
        
        for (ModelSpec spec : models.values()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", spec.id);
            obj.addProperty("name", spec.name);
            obj.addProperty("provider", spec.provider);
            obj.addProperty("parameters", spec.parameters);
            obj.addProperty("context_length", spec.context_length);
            obj.addProperty("description", spec.description);
            obj.addProperty("url", spec.url);
            
            JsonArray engines = new JsonArray();
            for (String engine : spec.supported_engines) {
                engines.add(engine);
            }
            obj.add("engines", engines);
            
            json.add(obj);
        }
        
        DataManager.saveJson(Paths.get(REGISTRY_FILE), json.toString());
    }

    /**
     * Register a model.
     */
    public void register(ModelSpec spec) throws IOException {
        models.put(spec.id, spec);
        save();
    }

    /**
     * Get model by ID.
     */
    public ModelSpec get(String modelId) {
        return models.get(modelId);
    }

    /**
     * List all models.
     */
    public Collection<ModelSpec> listAll() {
        return models.values();
    }

    /**
     * Find models by provider.
     */
    public List<ModelSpec> findByProvider(String provider) {
        List<ModelSpec> results = new ArrayList<>();
        
        for (ModelSpec spec : models.values()) {
            if (spec.provider.equalsIgnoreCase(provider)) {
                results.add(spec);
            }
        }
        
        return results;
    }

    /**
     * Find models by engine support.
     */
    public List<ModelSpec> findByEngine(String engine) {
        List<ModelSpec> results = new ArrayList<>();
        
        for (ModelSpec spec : models.values()) {
            if (spec.supported_engines.contains(engine)) {
                results.add(spec);
            }
        }
        
        return results;
    }

    /**
     * Find models by parameter count.
     */
    public List<ModelSpec> findByParameterRange(long minParams, long maxParams) {
        List<ModelSpec> results = new ArrayList<>();
        
        for (ModelSpec spec : models.values()) {
            if (spec.parameters >= minParams && spec.parameters <= maxParams) {
                results.add(spec);
            }
        }
        
        return results;
    }

    /**
     * Search models by name or description.
     */
    public List<ModelSpec> search(String query) {
        List<ModelSpec> results = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        
        for (ModelSpec spec : models.values()) {
            if (spec.name.toLowerCase().contains(lowerQuery) ||
                spec.description.toLowerCase().contains(lowerQuery) ||
                spec.id.toLowerCase().contains(lowerQuery)) {
                results.add(spec);
            }
        }
        
        return results;
    }

    /**
     * Get model count.
     */
    public int getCount() {
        return models.size();
    }

    /**
     * Check if model exists.
     */
    public boolean exists(String modelId) {
        return models.containsKey(modelId);
    }

    /**
     * Register builtin models.
     */
    private void registerBuiltinModels() throws IOException {
        // Ollama models
        register(new ModelSpec("qwen2.5:7b", "Qwen 2.5 7B", "Alibaba",
            7000000000L, 32768, "High-performance open source LLM",
            "https://huggingface.co/Qwen/Qwen2.5-7B", 
            Arrays.asList("ollama", "vllm")));

        register(new ModelSpec("qwen3.5:9b", "Qwen 3.5 9B", "Alibaba",
            9000000000L, 32768, "Qwen 3.5 variant with 9B parameters",
            "https://huggingface.co/Qwen/Qwen3.5-9B",
            Arrays.asList("ollama", "vllm")));

        register(new ModelSpec("mistral:7b", "Mistral 7B", "Mistral",
            7000000000L, 32768, "Mistral 7B open model",
            "https://huggingface.co/mistralai/Mistral-7B",
            Arrays.asList("ollama", "vllm", "llamacpp")));

        register(new ModelSpec("llama2:70b", "Llama 2 70B", "Meta",
            70000000000L, 4096, "Llama 2 70B model",
            "https://huggingface.co/meta-llama/Llama-2-70b",
            Arrays.asList("ollama", "vllm", "llamacpp")));

        // Cloud models
        register(new ModelSpec("gpt-4", "GPT-4", "OpenAI",
            -1, 8192, "OpenAI's most capable model",
            "https://openai.com/gpt-4",
            Arrays.asList("openai")));

        register(new ModelSpec("claude-3-opus", "Claude 3 Opus", "Anthropic",
            -1, 200000, "Anthropic's most capable model",
            "https://www.anthropic.com/claude",
            Arrays.asList("anthropic")));

        register(new ModelSpec("gemini-pro", "Gemini Pro", "Google",
            -1, 32768, "Google's Gemini model",
            "https://ai.google.dev",
            Arrays.asList("google")));
    }

    /**
     * Export registry as JSON.
     */
    public String exportJson() {
        JsonArray json = new JsonArray();
        
        for (ModelSpec spec : models.values()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", spec.id);
            obj.addProperty("name", spec.name);
            obj.addProperty("provider", spec.provider);
            obj.addProperty("parameters", spec.parameters);
            obj.addProperty("context_length", spec.context_length);
            obj.addProperty("description", spec.description);
            json.add(obj);
        }
        
        return json.toString();
    }

    /**
     * Parse registry from JSON.
     */
    private static Map<String, ModelSpec> parseRegistryJson(String json) {
        Map<String, ModelSpec> models = new HashMap<>();
        
        try {
            JsonArray array = com.google.gson.JsonParser.parseString(json).getAsJsonArray();
            
            for (int i = 0; i < array.size(); i++) {
                JsonObject obj = array.get(i).getAsJsonObject();
                
                List<String> engines = new ArrayList<>();
                if (obj.has("engines")) {
                    JsonArray engineArray = obj.getAsJsonArray("engines");
                    for (int j = 0; j < engineArray.size(); j++) {
                        engines.add(engineArray.get(j).getAsString());
                    }
                }
                
                ModelSpec spec = new ModelSpec(
                    obj.get("id").getAsString(),
                    obj.get("name").getAsString(),
                    obj.get("provider").getAsString(),
                    obj.get("parameters").getAsLong(),
                    obj.get("context_length").getAsInt(),
                    obj.get("description").getAsString(),
                    obj.get("url").getAsString(),
                    engines
                );
                
                models.put(spec.id, spec);
            }
        } catch (Exception e) {
            // Return empty map on parse error
        }
        
        return models;
    }

    /**
     * Model specification.
     */
    public static class ModelSpec {
        public String id;
        public String name;
        public String provider;
        public long parameters;  // -1 for unknown
        public int context_length;
        public String description;
        public String url;
        public List<String> supported_engines;

        public ModelSpec(String id, String name, String provider, long parameters,
                        int context_length, String description, String url,
                        List<String> supported_engines) {
            this.id = id;
            this.name = name;
            this.provider = provider;
            this.parameters = parameters;
            this.context_length = context_length;
            this.description = description;
            this.url = url;
            this.supported_engines = supported_engines;
        }

        @Override
        public String toString() {
            String paramStr = parameters > 0 ? String.format("%.1fB", parameters / 1e9) : "N/A";
            return String.format("%s (%s) - %s params, %d ctx", 
                name, provider, paramStr, context_length);
        }

        public String toDetailedString() {
            return String.format(
                "ID: %s\nName: %s\nProvider: %s\nParameters: %s\nContext: %d\nEngines: %s\n%s",
                id, name, provider, 
                parameters > 0 ? String.format("%.1fB", parameters / 1e9) : "N/A",
                context_length, 
                String.join(", ", supported_engines),
                description);
        }
    }
}
