package io.opentron.cli;

/**
 * Logging configuration and setup.
 */
public class LogConfig {
    public static final String LOG_DIR = System.getProperty("user.home") + "/.OpenTron/logs";
    public static final String DEBUG_LOG = LOG_DIR + "/debug.log";
    public static final String ERROR_LOG = LOG_DIR + "/error.log";
    public static final String AUDIT_LOG = LOG_DIR + "/audit.log";

    public static void initializeLogging() {
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(LOG_DIR));
        } catch (Exception e) {
            System.err.println("Failed to create log directory: " + e.getMessage());
        }
    }

    public static void logDebug(String message) {
        log(DEBUG_LOG, "[DEBUG] " + message);
    }

    public static void logError(String message) {
        log(ERROR_LOG, "[ERROR] " + message);
        System.err.println(message);
    }

    public static void logAudit(String action, String details) {
        log(AUDIT_LOG, String.format("[%s] ACTION: %s | DETAILS: %s", 
            new java.util.Date(), action, details));
    }

    public static void logInfo(String message) {
        log(DEBUG_LOG, "[INFO] " + message);
    }

    private static void log(String file, String message) {
        try {
            String timestamp = String.format("[%s] %s\n", 
                new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()),
                message);
            
            java.nio.file.Path path = java.nio.file.Paths.get(file);
            java.nio.file.Files.createDirectories(path.getParent());
            
            if (java.nio.file.Files.exists(path)) {
                java.nio.file.Files.write(path, timestamp.getBytes(), 
                    java.nio.file.StandardOpenOption.APPEND);
            } else {
                java.nio.file.Files.write(path, timestamp.getBytes());
            }
        } catch (Exception e) {
            System.err.println("Logging error: " + e.getMessage());
        }
    }

    public static String getLastNLines(String file, int n) {
        try {
            java.util.List<String> lines = java.nio.file.Files.readAllLines(
                java.nio.file.Paths.get(file));
            int start = Math.max(0, lines.size() - n);
            return String.join("\n", lines.subList(start, lines.size()));
        } catch (Exception e) {
            return "Could not read log file: " + e.getMessage();
        }
    }
}
