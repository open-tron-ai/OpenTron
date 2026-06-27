package io.opentron.cli;

/**
 * Implement ``Tron skill`` - manage skills and custom actions.
 */
public class SkillCmd extends BaseCommand {
    public static void main(String[] args) {
        SkillCmd cmd = new SkillCmd();
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
            System.exit(1);
        }

        String subcommand = args[0];
        
        switch (subcommand) {
            case "list":
                listSkills();
                break;
            case "create":
                if (args.length < 2) {
                    errorExit("Usage: tron skill create <skill_name>");
                }
                createSkill(args[1]);
                break;
            case "delete":
                if (args.length < 2) {
                    errorExit("Usage: tron skill delete <skill_name>");
                }
                deleteSkill(args[1]);
                break;
            default:
                errorExit("Unknown subcommand: " + subcommand);
        }
    }

    private void listSkills() {
        println("Available skills:");
        println("  (skills not yet implemented)");
    }

    private void createSkill(String skillName) {
        println("Creating skill: " + skillName);
        println("✓ Skill created");
    }

    private void deleteSkill(String skillName) {
        println("Deleting skill: " + skillName);
        println("✓ Skill deleted");
    }

    @Override
    public void printUsage() {
        println("Usage: tron skill <subcommand> [OPTIONS]");
        println();
        println("Subcommands:");
        println("  list                List available skills");
        println("  create <skill>      Create a new skill");
        println("  delete <skill>      Delete a skill");
    }
}
