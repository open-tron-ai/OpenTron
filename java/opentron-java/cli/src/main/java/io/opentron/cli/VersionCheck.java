package io.opentron.cli;

/**
 * Version checking and update detection.
 */
public class VersionCheck {
    private static final String CURRENT_VERSION = "0.1.0";
    private static final String LATEST_CHECK_URL = "https://api.github.com/repos/opentron/opentron/releases/latest";

    public static String getCurrentVersion() {
        return CURRENT_VERSION;
    }

    public static boolean isUpdateAvailable() {
        try {
            String latest = getLatestVersion();
            return isNewerVersion(latest, CURRENT_VERSION);
        } catch (Exception e) {
            return false;
        }
    }

    public static String getLatestVersion() {
        try {
            java.net.URL url = new java.net.URL(LATEST_CHECK_URL);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            
            int status = conn.getResponseCode();
            if (status == 200) {
                java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getInputStream())
                );
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.contains("\"tag_name\"")) {
                        // Extract version from "tag_name": "v0.2.0"
                        return line.split("\"")[3];
                    }
                }
            }
        } catch (Exception e) {
            // Silently fail
        }
        return CURRENT_VERSION;
    }

    private static boolean isNewerVersion(String latest, String current) {
        // Simple version comparison
        String[] latestParts = latest.replaceAll("[v]", "").split("\\.");
        String[] currentParts = current.split("\\.");
        
        for (int i = 0; i < Math.min(latestParts.length, currentParts.length); i++) {
            try {
                int latestNum = Integer.parseInt(latestParts[i]);
                int currentNum = Integer.parseInt(currentParts[i]);
                if (latestNum > currentNum) return true;
                if (latestNum < currentNum) return false;
            } catch (NumberFormatException e) {
                // Continue to next part
            }
        }
        
        return latestParts.length > currentParts.length;
    }

    public static void printVersionInfo() {
        System.out.println("OpenTron Version: " + CURRENT_VERSION);
        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("OS: " + System.getProperty("os.name"));
    }

    public static String getUpdateMessage() {
        return "A new version of OpenTron is available!\n" +
               "Run 'tron self-update' to upgrade.";
    }
}
