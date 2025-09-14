package online.chatchai.github.mcbench.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

/**
 * Utility class for MCBench Pro
 * Contains helper methods for version checking, formatting, and other common operations
 */
public class Util {
    
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");
    private static final Pattern MINECRAFT_VERSION_PATTERN = Pattern.compile("1\\.(\\d+)");
    
    /**
     * Get the major Java version number
     * @return Major Java version (e.g., 21 for Java 21)
     */
    public static int getJavaMajorVersion() {
        String version = System.getProperty("java.version");
        
        if (version.startsWith("1.")) {
            // Java 8 and earlier format: 1.8.0_xxx
            return Integer.parseInt(version.substring(2, 3));
        } else {
            // Java 9+ format: 11.0.1, 17.0.2, 21.0.1
            return Integer.parseInt(version.split("\\.")[0]);
        }
    }
    
    /**
     * Check if the current Minecraft version is 1.21 or newer
     * @param serverVersion Server version string from Bukkit.getVersion()
     * @return true if 1.21+, false otherwise
     */
    public static boolean isMinecraft121OrNewer(String serverVersion) {
        try {
            Matcher matcher = MINECRAFT_VERSION_PATTERN.matcher(serverVersion);
            if (matcher.find()) {
                int minorVersion = Integer.parseInt(matcher.group(1));
                return minorVersion >= 21;
            }
        } catch (Exception e) {
            // Fallback: if we can't parse, assume it's compatible
            return true;
        }
        return false;
    }
    
    /**
     * Format a double value to 2 decimal places
     * @param value Value to format
     * @return Formatted string
     */
    public static String formatDecimal(double value) {
        return DECIMAL_FORMAT.format(value);
    }
    
    /**
     * Format time in seconds to minutes:seconds format
     * @param seconds Time in seconds
     * @return Formatted time string (e.g., "2:35")
     */
    public static String formatTime(double seconds) {
        int minutes = (int) (seconds / 60);
        int remainingSeconds = (int) (seconds % 60);
        return String.format("%d:%02d", minutes, remainingSeconds);
    }
    
    /**
     * Format memory in bytes to MB
     * @param bytes Memory in bytes
     * @return Formatted string with MB suffix
     */
    public static String formatMemoryMB(long bytes) {
        return formatDecimal(bytes / 1024.0 / 1024.0) + " MB";
    }
    
    /**
     * Format memory in bytes to GB
     * @param bytes Memory in bytes
     * @return Formatted string with GB suffix
     */
    public static String formatMemoryGB(long bytes) {
        return formatDecimal(bytes / 1024.0 / 1024.0 / 1024.0) + " GB";
    }
    
    /**
     * Colorize a message using ChatColor codes
     * @param message Message with & color codes
     * @return Colored message
     */
    public static String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * Get server loader name (Paper, Pufferfish, etc.)
     * @return Server implementation name
     */
    public static String getServerLoader() {
        try {
            // Try server name first
            String serverName = Bukkit.getServer().getName();
            if (serverName != null) {
                serverName = serverName.toLowerCase();
                if (serverName.contains("folia")) {
                    return "Folia";
                } else if (serverName.contains("pufferfish")) {
                    return "Pufferfish";
                } else if (serverName.contains("purpur")) {
                    return "Purpur";
                } else if (serverName.contains("paper")) {
                    return "Paper";
                } else if (serverName.contains("spigot")) {
                    return "Spigot";
                }
            }
            
            // Fallback to version string
            String version = Bukkit.getVersion().toLowerCase();
            if (version.contains("folia")) {
                return "Folia";
            } else if (version.contains("pufferfish")) {
                return "Pufferfish";
            } else if (version.contains("purpur")) {
                return "Purpur";
            } else if (version.contains("paper")) {
                return "Paper";
            } else if (version.contains("spigot")) {
                return "Spigot";
            } else if (version.contains("craftbukkit")) {
                return "CraftBukkit";
            }
            
            return "Unknown";
        } catch (Exception e) {
            return "Unknown";
        }
    }
    
    /**
     * Check if ViaVersion plugin is present
     * @return true if ViaVersion is loaded, false otherwise
     */
    public static boolean hasViaVersion() {
        return Bukkit.getPluginManager().getPlugin("ViaVersion") != null;
    }
    
    /**
     * Get CPU model name from system properties and OS-specific commands
     * @return CPU model or "Unknown" if not available
     */
    public static String getCPUModel() {
        try {
            // Try platform-specific methods to get actual CPU model
            String osName = System.getProperty("os.name").toLowerCase();
            
            if (osName.contains("windows")) {
                return getCPUModelWindows();
            } else if (osName.contains("linux")) {
                return getCPUModelLinux();
            } else if (osName.contains("mac")) {
                return getCPUModelMac();
            }
            
            // Fallback to system properties
            String arch = System.getProperty("os.arch");
            return arch != null ? arch : "Unknown";
            
        } catch (Exception e) {
            return "Unknown";
        }
    }
    
    /**
     * Get CPU model on Windows using wmic command
     */
    private static String getCPUModelWindows() {
        try {
            Process process = Runtime.getRuntime().exec("wmic cpu get name /value");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("Name=")) {
                        String cpuName = line.substring(5).trim();
                        if (!cpuName.isEmpty()) {
                            return cpuName;
                        }
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            // Ignore and fallback
        }
        return System.getProperty("os.arch", "Unknown");
    }
    
    /**
     * Get CPU model on Linux from /proc/cpuinfo
     */
    private static String getCPUModelLinux() {
        try {
            Process process = Runtime.getRuntime().exec("cat /proc/cpuinfo");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("model name")) {
                        String[] parts = line.split(":");
                        if (parts.length > 1) {
                            return parts[1].trim();
                        }
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            // Ignore and fallback
        }
        return System.getProperty("os.arch", "Unknown");
    }
    
    /**
     * Get CPU model on macOS using sysctl command
     */
    private static String getCPUModelMac() {
        try {
            Process process = Runtime.getRuntime().exec("sysctl -n machdep.cpu.brand_string");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String cpuName = reader.readLine();
                if (cpuName != null && !cpuName.trim().isEmpty()) {
                    return cpuName.trim();
                }
            }
            process.waitFor();
        } catch (Exception e) {
            // Ignore and fallback
        }
        return System.getProperty("os.arch", "Unknown");
    }
    
    /**
     * Get operating system name
     * @return OS name
     */
    public static String getOSName() {
        return System.getProperty("os.name") + " " + 
               System.getProperty("os.version") + " (" + 
               System.getProperty("os.arch") + ")";
    }
    
    /**
     * Calculate percentage
     * @param value Current value
     * @param max Maximum value
     * @return Percentage as double
     */
    public static double calculatePercentage(double value, double max) {
        if (max <= 0) return 0.0;
        return (value / max) * 100.0;
    }
    
    /**
     * Safe divide operation that prevents division by zero
     * @param numerator Numerator
     * @param denominator Denominator
     * @param defaultValue Value to return if denominator is 0
     * @return Result of division or default value
     */
    public static double safeDivide(double numerator, double denominator, double defaultValue) {
        if (denominator == 0.0) {
            return defaultValue;
        }
        return numerator / denominator;
    }
    
    /**
     * Clamp a value between min and max
     * @param value Value to clamp
     * @param min Minimum value
     * @param max Maximum value
     * @return Clamped value
     */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
    
    /**
     * Get detailed JVM memory information
     * @return JVMMemoryInfo object with min, max, and current values
     */
    public static JVMMemoryInfo getJVMMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        // Initial/minimum memory is typically close to the initial heap size
        // We'll estimate it as a fraction of max memory or use a reasonable default
        long minMemory = Math.min(totalMemory, maxMemory / 8);
        
        return new JVMMemoryInfo(minMemory, maxMemory, usedMemory);
    }
    
    /**
     * Format console message with proper color codes for server console
     * @param message Message with & color codes
     * @return Console-compatible colored message
     */
    public static String formatConsoleMessage(String message) {
        // Convert & codes to § codes first
        String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);
        
        // Then convert § codes to ANSI escape codes for proper console coloring
        return translateColorCodesForConsole(coloredMessage);
    }
    
    /**
     * Convert Minecraft color codes to ANSI escape codes for console output
     * @param message Message with § color codes
     * @return Message with ANSI escape codes for colored console output
     */
    public static String translateColorCodesForConsole(String message) {
        if (message == null) return "";
        
        // Convert Minecraft color codes to ANSI escape codes
        return message
            .replace("§0", "\u001B[30m")    // Black
            .replace("§1", "\u001B[34m")    // Dark Blue
            .replace("§2", "\u001B[32m")    // Dark Green
            .replace("§3", "\u001B[36m")    // Dark Aqua
            .replace("§4", "\u001B[31m")    // Dark Red
            .replace("§5", "\u001B[35m")    // Dark Purple
            .replace("§6", "\u001B[33m")    // Gold
            .replace("§7", "\u001B[37m")    // Gray
            .replace("§8", "\u001B[90m")    // Dark Gray
            .replace("§9", "\u001B[94m")    // Blue
            .replace("§a", "\u001B[92m")    // Green
            .replace("§b", "\u001B[96m")    // Aqua
            .replace("§c", "\u001B[91m")    // Red
            .replace("§d", "\u001B[95m")    // Light Purple
            .replace("§e", "\u001B[93m")    // Yellow
            .replace("§f", "\u001B[97m")    // White
            .replace("§l", "\u001B[1m")     // Bold
            .replace("§m", "\u001B[9m")     // Strikethrough
            .replace("§n", "\u001B[4m")     // Underline
            .replace("§o", "\u001B[3m")     // Italic
            .replace("§r", "\u001B[0m");    // Reset
    }
    
    /**
     * Format JVM memory information into human-readable string
     * @return Formatted JVM memory info
     */
    public static String formatMemoryInfo() {
        JVMMemoryInfo memInfo = getJVMMemoryInfo();
        return String.format("JVM RAM:\nMin: %s\nMax: %s\nIn-use: %s", 
            memInfo.getFormattedMinMemory(),
            memInfo.getFormattedMaxMemory(),
            memInfo.getFormattedUsedMemory());
    }
    
    /**
     * JVM Memory information data class
     */
    public static class JVMMemoryInfo {
        private final long minMemoryMB;
        private final long maxMemoryMB;
        private final long usedMemoryMB;
        
        public JVMMemoryInfo(long minMemory, long maxMemory, long usedMemory) {
            this.minMemoryMB = minMemory / 1024 / 1024;
            this.maxMemoryMB = maxMemory / 1024 / 1024;
            this.usedMemoryMB = usedMemory / 1024 / 1024;
        }
        
        public long getMinMemoryMB() { return minMemoryMB; }
        public long getMaxMemoryMB() { return maxMemoryMB; }
        public long getUsedMemoryMB() { return usedMemoryMB; }
        
        public String getFormattedMinMemory() { return minMemoryMB + " MB"; }
        public String getFormattedMaxMemory() { return maxMemoryMB + " MB"; }
        public String getFormattedUsedMemory() { return usedMemoryMB + " MB"; }
        
        @Override
        public String toString() {
            return String.format("JVM RAM:\nMin: %s\nMax: %s\nIn-use: %s", 
                getFormattedMinMemory(),
                getFormattedMaxMemory(),
                getFormattedUsedMemory());
        }
    }
}
