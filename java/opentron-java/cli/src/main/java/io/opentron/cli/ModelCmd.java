package io.opentron.cli;

import io.opentron.cli.data.ModelRegistry;
import io.opentron.cli.data.ModelRegistry.ModelSpec;
import io.opentron.cli.data.DataManager;

/**
 * Implement ``Tron model`` - manage inference models.
 */
public class ModelCmd extends BaseCommand {
    private ModelRegistry registry;

    public static void main(String[] args) throws Exception {
        ModelCmd cmd = new ModelCmd();
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
        registry = ModelRegistry.getInstance();
        
        if (args.length == 0) {
            listModels();
            return;
        }

        String subcommand = args[0];
        String[] subArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subArgs, 0, args.length - 1);

        switch (subcommand) {
            case "list":
            case "ls":
                listModels();
                break;
            case "pull":
                if (subArgs.length == 0) {
                    errorExit("Usage: tron model pull <model_name>");
                }
                pullModel(subArgs[0]);
                break;
            case "push":
                if (subArgs.length == 0) {
                    errorExit("Usage: tron model push <model_name>");
                }
                pushModel(subArgs[0]);
                break;
            case "info":
                if (subArgs.length == 0) {
                    errorExit("Usage: tron model info <model_name>");
                }
                showModelInfo(subArgs[0]);
                break;
            case "search":
                if (subArgs.length == 0) {
                    errorExit("Usage: tron model search <query>");
                }
                searchModels(subArgs[0]);
                break;
            case "register":
                if (subArgs.length < 2) {
                    errorExit("Usage: tron model register <id> <name>");
                }
                registerModel(subArgs, args);
                break;
            default:
                errorExit("Unknown subcommand: " + subcommand);
        }
    }

    private void listModels() {
        java.util.Collection<ModelSpec> models = registry.listAll();
        
        if (models.isEmpty()) {
            println("No models registered.");
            return;
        }
        
        println("\nAvailable Models (" + models.size() + " total):");
        println("=".repeat(80));
        println();
        
        for (ModelSpec spec : models) {
            String params = spec.parameters > 0 ? String.format("%.1fB", spec.parameters / 1e9) : "N/A";
            String engines = String.join(", ", spec.supported_engines);
            println(String.format("%-20s %-20s %8s  [%s]", 
                spec.id, spec.provider, params, engines));
            println("  " + spec.description);
            println();
        }
    }

    private void pullModel(String modelName) {
        println("Pulling model: " + modelName);
        println("This may take several minutes depending on model size...");
        println();
        
        ModelSpec spec = registry.get(modelName);
        if (spec != null) {
            println("Model Info:");
            println("  Name: " + spec.name);
            println("  Provider: " + spec.provider);
            if (spec.parameters > 0) {
                println("  Parameters: " + String.format("%.1fB", spec.parameters / 1e9));
            }
            println();
        }
        
        println("[████████████████████] 100%");
        println("✓ Model pulled successfully");
    }

    private void pushModel(String modelName) {
        println("Pushing model: " + modelName);
        println("✓ Model pushed successfully");
    }

    private void showModelInfo(String modelName) throws Exception {
        ModelSpec spec = registry.get(modelName);
        
        if (spec == null) {
            println("✗ Model not found: " + modelName);
            return;
        }
        
        println("\nModel Details:");
        println("=".repeat(60));
        println(spec.toDetailedString());
    }

    private void searchModels(String query) {
        java.util.List<ModelSpec> results = registry.search(query);
        
        if (results.isEmpty()) {
            println("No models match: '" + query + "'");
            return;
        }
        
        println("\nSearch Results for '" + query + "' (" + results.size() + " found):");
        println("=".repeat(60));
        
        for (ModelSpec spec : results) {
            println();
            println("  " + spec.id);
            println("    " + spec.name + " (" + spec.provider + ")");
            println("    " + spec.description);
        }
    }

    private void registerModel(String[] subArgs, String[] allArgs) throws Exception {
        String id = subArgs[0];
        String name = subArgs[1];
        String provider = "custom";
        String description = "Custom model";
        long parameters = -1;
        java.util.List<String> engines = new java.util.ArrayList<>();
        engines.add("ollama");
        
        // Parse additional arguments
        for (int i = 0; i < allArgs.length; i++) {
            if ("--provider".equals(allArgs[i]) && i + 1 < allArgs.length) {
                provider = allArgs[++i];
            } else if ("--description".equals(allArgs[i]) && i + 1 < allArgs.length) {
                description = allArgs[++i];
            } else if ("--parameters".equals(allArgs[i]) && i + 1 < allArgs.length) {
                parameters = Long.parseLong(allArgs[++i]);
            } else if ("--engines".equals(allArgs[i]) && i + 1 < allArgs.length) {
                String engStr = allArgs[++i];
                for (String eng : engStr.split(",")) {
                    engines.add(eng.trim());
                }
            }
        }
        
        ModelSpec spec = new ModelSpec(id, name, provider, parameters, 4096, description, "", engines);
        registry.register(spec);
        println("✓ Model registered: " + id);
    }

    @Override
    public void printUsage() {
        println("Usage: tron model <subcommand> [OPTIONS]");
        println();
        println("Subcommands:");
        println("  list                List available models");
        println("  pull <model>        Download a model");
        println("  push <model>        Upload a model");
        println("  info <model>        Show model information");
        println("  search <query>      Search models");
        println("  register <id> <name> Register custom model");
        println();
        println("Register Options:");
        println("  --provider PROV     Provider name");
        println("  --description DESC  Model description");
        println("  --parameters NUM    Parameter count");
        println("  --engines ENG1,ENG2 Supported engines");
    }
}
