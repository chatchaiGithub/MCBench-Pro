package online.chatchai.github.mcbench.verification;

import online.chatchai.github.mcbench.Main;
import online.chatchai.github.mcbench.util.Util;

/**
 * RAM requirement verification system
 */
public class RAMVerifier {
    
    private final Main plugin;
    
    public RAMVerifier(Main plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Check if the server has enough RAM for the specified profile
     * 
     * @param profile The benchmark profile to check
     * @return VerificationResult containing the check results
     */
    public VerificationResult verifyRAMRequirements(String profile) {
        // Get current JVM RAM
        Util.JVMMemoryInfo memoryInfo = Util.getJVMMemoryInfo();
        long currentRAM = memoryInfo.getMaxMemoryMB();
        
        // Get minimum RAM requirement for profile
        long requiredRAM = plugin.getConfig().getLong("minimumRamPerProfile." + profile, 1024);
        
        boolean sufficient = currentRAM >= requiredRAM;
        
        return new VerificationResult(
            currentRAM,
            requiredRAM,
            sufficient,
            profile
        );
    }
    
    /**
     * Get RAM requirement message key for the profile
     */
    public String getRAMMessageKey(String profile) {
        return "ramWarning." + profile;
    }
    
    /**
     * RAM verification result
     */
    public static class VerificationResult {
        public final long currentRAM;
        public final long requiredRAM;
        public final boolean sufficient;
        public final String profile;
        
        public VerificationResult(long currentRAM, long requiredRAM, boolean sufficient, String profile) {
            this.currentRAM = currentRAM;
            this.requiredRAM = requiredRAM;
            this.sufficient = sufficient;
            this.profile = profile;
        }
        
        /**
         * Get formatted RAM values for display
         */
        public String getFormattedCurrentRAM() {
            return formatRAM(currentRAM);
        }
        
        public String getFormattedRequiredRAM() {
            return formatRAM(requiredRAM);
        }
        
        private String formatRAM(long mb) {
            if (mb >= 1024) {
                return String.format("%.1f GB", mb / 1024.0);
            } else {
                return mb + " MB";
            }
        }
    }
}
