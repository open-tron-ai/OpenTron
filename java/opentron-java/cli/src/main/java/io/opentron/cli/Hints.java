package io.opentron.cli;

/**
 * Helpful hints and suggestions for users.
 */
public class Hints {
    public static String getHint(String command) {
        switch (command) {
            case "ask":
                return "💡 Tip: Use 'tron ask \"query\" --research' for deep research mode with citations.";
            case "chat":
                return "💡 Tip: Use /help in chat mode to see all available commands.";
            case "model":
                return "💡 Tip: Use 'tron model search llama' to find models matching your query.";
            case "memory":
                return "💡 Tip: Use 'tron memory add \"text\" --tags ml,learning' to organize entries.";
            case "bench":
                return "💡 Tip: Use 'tron bench run -n 100' to run performance benchmarks.";
            case "vault":
                return "💡 Tip: Store secrets securely with 'tron vault set api-key value'.";
            case "telemetry":
                return "💡 Tip: Use 'tron telemetry summary' to see usage statistics.";
            case "init":
                return "💡 Tip: Run 'tron init' once to detect hardware and set up configuration.";
            case "doctor":
                return "💡 Tip: Use 'tron doctor' to diagnose any setup issues.";
            case "serve":
                return "💡 Tip: Use 'tron serve' to start an OpenAI-compatible API server.";
            default:
                return "💡 Tip: Use 'tron --help' to see all available commands.";
        }
    }

    public static String getPerformanceTip() {
        return "💡 Performance Tip: Use '--no-stream' flag for faster non-streaming responses.";
    }

    public static String getSecurityTip() {
        return "🔒 Security Tip: Store sensitive keys in vault using 'tron vault set'.";
    }

    public static String getGetStartedTip() {
        return "🚀 Get Started: Run 'tron quickstart' for a guided introduction.";
    }

    public static String getAdvancedTip() {
        return "🔧 Advanced: Use 'tron config list' to see all available configuration options.";
    }
}
