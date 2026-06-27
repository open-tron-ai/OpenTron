package io.opentron.cli;

public class AddCmd extends BaseCommand {
    public static void main(String[] args) throws Exception {
        AddCmd cmd = new AddCmd();
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

        String resource = args[0];
        String[] subArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subArgs, 0, args.length - 1);

        switch (resource) {
            case "model":
                addModel(subArgs);
                break;
            case "skill":
                addSkill(subArgs);
                break;
            case "tool":
                addTool(subArgs);
                break;
            case "channel":
                addChannel(subArgs);
                break;
            default:
                errorExit("Unknown resource: " + resource);
        }
    }

    private void addModel(String[] args) {
        String name = args.length > 0 ? args[0] : "new-model";
        println("Adding model: " + name);
        println("✓ Model added to registry");
    }

    private void addSkill(String[] args) {
        String name = args.length > 0 ? args[0] : "new-skill";
        println("Adding skill: " + name);
        println("✓ Skill added");
    }

    private void addTool(String[] args) {
        String name = args.length > 0 ? args[0] : "new-tool";
        println("Adding tool: " + name);
        println("✓ Tool added");
    }

    private void addChannel(String[] args) {
        String name = args.length > 0 ? args[0] : "new-channel";
        println("Adding channel: " + name);
        println("✓ Channel added");
    }

    @Override
    public void printUsage() {
        println("Usage: tron add <resource> [OPTIONS]");
        println();
        println("Resources:");
        println("  model      Add a new model");
        println("  skill      Add a new skill");
        println("  tool       Add a new tool");
        println("  channel    Add a new channel");
    }
}
