package online.chatchai.github.mcbench.scoring;

import online.chatchai.github.mcbench.Main;
import online.chatchai.github.mcbench.util.Util;

/**
 * New comprehensive scoring system for MCBench Pro
 * Formula: profileBasePoints - (recoverySeconds * penaltyPerSecond) + (jvmMb * 1.75)
 */
public class ScoreCalculator {
    
    private final Main plugin;
    
    public ScoreCalculator(Main plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Calculate final score for a benchmark result
     * 
     * @param profile The benchmark profile name
     * @param recoverySeconds The recovery time in seconds
     * @return ScoreResult containing the breakdown
     */
    public ScoreResult calculateScore(String profile, long recoverySeconds) {
        // Get configuration values from the correct path
        int profileBasePoints = plugin.getConfig().getInt(profile + ".profileBasePoints", 1000);
        int penaltyPerSecond = plugin.getConfig().getInt(profile + ".penaltyPerSecond", 10);
        
        // Debug logging to help verify the fix
        plugin.getLogger().info(String.format("Score calculation for profile '%s': basePoints=%d, penalty=%d/sec, recovery=%ds", 
            profile, profileBasePoints, penaltyPerSecond, recoverySeconds));
        
        // Get JVM RAM information
        Util.JVMMemoryInfo memoryInfo = Util.getJVMMemoryInfo();
        long jvmMB = memoryInfo.getMaxMemoryMB();
        
        // Calculate components
        int basePoints = profileBasePoints;
        int timePenalty = (int)(recoverySeconds * penaltyPerSecond);
        int ramBonus = (int)(jvmMB * 1.75);
        
        // Final score
        int finalScore = basePoints - timePenalty + ramBonus;
        
        return new ScoreResult(
            basePoints,
            timePenalty,
            ramBonus,
            finalScore,
            profileBasePoints,
            penaltyPerSecond,
            jvmMB,
            recoverySeconds
        );
    }
    
    /**
     * Get performance grade based on score
     */
    public String getPerformanceGrade(int score) {
        if (score >= 3000) return "S";
        if (score >= 2500) return "A+";
        if (score >= 2000) return "A";
        if (score >= 1500) return "B+";
        if (score >= 1000) return "B";
        if (score >= 500) return "C+";
        if (score >= 200) return "C";
        if (score >= 50) return "D";
        return "F";
    }
    
    /**
     * Get performance grade color code
     */
    public String getGradeColor(String grade) {
        switch (grade) {
            case "S": return "§d"; // Light purple
            case "A+": return "§a"; // Green
            case "A": return "§2"; // Dark green
            case "B+": return "§e"; // Yellow
            case "B": return "§6"; // Gold
            case "C+": return "§c"; // Red
            case "C": return "§4"; // Dark red
            case "D": return "§8"; // Dark gray
            case "F": return "§0"; // Black
            default: return "§7"; // Gray
        }
    }
    
    /**
     * Score calculation result
     */
    public static class ScoreResult {
        public final int basePoints;
        public final int timePenalty;
        public final int ramBonus;
        public final int finalScore;
        public final int profileBasePoints;
        public final int penaltyPerSecond;
        public final long jvmMB;
        public final long recoverySeconds;
        
        public ScoreResult(int basePoints, int timePenalty, int ramBonus, int finalScore, 
                          int profileBasePoints, int penaltyPerSecond, long jvmMB, long recoverySeconds) {
            this.basePoints = basePoints;
            this.timePenalty = timePenalty;
            this.ramBonus = ramBonus;
            this.finalScore = finalScore;
            this.profileBasePoints = profileBasePoints;
            this.penaltyPerSecond = penaltyPerSecond;
            this.jvmMB = jvmMB;
            this.recoverySeconds = recoverySeconds;
        }
    }
}
