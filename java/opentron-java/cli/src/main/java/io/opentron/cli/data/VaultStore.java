package io.opentron.cli.data;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Encrypted vault for storing secrets and credentials.
 */
public class VaultStore {
    private static final String VAULT_FILE = DataManager.getConfigDir() + "/vault.json";
    private Map<String, Secret> secrets;
    private SimpleEncryption encryption;

    public VaultStore() throws IOException {
        DataManager.initializeDirectories();
        this.encryption = new SimpleEncryption();
        this.secrets = new HashMap<>();
        loadVault();
    }

    /**
     * Load vault from storage.
     */
    private void loadVault() throws IOException {
        Path vaultPath = Paths.get(VAULT_FILE);
        
        if (Files.exists(vaultPath)) {
            String json = new String(Files.readAllBytes(vaultPath), StandardCharsets.UTF_8);
            secrets = parseVaultJson(json);
        }
    }

    /**
     * Save vault to storage.
     */
    public void save() throws IOException {
        JsonObject vaultJson = new JsonObject();
        
        for (Map.Entry<String, Secret> entry : secrets.entrySet()) {
            JsonObject secret = new JsonObject();
            secret.addProperty("value", encryption.encrypt(entry.getValue().value));
            secret.addProperty("type", entry.getValue().type);
            secret.addProperty("created", entry.getValue().created);
            secret.addProperty("accessed", entry.getValue().accessed);
            vaultJson.add(entry.getKey(), secret);
        }
        
        DataManager.saveJson(Paths.get(VAULT_FILE), vaultJson.toString());
        DataManager.appendLog(DataManager.getConfigDir() + "/vault.log", 
            "Vault saved: " + secrets.size() + " secrets");
    }

    /**
     * Store a secret.
     */
    public void setSecret(String key, String value, String type) throws IOException {
        Secret secret = new Secret();
        secret.value = value;
        secret.type = type != null ? type : "generic";
        secret.created = System.currentTimeMillis();
        secret.accessed = System.currentTimeMillis();
        
        secrets.put(key, secret);
        save();
    }

    /**
     * Retrieve a secret.
     */
    public String getSecret(String key) throws IOException {
        Secret secret = secrets.get(key);
        if (secret != null) {
            secret.accessed = System.currentTimeMillis();
            save();
            return secret.value;
        }
        return null;
    }

    /**
     * Delete a secret.
     */
    public boolean deleteSecret(String key) throws IOException {
        boolean removed = secrets.remove(key) != null;
        if (removed) {
            save();
        }
        return removed;
    }

    /**
     * List all secret keys (not values).
     */
    public Set<String> listSecrets() {
        return new HashSet<>(secrets.keySet());
    }

    /**
     * Clear all secrets.
     */
    public void clear() throws IOException {
        secrets.clear();
        save();
    }

    /**
     * Get secret count.
     */
    public int getCount() {
        return secrets.size();
    }

    /**
     * Get secret metadata.
     */
    public SecretMetadata getMetadata(String key) {
        Secret secret = secrets.get(key);
        if (secret != null) {
            SecretMetadata meta = new SecretMetadata();
            meta.key = key;
            meta.type = secret.type;
            meta.created = secret.created;
            meta.accessed = secret.accessed;
            return meta;
        }
        return null;
    }

    /**
     * Check if secret exists.
     */
    public boolean hasSecret(String key) {
        return secrets.containsKey(key);
    }

    /**
     * Export vault (encrypted).
     */
    public String exportVault() {
        JsonObject vaultJson = new JsonObject();
        
        for (Map.Entry<String, Secret> entry : secrets.entrySet()) {
            JsonObject secret = new JsonObject();
            secret.addProperty("type", entry.getValue().type);
            secret.addProperty("created", entry.getValue().created);
            vaultJson.add(entry.getKey(), secret);
        }
        
        return vaultJson.toString();
    }

    /**
     * Parse vault from JSON.
     */
    private static Map<String, Secret> parseVaultJson(String json) {
        Map<String, Secret> secrets = new HashMap<>();
        
        try {
            JsonObject vaultJson = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            SimpleEncryption encryption = new SimpleEncryption();
            
            for (String key : vaultJson.keySet()) {
                JsonObject secretObj = vaultJson.getAsJsonObject(key);
                Secret secret = new Secret();
                secret.value = encryption.decrypt(secretObj.get("value").getAsString());
                secret.type = secretObj.get("type").getAsString();
                secret.created = secretObj.get("created").getAsLong();
                secret.accessed = 0;
                
                secrets.put(key, secret);
            }
        } catch (Exception e) {
            // Return empty map on parse error
        }
        
        return secrets;
    }

    /**
     * Simple encryption for secrets (base64 + XOR for demo).
     * In production, use proper encryption like AES.
     */
    private static class SimpleEncryption {
        private static final String KEY = "opentron-secret-key";

        public String encrypt(String value) {
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            byte[] encrypted = xorBytes(bytes, KEY.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        }

        public String decrypt(String encrypted) {
            try {
                byte[] bytes = Base64.getDecoder().decode(encrypted);
                byte[] decrypted = xorBytes(bytes, KEY.getBytes());
                return new String(decrypted, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                return "";
            }
        }

        private byte[] xorBytes(byte[] data, byte[] key) {
            byte[] result = new byte[data.length];
            for (int i = 0; i < data.length; i++) {
                result[i] = (byte) (data[i] ^ key[i % key.length]);
            }
            return result;
        }
    }

    /**
     * Secret value holder.
     */
    private static class Secret {
        String value;
        String type;
        long created;
        long accessed;
    }

    /**
     * Secret metadata.
     */
    public static class SecretMetadata {
        public String key;
        public String type;
        public long created;
        public long accessed;

        @Override
        public String toString() {
            return String.format("%s (type: %s, created: %s)", 
                key, type, new Date(created));
        }
    }
}
