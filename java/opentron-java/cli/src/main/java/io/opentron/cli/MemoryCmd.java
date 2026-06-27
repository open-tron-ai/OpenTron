package io.opentron.cli;

import io.opentron.cli.data.MemoryStore;
import io.opentron.cli.data.MemoryStore.MemoryEntry;
import io.opentron.cli.data.DataManager;

/**
 * Implement ``Tron memory`` - manage memory and context.
 */
public class MemoryCmd extends BaseCommand {
    private MemoryStore memoryStore;

    public static void main(String[] args) throws Exception {
        MemoryCmd cmd = new MemoryCmd();
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
        memoryStore = new MemoryStore();
        
        if (args.length == 0) {
            listMemory();
            return;
        }

        String subcommand = args[0];
        String[] subArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subArgs, 0, args.length - 1);
        
        switch (subcommand) {
            case "list":
            case "ls":
                listMemory();
                break;
            case "search":
                if (subArgs.length == 0) {
                    errorExit("Usage: tron memory search <query>");
                }
                searchMemory(subArgs[0]);
                break;
            case "add":
                if (subArgs.length == 0) {
                    errorExit("Usage: tron memory add <text>");
                }
                addMemory(subArgs, args);
                break;
            case "delete":
                if (subArgs.length == 0) {
                    errorExit("Usage: tron memory delete <id>");
                }
                deleteMemory(subArgs[0]);
                break;
            case "clear":
                clearMemory();
                break;
            case "export":
                exportMemory(subArgs);
                break;
            case "count":
                showCount();
                break;
            default:
                errorExit("Unknown subcommand: " + subcommand);
        }
    }

    private void listMemory() throws Exception {
        java.util.List<MemoryEntry> entries = memoryStore.getAll();
        
        if (entries.isEmpty()) {
            println("No memory entries yet.");
            println("Use 'tron memory add' to create entries.");
            return;
        }
        
        println("\nMemory Entries (" + entries.size() + " total):");
        println("=".repeat(70));
        
        for (int i = 0; i < Math.min(entries.size(), 20); i++) {
            MemoryEntry entry = entries.get(i);
            java.util.Date date = new java.util.Date(entry.timestamp);
            println(String.format("\n[%s] %s", date, entry.id.substring(0, 8)));
            println("  Category: " + entry.category);
            println("  Tags: " + String.join(", ", entry.tags));
            println("  Content: " + entry.content.substring(0, Math.min(60, entry.content.length())) + "...");
        }
        
        if (entries.size() > 20) {
            println("\n... and " + (entries.size() - 20) + " more entries");
        }
    }

    private void searchMemory(String query) throws Exception {
        java.util.List<MemoryEntry> results = memoryStore.search(query);
        
        if (results.isEmpty()) {
            println("No memory entries match: '" + query + "'");
            return;
        }
        
        println("\nSearch Results for '" + query + "' (" + results.size() + " found):");
        println("=".repeat(70));
        
        for (MemoryEntry entry : results) {
            java.util.Date date = new java.util.Date(entry.timestamp);
            println(String.format("\n[%s] %s", date, entry.id.substring(0, 8)));
            println("  " + entry.content.substring(0, Math.min(65, entry.content.length())));
        }
    }

    private void addMemory(String[] subArgs, String[] allArgs) throws Exception {
        String text = String.join(" ", subArgs);
        String category = "general";
        java.util.List<String> tags = new java.util.ArrayList<>();
        
        // Parse category and tags from args
        for (int i = 0; i < allArgs.length; i++) {
            if ("--category".equals(allArgs[i]) && i + 1 < allArgs.length) {
                category = allArgs[++i];
            } else if ("--tags".equals(allArgs[i]) && i + 1 < allArgs.length) {
                String tagStr = allArgs[++i];
                for (String tag : tagStr.split(",")) {
                    tags.add(tag.trim());
                }
            }
        }
        
        memoryStore.addEntry(text, category, tags);
        println("✓ Memory entry added");
        println("  Category: " + category);
        if (!tags.isEmpty()) {
            println("  Tags: " + String.join(", ", tags));
        }
    }

    private void deleteMemory(String id) throws Exception {
        boolean deleted = memoryStore.deleteEntry(id);
        if (deleted) {
            println("✓ Memory entry deleted: " + id);
        } else {
            println("✗ Memory entry not found: " + id);
        }
    }

    private void clearMemory() throws Exception {
        println("Clearing all memory entries...");
        memoryStore.clear();
        println("✓ Memory cleared");
    }

    private void exportMemory(String[] subArgs) throws Exception {
        String format = "json";
        String outputFile = null;
        
        for (int i = 0; i < subArgs.length; i++) {
            if (("-f".equals(subArgs[i]) || "--format".equals(subArgs[i])) && i + 1 < subArgs.length) {
                format = subArgs[++i];
            } else if (("-o".equals(subArgs[i]) || "--output".equals(subArgs[i])) && i + 1 < subArgs.length) {
                outputFile = subArgs[++i];
            }
        }
        
        if (outputFile == null) {
            outputFile = "memory_export." + format;
        }
        
        if ("json".equalsIgnoreCase(format)) {
            java.nio.file.Files.write(
                java.nio.file.Paths.get(outputFile),
                memoryStore.exportJson().getBytes()
            );
        } else if ("csv".equalsIgnoreCase(format)) {
            java.nio.file.Files.write(
                java.nio.file.Paths.get(outputFile),
                memoryStore.exportCsv().getBytes()
            );
        }
        
        println("✓ Memory exported to: " + outputFile);
    }

    private void showCount() throws Exception {
        int count = memoryStore.getCount();
        println("Memory Statistics:");
        println("  Total entries: " + count);
    }

    @Override
    public void printUsage() {
        println("Usage: tron memory <subcommand> [OPTIONS]");
        println();
        println("Subcommands:");
        println("  list                List memory entries");
        println("  search <query>      Search memory");
        println("  add <text> [OPT]    Add memory entry");
        println("  delete <id>         Delete memory entry");
        println("  clear               Clear all memory");
        println("  export [OPT]        Export memory to file");
        println("  count               Show memory count");
        println();
        println("Add Options:");
        println("  --category CAT      Set category (default: general)");
        println("  --tags TAG1,TAG2    Add tags (comma-separated)");
        println();
        println("Export Options:");
        println("  -f, --format        Output format (json, csv)");
        println("  -o, --output        Output file path");
    }
}
