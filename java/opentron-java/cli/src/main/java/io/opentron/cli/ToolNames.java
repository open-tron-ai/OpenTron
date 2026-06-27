package io.opentron.cli;

/**
 * Tool name resolution and validation utilities.
 */
public class ToolNames {
    private static final String[] BUILTIN_TOOLS = {
        "calculator", "web_search", "file_read", "code_execute",
        "think", "summarize", "translate", "generate_image",
        "memory_store", "memory_search", "memory_retrieve",
        "channel_send", "channel_list", "channel_status",
        "llm", "embeddings", "vector_search"
    };

    public static boolean isValidTool(String name) {
        for (String tool : BUILTIN_TOOLS) {
            if (tool.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    public static String[] listAllTools() {
        return BUILTIN_TOOLS;
    }

    public static String[] parseToolNames(String commaSeparated) {
        if (commaSeparated == null || commaSeparated.trim().isEmpty()) {
            return new String[0];
        }
        
        String[] names = commaSeparated.split(",");
        java.util.List<String> validated = new java.util.ArrayList<>();
        
        for (String name : names) {
            String trimmed = name.trim();
            if (isValidTool(trimmed)) {
                validated.add(trimmed);
            }
        }
        
        return validated.toArray(new String[0]);
    }

    public static String getToolDescription(String name) {
        switch (name.toLowerCase()) {
            case "calculator":
                return "Perform mathematical calculations";
            case "web_search":
                return "Search the web for information";
            case "file_read":
                return "Read files from the filesystem";
            case "code_execute":
                return "Execute code snippets";
            case "think":
                return "Enable chain-of-thought reasoning";
            case "summarize":
                return "Summarize text or documents";
            case "translate":
                return "Translate text between languages";
            case "memory_store":
                return "Store information in memory";
            case "memory_search":
                return "Search stored memories";
            case "llm":
                return "Access language model directly";
            default:
                return "Unknown tool";
        }
    }

    public static String formatToolList() {
        StringBuilder sb = new StringBuilder("Available Tools:\n");
        for (String tool : BUILTIN_TOOLS) {
            sb.append(String.format("  - %s: %s\n", tool, getToolDescription(tool)));
        }
        return sb.toString();
    }
}
