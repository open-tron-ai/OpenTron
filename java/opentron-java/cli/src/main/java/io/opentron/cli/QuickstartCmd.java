package io.opentron.cli;

public class QuickstartCmd extends BaseCommand {
    public static void main(String[] args) throws Exception {
        QuickstartCmd cmd = new QuickstartCmd();
        try {
            cmd.execute(args);
        } catch (Exception e) {
            System.err.println("✗ Error: " + e.getMessage());
            System.exit(1);
        }
    }

    @Override
    public void execute(String[] args) throws Exception {
        println("\n" + "=".repeat(60));
        println("OpenTron Quick Start Guide");
        println("=".repeat(60) + "\n");

        println("Step 1: Initialize OpenTron");
        println("-".repeat(30));
        println("$ tron init");
        println("  - Detects your hardware");
        println("  - Generates config.toml");
        println("  - Sets up default directories\n");

        println("Step 2: Install an Inference Engine");
        println("-".repeat(30));
        println("$ ollama serve");
        println("  - Starts Ollama on localhost:11434\n");

        println("Step 3: Pull a Model");
        println("-".repeat(30));
        println("$ ollama pull qwen2.5:7b");
        println("  - Downloads the model (~3.8GB)\n");

        println("Step 4: Verify Setup");
        println("-".repeat(30));
        println("$ tron doctor");
        println("  - Checks all components\n");

        println("Step 5: Start Chatting!");
        println("-".repeat(30));
        println("$ tron chat");
        println("  - Interactive conversation mode\n");

        println("Common Commands:");
        println("-".repeat(30));
        println("  tron ask \"What is AI?\"       Query the assistant");
        println("  tron chat                     Start interactive mode");
        println("  tron serve                    Start API server");
        println("  tron config list              View configuration");
        println("  tron model list               List available models");
        println("  tron --help                   Show all commands\n");

        println("=".repeat(60) + "\n");
    }

    @Override
    public void printUsage() {
        println("Usage: tron quickstart");
        println();
        println("Show the quick start guide for OpenTron");
    }
}
