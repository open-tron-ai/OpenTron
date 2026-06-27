package io.opentron.cli;

import io.opentron.cli.data.TelemetryStore;
import io.opentron.cli.data.TelemetryStore.TelemetrySummary;
import io.opentron.cli.data.TelemetryStore.TelemetryEvent;
import io.opentron.cli.data.DataManager;

/**
 * Implement ``Tron telemetry`` - manage telemetry collection.
 */
public class TelemetryCmd extends BaseCommand {
    private TelemetryStore telemetryStore;

    public static void main(String[] args) throws Exception {
        TelemetryCmd cmd = new TelemetryCmd();
        try {
            cmd.execute(args);
        } catch (Exception e) {
            System.err.println("✗ Error: " + e.getMessage());
            System.exit(1);
        }
    }

    @Override
    public void execute(String[] args) throws Exception {
        DataManager.initializeDirectories();
        telemetryStore = new TelemetryStore();
        
        if (args.length == 0) {
            showStatus();
            return;
        }

        String subcommand = args[0];
        String[] subArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subArgs, 0, args.length - 1);

        switch (subcommand) {
            case "status":
                showStatus();
                break;
            case "enable":
                enableTelemetry();
                break;
            case "disable":
                disableTelemetry();
                break;
            case "export":
                exportTelemetry(subArgs);
                break;
            case "clear":
                clearTelemetry();
                break;
            case "summary":
                showSummary();
                break;
            case "recent":
                showRecent(subArgs);
                break;
            default:
                errorExit("Unknown subcommand: " + subcommand);
        }
    }

    private void showStatus() {
        int count = telemetryStore.getEventCount();
        println("Telemetry Status:");
        println("  Collection: enabled");
        println("  Database: " + DataManager.getConfigDir() + "/telemetry.db");
        println("  Recorded Events: " + count);
        
        TelemetrySummary summary = telemetryStore.getSummary();
        if (count > 0) {
            println();
            println("Quick Stats:");
            println("  " + summary.toString());
        }
    }

    private void enableTelemetry() throws Exception {
        telemetryStore.setEnabled(true);
        println("✓ Telemetry enabled");
        println("  Data will be collected to " + DataManager.getConfigDir() + "/telemetry.db");
    }

    private void disableTelemetry() throws Exception {
        telemetryStore.setEnabled(false);
        println("✓ Telemetry disabled");
    }

    private void exportTelemetry(String[] args) throws Exception {
        String format = "csv";
        String outputFile = null;

        for (int i = 0; i < args.length; i++) {
            if (("-f".equals(args[i]) || "--format".equals(args[i])) && i + 1 < args.length) {
                format = args[++i];
            } else if (("-o".equals(args[i]) || "--output".equals(args[i])) && i + 1 < args.length) {
                outputFile = args[++i];
            }
        }

        if (outputFile == null) {
            outputFile = "telemetry_export." + format;
        }

        if ("csv".equalsIgnoreCase(format)) {
            java.nio.file.Files.write(
                java.nio.file.Paths.get(outputFile),
                telemetryStore.exportCsv().getBytes()
            );
        } else if ("json".equalsIgnoreCase(format)) {
            java.nio.file.Files.write(
                java.nio.file.Paths.get(outputFile),
                telemetryStore.exportJson().getBytes()
            );
        }

        println("✓ Export complete: " + outputFile);
    }

    private void clearTelemetry() throws Exception {
        println("Clearing telemetry database...");
        telemetryStore.clear();
        println("✓ Telemetry cleared");
    }

    private void showSummary() throws Exception {
        TelemetrySummary summary = telemetryStore.getSummary();
        
        println("\nTelemetry Summary");
        println("=".repeat(60));
        println();
        println("Total Queries:        " + summary.total_queries);
        println("Successful:           " + summary.successful_queries);
        println("Failed:               " + summary.failed_queries);
        println("Average Latency:      " + String.format("%.1f", summary.average_latency_ms) + "ms");
        println("Total Tokens:         " + summary.total_tokens);
        println("Throughput:           " + String.format("%.1f", summary.throughput_tokens_per_sec) + " tok/s");
        println("Top Model:            " + summary.top_model);
        println("Top Engine:           " + summary.top_engine);
    }

    private void showRecent(String[] args) throws Exception {
        int count = 10;
        
        for (int i = 0; i < args.length; i++) {
            if (("-n".equals(args[i]) || "--count".equals(args[i])) && i + 1 < args.length) {
                count = Integer.parseInt(args[++i]);
            }
        }
        
        java.util.List<TelemetryEvent> events = telemetryStore.getRecent(count);
        
        if (events.isEmpty()) {
            println("No telemetry events recorded yet.");
            return;
        }
        
        println("\nRecent Telemetry Events:");
        println("=".repeat(70));
        
        for (TelemetryEvent event : events) {
            println(event.toString());
        }
    }

    @Override
    public void printUsage() {
        println("Usage: tron telemetry <subcommand> [OPTIONS]");
        println();
        println("Subcommands:");
        println("  status              Show telemetry status");
        println("  enable              Enable telemetry collection");
        println("  disable             Disable telemetry collection");
        println("  summary             Show telemetry summary");
        println("  recent [OPT]        Show recent events");
        println("  export [OPT]        Export telemetry data");
        println("  clear               Clear telemetry database");
        println();
        println("Export Options:");
        println("  -f, --format        Output format (csv, json)");
        println("  -o, --output        Output file path");
        println();
        println("Recent Options:");
        println("  -n, --count         Number of events (default: 10)");
    }
}
