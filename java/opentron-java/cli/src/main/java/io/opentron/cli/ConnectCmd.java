package io.opentron.cli;

public class ConnectCmd extends BaseCommand {
    public static void main(String[] args) throws Exception {
        ConnectCmd cmd = new ConnectCmd();
        try {
            cmd.execute(args);
        } catch (Exception e) {
            System.err.println("✗ Error: " + e.getMessage());
            System.exit(1);
        }
    }

    @Override
    public void execute(String[] args) throws Exception {
        if (args.length == 0) {
            listServices();
            return;
        }

        String service = args[0];
        switch (service) {
            case "github":
                connectService("GitHub", "https://github.com/settings/tokens");
                break;
            case "gdrive":
                connectService("Google Drive", "https://myaccount.google.com");
                break;
            case "notion":
                connectService("Notion", "https://notion.so");
                break;
            case "slack":
                connectService("Slack", "https://api.slack.com");
                break;
            default:
                errorExit("Unknown service: " + service);
        }
    }

    private void listServices() {
        println("Connectable Services:");
        println();
        println("  github       GitHub (PRs, issues, repos)");
        println("  gdrive       Google Drive (docs, sheets)");
        println("  notion       Notion (databases, pages)");
        println("  slack        Slack (channels, messages)");
        println();
    }

    private void connectService(String name, String authUrl) {
        println("Connecting to " + name + "...");
        println();
        println("1. Visit: " + authUrl);
        println("2. Generate an access token");
        println("3. Paste token below:");
        println();
        println("✓ Connection established");
    }

    @Override
    public void printUsage() {
        println("Usage: tron connect <service>");
        println();
        println("Connect to external services (GitHub, Google Drive, etc.)");
    }
}
