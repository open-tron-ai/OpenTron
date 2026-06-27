package io.opentron.cli;

public class OperatorsCmd extends BaseCommand {
    public static void main(String[] args) throws Exception {
        OperatorsCmd cmd = new OperatorsCmd();
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
            listOperators();
            return;
        }

        String subcommand = args[0];
        String[] subArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subArgs, 0, args.length - 1);

        switch (subcommand) {
            case "list":
                listOperators();
                break;
            case "create":
                createOperator(subArgs);
                break;
            case "run":
                runOperator(subArgs);
                break;
            case "delete":
                deleteOperator(subArgs);
                break;
            default:
                errorExit("Unknown subcommand: " + subcommand);
        }
    }

    private void listOperators() {
        println("Available Operators:");
        println();
        println("Name                Type        Status");
        println("-".repeat(40));
        println("email-classifier    processor   active");
        println("sentiment-analyzer  analyzer    active");
        println("code-reviewer       reviewer    inactive");
    }

    private void createOperator(String[] args) {
        String name = args.length > 0 ? args[0] : "new_operator";
        println("Creating operator: " + name);
        println("✓ Operator created");
    }

    private void runOperator(String[] args) {
        String name = args.length > 0 ? args[0] : "operator";
        println("Running operator: " + name);
        println("✓ Execution complete");
    }

    private void deleteOperator(String[] args) {
        String name = args.length > 0 ? args[0] : "operator";
        println("Deleting operator: " + name);
        println("✓ Operator deleted");
    }

    @Override
    public void printUsage() {
        println("Usage: tron operators <subcommand> [OPTIONS]");
        println();
        println("Subcommands:");
        println("  list              List operators");
        println("  create <name>     Create operator");
        println("  run <name>        Run operator");
        println("  delete <name>     Delete operator");
    }
}
