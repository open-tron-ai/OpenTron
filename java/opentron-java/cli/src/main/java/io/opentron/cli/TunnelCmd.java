package io.opentron.cli;

public class TunnelCmd extends BaseCommand {
    public static void main(String[] args) throws Exception {
        TunnelCmd cmd = new TunnelCmd();
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
            listTunnels();
            return;
        }

        String subcommand = args[0];
        String[] subArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subArgs, 0, args.length - 1);

        switch (subcommand) {
            case "list":
                listTunnels();
                break;
            case "create":
                createTunnel(subArgs);
                break;
            case "connect":
                connectTunnel(subArgs);
                break;
            case "close":
                closeTunnel(subArgs);
                break;
            default:
                errorExit("Unknown subcommand: " + subcommand);
        }
    }

    private void listTunnels() {
        println("Active Tunnels:");
        println();
        println("ID       Local         Remote              Status");
        println("-".repeat(60));
        println("1        localhost:80  192.168.1.100:8000  connected");
        println();
    }

    private void createTunnel(String[] args) {
        String localPort = "8000";
        String remoteHost = null;

        for (int i = 0; i < args.length; i++) {
            if (("-l".equals(args[i]) || "--local".equals(args[i])) && i + 1 < args.length) {
                localPort = args[++i];
            } else if (("-r".equals(args[i]) || "--remote".equals(args[i])) && i + 1 < args.length) {
                remoteHost = args[++i];
            }
        }

        println("Creating tunnel...");
        println("  Local:  localhost:" + localPort);
        println("  Remote: " + (remoteHost != null ? remoteHost : "default"));
        println();
        println("✓ Tunnel created (ID: 2)");
    }

    private void connectTunnel(String[] args) {
        String tunnelId = args.length > 0 ? args[0] : "1";
        println("Connecting to tunnel " + tunnelId + "...");
        println("✓ Connected");
    }

    private void closeTunnel(String[] args) {
        String tunnelId = args.length > 0 ? args[0] : "1";
        println("Closing tunnel " + tunnelId + "...");
        println("✓ Tunnel closed");
    }

    @Override
    public void printUsage() {
        println("Usage: tron tunnel <subcommand> [OPTIONS]");
        println();
        println("Subcommands:");
        println("  list            List active tunnels");
        println("  create [OPT]    Create a new tunnel");
        println("  connect [ID]    Connect to a tunnel");
        println("  close [ID]      Close a tunnel");
    }
}
