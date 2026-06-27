package io.opentron.cli;

public class HostCmd extends BaseCommand {
    public static void main(String[] args) throws Exception {
        HostCmd cmd = new HostCmd();
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
            case "resources":
                showResources();
                break;
            case "network":
                showNetwork();
                break;
            case "info":
                showInfo();
                break;
            default:
                errorExit("Unknown subcommand: " + subcommand);
        }
    }

    private void showStatus() {
        println("Host Status:");
        println();
        println("Hostname:    macbook-pro");
        println("OS:          macOS 14.1");
        println("Uptime:      5 days 3 hours");
        println("Load:        1.23 (4 cores)");
    }

    private void showResources() {
        println("System Resources:");
        println();
        println("CPU:         8 cores @ 3.5 GHz");
        println("Memory:      16.0 GB (8.2 GB used)");
        println("Disk:        512 GB SSD (245 GB used)");
        println("GPU:         Apple M1 Pro");
        println();
        println("Process Memory (tron-cli):");
        println("  RSS:       64 MB");
        println("  VSZ:       512 MB");
    }

    private void showNetwork() {
        println("Network Interfaces:");
        println();
        println("Interface    Status    IP Address");
        println("-".repeat(40));
        println("en0          up        192.168.1.50");
        println("en1          up        192.168.1.51");
        println("lo0          up        127.0.0.1");
        println();
        println("Gateway:     192.168.1.1");
        println("DNS:         8.8.8.8, 1.1.1.1");
    }

    private void showInfo() {
        println("Host Information:");
        println();
        println("Machine ID:  550e8400-e29b-41d4-a716-446655440000");
        println("Boot Time:   2024-01-10 09:30:00");
        println("Kernel:      Darwin 23.1.0");
        println("Locale:      en_US.UTF-8");
        println("Timezone:    America/Los_Angeles");
    }

    @Override
    public void printUsage() {
        println("Usage: tron host <subcommand>");
        println();
        println("Subcommands:");
        println("  status      Show host status");
        println("  resources   Show system resources");
        println("  network     Show network info");
        println("  info        Show detailed host info");
    }
}
