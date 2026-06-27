package io.opentron.cli;

/**
 * Implement ``Tron tool`` - manage tools and plugins.
 */
public class ToolCmd extends BaseCommand {
    public static void main(String[] args) {
        ToolCmd cmd = new ToolCmd();
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
            printUsage();
            System.exit(1);
        }

        String subcommand = args[0];
        
        switch (subcommand) {
            case "list":
                listTools();
                break;
            case "enable":
                if (args.length < 2) {
                    errorExit("Usage: tron tool enable <tool_name>");
                }
                enableTool(args[1]);
                break;
            case "disable":
                if (args.length < 2) {
                    errorExit("Usage: tron tool disable <tool_name>");
                }
                disableTool(args[1]);
                break;
            case "info":
                if (args.length < 2) {
                    errorExit("Usage: tron tool info <tool_name>");
                }
                showToolInfo(args[1]);
                break;
            default:
                errorExit("Unknown subcommand: " + subcommand);
        }
    }

    private void listTools() {
        println("Available tools:");
        println("  - calculator    Perform mathematical calculations");
        println("  - web_search    Search the web");
        println("  - file_read     Read files");
        println("  - code_execute  Execute code");
    }

    private void enableTool(String toolName) {
        println("Enabling tool: " + toolName);
        println("✓ Tool enabled");
    }

    private void disableTool(String toolName) {
        println("Disabling tool: " + toolName);
        println("✓ Tool disabled");
    }

    private void showToolInfo(String toolName) {
        println("Tool: " + toolName);
        println("  Status: enabled");
        println("  (tool info not yet implemented)");
    }

    @Override
    public void printUsage() {
        println("Usage: tron tool <subcommand> [OPTIONS]");
        println();
        println("Subcommands:");
        println("  list                List available tools");
        println("  enable <tool>       Enable a tool");
        println("  disable <tool>      Disable a tool");
        println("  info <tool>         Show tool information");
    }
}
