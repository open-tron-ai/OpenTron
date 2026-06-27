package io.opentron.cli;

/**
 * Chat banner and styling utilities.
 */
public class ChatBanner {
    public static String getChatBanner() {
        return "\n" +
            "╭──────────────────────────────────────╮\n" +
            "│   OpenTron Interactive Chat          │\n" +
            "╰──────────────────────────────────────╯\n" +
            "Type /help for commands, /quit to exit\n";
    }

    public static String getWelcome(String model, String engine) {
        return String.format(
            "\n╔══════════════════════════════════════╗\n" +
            "║  Welcome to OpenTron Chat            ║\n" +
            "╚══════════════════════════════════════╝\n" +
            "\n  Engine: %s\n  Model:  %s\n" +
            "  Type /help for available commands\n" +
            "  Type /quit to exit\n",
            engine, model
        );
    }

    public static String formatResponse(String content, String model) {
        return String.format("[%s]: %s", model, content);
    }

    public static String formatUserInput(String input) {
        return "You> " + input;
    }

    public static String getHelpText() {
        return "\n" +
            "Available Commands:\n" +
            "  /quit, /exit     Exit the chat\n" +
            "  /clear           Clear history\n" +
            "  /model           Show current model\n" +
            "  /history         Show conversation\n" +
            "  /help            Show this help\n" +
            "  /status          Show connection status\n" +
            "  /save            Save conversation\n" +
            "  /load            Load conversation\n";
    }

    public static String getStatusBar(int messageCount, boolean connected) {
        return String.format(
            "Messages: %d | Connected: %s | Ready for input",
            messageCount, connected ? "Yes" : "No"
        );
    }
}
