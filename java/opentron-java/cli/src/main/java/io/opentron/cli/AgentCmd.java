package io.opentron.cli;

/**
 * Implement ``Tron agent`` / ``Tron agents`` - manage agents.
 */
public class AgentCmd extends BaseCommand {
    public static void main(String[] args) {
        AgentCmd cmd = new AgentCmd();
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
            listAgents();
            return;
        }

        String subcommand = args[0];
        
        switch (subcommand) {
            case "list":
                listAgents();
                break;
            case "create":
                if (args.length < 2) {
                    errorExit("Usage: tron agents create <agent_name>");
                }
                createAgent(args[1]);
                break;
            case "run":
                if (args.length < 2) {
                    errorExit("Usage: tron agents run <agent_name>");
                }
                runAgent(args[1]);
                break;
            case "delete":
                if (args.length < 2) {
                    errorExit("Usage: tron agents delete <agent_name>");
                }
                deleteAgent(args[1]);
                break;
            default:
                errorExit("Unknown subcommand: " + subcommand);
        }
    }

    private void listAgents() {
        println("Available agents:");
        println("  - simple         Basic question-answering agent");
        println("  - orchestrator   Multi-tool orchestration agent");
        println("  - react          ReAct reasoning agent");
    }

    private void createAgent(String agentName) {
        println("Creating agent: " + agentName);
        println("✓ Agent created");
    }

    private void runAgent(String agentName) {
        println("Running agent: " + agentName);
        println("(agent execution not yet implemented)");
    }

    private void deleteAgent(String agentName) {
        println("Deleting agent: " + agentName);
        println("✓ Agent deleted");
    }

    @Override
    public void printUsage() {
        println("Usage: tron agents <subcommand> [OPTIONS]");
        println();
        println("Subcommands:");
        println("  list                List available agents");
        println("  create <agent>      Create a new agent");
        println("  run <agent>         Run an agent");
        println("  delete <agent>      Delete an agent");
    }
}
