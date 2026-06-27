package io.opentron.cli;

import io.opentron.cli.data.VaultStore;
import io.opentron.cli.data.VaultStore.SecretMetadata;
import io.opentron.cli.data.DataManager;

/**
 * Implement ``Tron vault`` - manage secrets and credentials.
 */
public class VaultCmd extends BaseCommand {
    private VaultStore vaultStore;

    public static void main(String[] args) throws Exception {
        VaultCmd cmd = new VaultCmd();
        try {
            cmd.execute(args);
        } catch (Exception e) {
            System.err.println("✗ Error: " + e.getMessage());
            if (cmd.verbose) {
                e.printStackTrace();
            }
            System.exit(1);
        }
    }

    @Override
    public void execute(String[] args) throws Exception {
        DataManager.initializeDirectories();
        vaultStore = new VaultStore();
        
        if (args.length == 0) {
            printUsage();
            return;
        }

        String subcommand = args[0];
        String[] subArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subArgs, 0, args.length - 1);
        
        switch (subcommand) {
            case "list":
                listSecrets();
                break;
            case "set":
                if (subArgs.length < 2) {
                    errorExit("Usage: tron vault set <key> <value>");
                }
                setSecret(subArgs[0], subArgs[1]);
                break;
            case "get":
                if (subArgs.length == 0) {
                    errorExit("Usage: tron vault get <key>");
                }
                getSecret(subArgs[0]);
                break;
            case "delete":
                if (subArgs.length == 0) {
                    errorExit("Usage: tron vault delete <key>");
                }
                deleteSecret(subArgs[0]);
                break;
            case "clear":
                clearVault();
                break;
            case "export":
                exportVault();
                break;
            case "count":
                showCount();
                break;
            default:
                errorExit("Unknown subcommand: " + subcommand);
        }
    }

    private void listSecrets() {
        java.util.Set<String> secrets = vaultStore.listSecrets();
        
        if (secrets.isEmpty()) {
            println("No secrets stored in vault.");
            return;
        }
        
        println("\nStored Secrets (" + secrets.size() + " total):");
        println("=".repeat(50));
        
        for (String key : secrets) {
            SecretMetadata meta = vaultStore.getMetadata(key);
            if (meta != null) {
                java.util.Date date = new java.util.Date(meta.created);
                println(String.format("  %s (type: %s, created: %s)", key, meta.type, date));
            }
        }
    }

    private void setSecret(String key, String value) throws Exception {
        vaultStore.setSecret(key, value, "generic");
        println("✓ Secret stored: " + key);
    }

    private void getSecret(String key) throws Exception {
        String value = vaultStore.getSecret(key);
        if (value != null) {
            println("Secret value for '" + key + "':");
            println("  " + value);
        } else {
            println("✗ Secret not found: " + key);
        }
    }

    private void deleteSecret(String key) throws Exception {
        boolean deleted = vaultStore.deleteSecret(key);
        if (deleted) {
            println("✓ Secret deleted: " + key);
        } else {
            println("✗ Secret not found: " + key);
        }
    }

    private void clearVault() throws Exception {
        println("Clearing all secrets from vault...");
        vaultStore.clear();
        println("✓ Vault cleared");
    }

    private void exportVault() throws Exception {
        String export = vaultStore.exportVault();
        println("Vault contents (encrypted):");
        println(export);
    }

    private void showCount() {
        int count = vaultStore.getCount();
        println("Vault Statistics:");
        println("  Stored secrets: " + count);
    }

    @Override
    public void printUsage() {
        println("Usage: tron vault <subcommand> [OPTIONS]");
        println();
        println("Subcommands:");
        println("  list                List stored secrets");
        println("  get <key>           Retrieve secret");
        println("  set <key> <value>   Store secret");
        println("  delete <key>        Delete secret");
        println("  clear               Clear all secrets");
        println("  export              Export vault");
        println("  count               Show secret count");
    }
}
