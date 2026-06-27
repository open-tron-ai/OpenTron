package io.opentron.cli;

/**
 * Installation and deployment detection utilities.
 */
public class InstallDetect {
    public static boolean isInstalledGlobally() {
        // Check if tron command exists in PATH
        try {
            Process p = Runtime.getRuntime().exec("which tron");
            int exitCode = p.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getInstallLocation() {
        String osName = System.getProperty("os.name").toLowerCase();
        
        if (osName.contains("win")) {
            return "C:\\Program Files\\OpenTron\\bin\\tron.exe";
        } else if (osName.contains("mac")) {
            return "/usr/local/bin/tron";
        } else {
            return "/usr/bin/tron";
        }
    }

    public static String getConfigDirectory() {
        return System.getProperty("user.home") + "/.OpenTron";
    }

    public static String getCacheDirectory() {
        return getConfigDirectory() + "/cache";
    }

    public static String getLogsDirectory() {
        return getConfigDirectory() + "/logs";
    }

    public static boolean isDockerAvailable() {
        try {
            Process p = Runtime.getRuntime().exec("docker --version");
            int exitCode = p.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getOsName() {
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        return osName + " " + osVersion;
    }

    public static String getJavaVersion() {
        return System.getProperty("java.version");
    }
}
