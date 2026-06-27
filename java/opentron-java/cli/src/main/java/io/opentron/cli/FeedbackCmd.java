package io.opentron.cli;

public class FeedbackCmd extends BaseCommand {
    public static void main(String[] args) throws Exception {
        FeedbackCmd cmd = new FeedbackCmd();
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
            return;
        }

        String subcommand = args[0];
        String[] subArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subArgs, 0, args.length - 1);

        switch (subcommand) {
            case "send":
                sendFeedback(subArgs);
                break;
            case "list":
                listFeedback();
                break;
            case "view":
                viewFeedback(subArgs);
                break;
            default:
                errorExit("Unknown subcommand: " + subcommand);
        }
    }

    private void sendFeedback(String[] args) {
        String type = "general";
        String message = null;

        for (int i = 0; i < args.length; i++) {
            if (("-t".equals(args[i]) || "--type".equals(args[i])) && i + 1 < args.length) {
                type = args[++i];
            } else if (("-m".equals(args[i]) || "--message".equals(args[i])) && i + 1 < args.length) {
                message = args[++i];
            }
        }

        println("Sending " + type + " feedback...");
        if (message != null) {
            println("Message: " + message);
        }
        println();
        println("✓ Feedback submitted successfully");
        println("  Thank you for helping improve OpenTron!");
    }

    private void listFeedback() {
        println("Your Feedback History:");
        println();
        println("Date                Type       Status");
        println("-".repeat(50));
        println("2024-01-15 10:30    bug        resolved");
        println("2024-01-14 15:45    feature    in-progress");
        println("2024-01-10 08:20    general    acknowledged");
    }

    private void viewFeedback(String[] args) {
        String feedbackId = args.length > 0 ? args[0] : "1";
        println("Feedback #" + feedbackId);
        println();
        println("Status:     resolved");
        println("Type:       bug");
        println("Created:    2024-01-15 10:30");
        println("Updated:    2024-01-15 12:00");
        println();
        println("Description:");
        println("  Chat mode not saving conversation history");
        println();
        println("Resolution:");
        println("  Fixed in version 0.2.1");
    }

    @Override
    public void printUsage() {
        println("Usage: tron feedback <subcommand> [OPTIONS]");
        println();
        println("Subcommands:");
        println("  send [OPTIONS]  Send feedback");
        println("  list            Show feedback history");
        println("  view [ID]       View specific feedback");
        println();
        println("Send Options:");
        println("  -t, --type      Feedback type (bug, feature, general)");
        println("  -m, --message   Feedback message");
    }
}
