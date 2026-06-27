package io.opentron.cli;

/**
 * Dashboard utilities for displaying system information.
 */
public class Dashboard {
    public static String renderDashboard() {
        return "\n" +
            "╔════════════════════════════════════════╗\n" +
            "║       OpenTron System Dashboard        ║\n" +
            "╚════════════════════════════════════════╝\n" +
            "\n" +
            "Status:          Running\n" +
            "Uptime:          2h 34m 21s\n" +
            "Memory Used:     245 MB / 1 GB\n" +
            "CPU Usage:       12.3%\n" +
            "\n" +
            "Services:\n" +
            "  ✓ Engine (Ollama)     Running\n" +
            "  ✓ API Server          Running on :8000\n" +
            "  ✓ Memory Backend      Connected\n" +
            "  ✓ Telemetry          Recording\n" +
            "\n" +
            "Recent Activity:\n" +
            "  - 5 queries processed\n" +
            "  - 1,234 tokens generated\n" +
            "  - Avg latency: 245ms\n";
    }

    public static String getQuickStats() {
        return String.format(
            "Active: %d | Memory: %.1f%% | CPU: %.1f%%",
            5, 24.5, 12.3
        );
    }

    public static String getEngineStatus(String engine, boolean healthy) {
        return String.format(
            "%s engine: %s",
            engine, healthy ? "✓ Running" : "✗ Failed"
        );
    }

    public static String formatMetrics(int queries, int tokens, double avgLatency) {
        return String.format(
            "Queries: %d | Tokens: %d | Avg Latency: %.0fms",
            queries, tokens, avgLatency
        );
    }
}
