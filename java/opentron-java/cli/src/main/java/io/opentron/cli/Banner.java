package io.opentron.cli;

/**
 * Banner - CLI text formatting and banners
 */
public class Banner {

    public static String getMainBanner() {
        return """
            ╭────────────────────────────────────────────────────────╮
            │         OpenTron - Personal AI Assistant CLI            │
            │                                                          │
            │  Version 0.1.0 | Local-first | Privacy-focused         │
            ╰────────────────────────────────────────────────────────╯
            """;
    }

    public static String getInitBanner() {
        return """
            ╭────────────────────────────────────────────────────────╮
            │         OpenTron Configuration Setup                     │
            ├────────────────────────────────────────────────────────┤
            │  This will initialize your local setup with hardware    │
            │  detection and optimal engine configuration.            │
            ╰────────────────────────────────────────────────────────╯
            """;
    }

    public static String getChatBanner() {
        return """
            ╭────────────────────────────────────────────────────────╮
            │         OpenTron Interactive Chat                        │
            ├────────────────────────────────────────────────────────┤
            │  /quit, /exit  - Exit chat                               │
            │  /clear        - Clear history                           │
            │  /model        - Show current model                      │
            │  /help         - Show all commands                       │
            ╰────────────────────────────────────────────────────────╯
            """;
    }

    public static String getVoiceBanner() {
        return """
            ╭────────────────────────────────────────────────────────╮
            │         OpenTron Voice Chat                              │
            ├────────────────────────────────────────────────────────┤
            │  🎤 Voice Activation Enabled                             │
            │  Say "/exit" or "goodbye" to quit                        │
            │  All responses will be spoken aloud                      │
            ╰────────────────────────────────────────────────────────╯
            """;
    }

    public static String getDoctorBanner() {
        return """
            ╭────────────────────────────────────────────────────────╮
            │         OpenTron System Health Check                     │
            ├────────────────────────────────────────────────────────┤
            │  Checking engines, models, config, and connectivity...  │
            ╰────────────────────────────────────────────────────────╯
            """;
    }

    public static String getConfigBanner() {
        return """
            ╭────────────────────────────────────────────────────────╮
            │         OpenTron Configuration                           │
            ├────────────────────────────────────────────────────────┤
            │  Inspect and manage your OpenTron settings               │
            ╰────────────────────────────────────────────────────────╯
            """;
    }

    public static String getAgentBanner() {
        return """
            ╭────────────────────────────────────────────────────────╮
            │         OpenTron Agent Management                        │
            ├────────────────────────────────────────────────────────┤
            │  Create, schedule, and monitor persistent agents         │
            ╰────────────────────────────────────────────────────────╯
            """;
    }

    public static void printBanner(String banner) {
        System.out.println(banner);
    }

    public static void printStatusBox(String title, String... lines) {
        System.out.println("\n┌" + "─".repeat(58) + "┐");
        System.out.printf("│ %-56s │\n", " " + title);
        System.out.println("├" + "─".repeat(58) + "┤");
        for (String line : lines) {
            System.out.printf("│ %-56s │\n", line);
        }
        System.out.println("└" + "─".repeat(58) + "┘\n");
    }

    public static void printSuccess(String message) {
        System.out.println("✓ " + message);
    }

    public static void printError(String message) {
        System.out.println("✗ " + message);
    }

    public static void printWarning(String message) {
        System.out.println("⚠ " + message);
    }

    public static void printInfo(String message) {
        System.out.println("ℹ " + message);
    }

    public static String colorGreen(String text) {
        return "\u001B[32m" + text + "\u001B[0m";
    }

    public static String colorRed(String text) {
        return "\u001B[31m" + text + "\u001B[0m";
    }

    public static String colorYellow(String text) {
        return "\u001B[33m" + text + "\u001B[0m";
    }

    public static String colorCyan(String text) {
        return "\u001B[36m" + text + "\u001B[0m";
    }

    public static String colorBold(String text) {
        return "\u001B[1m" + text + "\u001B[0m";
    }
}
