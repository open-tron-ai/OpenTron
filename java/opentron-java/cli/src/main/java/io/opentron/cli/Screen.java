package io.opentron.cli;

/**
 * Screen capture and management utilities.
 */
public class Screen {
    public static String captureScreenToTemp() {
        try {
            String tempDir = System.getProperty("java.io.tmpdir");
            String timestamp = String.valueOf(System.currentTimeMillis());
            String filename = "screen_" + timestamp + ".png";
            String path = tempDir + "/" + filename;
            
            // Attempt platform-specific screen capture
            String osName = System.getProperty("os.name").toLowerCase();
            
            if (osName.contains("win")) {
                captureWindowsScreen(path);
            } else if (osName.contains("mac")) {
                captureMacScreen(path);
            } else if (osName.contains("nix") || osName.contains("nux")) {
                captureLinuxScreen(path);
            }
            
            return path;
        } catch (Exception e) {
            throw new RuntimeException("Screen capture failed: " + e.getMessage());
        }
    }

    private static void captureWindowsScreen(String path) throws Exception {
        // Use PowerShell to capture screen
        String cmd = String.format(
            "powershell -Command \"[System.Windows.Forms.SendKeys]::SendWait('^+{Print}'); " +
            "Start-Sleep -Milliseconds 500; " +
            "$img = [System.Windows.Forms.Clipboard]::GetImage(); " +
            "$img.Save('%s')\";", path
        );
        Runtime.getRuntime().exec(cmd).waitFor();
    }

    private static void captureMacScreen(String path) throws Exception {
        String cmd = "screencapture -x " + path;
        Runtime.getRuntime().exec(cmd).waitFor();
    }

    private static void captureLinuxScreen(String path) throws Exception {
        // Try common screen capture tools
        String[] commands = {
            "gnome-screenshot -f " + path,
            "import -window root " + path,
            "scrot " + path
        };
        
        for (String cmd : commands) {
            try {
                Process p = Runtime.getRuntime().exec(cmd);
                if (p.waitFor() == 0) return;
            } catch (Exception e) {
                // Try next command
            }
        }
        throw new RuntimeException("No screen capture tool available");
    }

    public static void displayScreenContent(String path) {
        try {
            String osName = System.getProperty("os.name").toLowerCase();
            String[] cmd;
            
            if (osName.contains("win")) {
                cmd = new String[]{"cmd", "/c", "start", path};
            } else if (osName.contains("mac")) {
                cmd = new String[]{"open", path};
            } else {
                cmd = new String[]{"xdg-open", path};
            }
            
            Runtime.getRuntime().exec(cmd);
        } catch (Exception e) {
            System.err.println("Could not display screen: " + e.getMessage());
        }
    }
}
