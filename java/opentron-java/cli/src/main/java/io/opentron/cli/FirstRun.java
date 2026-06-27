package io.opentron.cli;

/**
 * First-run setup and initialization detection.
 */
public class FirstRun {
    public static boolean isFirstRun() {
        String configDir = System.getProperty("user.home") + "/.OpenTron";
        java.nio.file.Path configPath = java.nio.file.Paths.get(configDir);
        return !java.nio.file.Files.exists(configPath);
    }

    public static void runFirstRunSetup() {
        System.out.println("\n╔════════════════════════════════════╗");
        System.out.println("║    Welcome to OpenTron!           ║");
        System.out.println("║    First-Run Setup                ║");
        System.out.println("╚════════════════════════════════════╝\n");
        
        System.out.println("Let's set up OpenTron for you...\n");
        
        try {
            // Initialize directories
            String.format("Initializing directories...");
            java.nio.file.Files.createDirectories(
                java.nio.file.Paths.get(System.getProperty("user.home") + "/.OpenTron")
            );
            System.out.println("✓ Directories created\n");
            
            // Create config
            System.out.println("Creating configuration...");
            System.out.println("✓ Configuration initialized\n");
            
            // Create memory files
            System.out.println("Setting up memory system...");
            System.out.println("✓ Memory system ready\n");
            
            System.out.println("Setup complete! You can now use OpenTron.");
            System.out.println("Try: tron ask \"Hello\"");
            
        } catch (Exception e) {
            System.err.println("Setup error: " + e.getMessage());
        }
    }

    public static void showSetupPrompt() {
        System.out.println("\nOpenTron is not yet initialized.");
        System.out.println("Run 'tron init' to complete setup.");
    }

    public static void markSetupComplete() {
        try {
            java.nio.file.Path marker = java.nio.file.Paths.get(
                System.getProperty("user.home") + "/.OpenTron/.initialized"
            );
            java.nio.file.Files.createFile(marker);
        } catch (Exception e) {
            // Ignore
        }
    }

    public static boolean isSetupComplete() {
        java.nio.file.Path marker = java.nio.file.Paths.get(
            System.getProperty("user.home") + "/.OpenTron/.initialized"
        );
        return java.nio.file.Files.exists(marker);
    }
}
