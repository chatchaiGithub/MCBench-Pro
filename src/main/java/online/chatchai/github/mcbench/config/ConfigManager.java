package online.chatchai.github.mcbench.config;

import java.io.File;
import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import online.chatchai.github.mcbench.Main;
import online.chatchai.github.mcbench.util.Util;

/**
 * Configuration manager for MCBench Pro
 * Handles loading and accessing configuration values from config.yml and lang.yml
 */
public class ConfigManager {
    
    private final Main plugin;
    private FileConfiguration config;
    // Legacy lang.yml support (migration only)
    // Legacy holder kept to minimize diff history (not used anymore)
    // private FileConfiguration lang;
    
    public ConfigManager(Main plugin) {
        this.plugin = plugin;
        loadConfiguration();
    }
    
    /**
     * Load configuration files
     */
    private void loadConfiguration() {
        try {
            // Save default config files if they don't exist
            plugin.saveDefaultConfig();
            saveDefaultLang();
            
            // Load config.yml
            config = plugin.getConfig();
            
            // Legacy: migrate old lang.yml to new system if exists (optional)
            File legacyLang = new File(plugin.getDataFolder(), "lang.yml");
            if (legacyLang.exists()) {
                plugin.getLogger().info("Legacy lang.yml detected. Using new multi-language system (lang/en-US.yml). Legacy file will be ignored.");
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load configuration", e);
        }
    }
    
    /**
     * Save default lang.yml if it doesn't exist
     */
    private void saveDefaultLang() {
        // Ensure multi-language resources exist in plugins folder
        File outDir = new File(plugin.getDataFolder(), "lang");
        if (!outDir.exists()) outDir.mkdirs();
        File en = new File(outDir, "en-US.yml");
        File th = new File(outDir, "th-TH.yml");
        File cn = new File(outDir, "cn-CN.yml");
        if (!en.exists()) plugin.saveResource("lang/en-US.yml", false);
        if (!th.exists()) plugin.saveResource("lang/th-TH.yml", false);
        if (!cn.exists()) plugin.saveResource("lang/cn-CN.yml", false);
    }
    
    /**
     * Reload configuration files
     */
    public void reload() {
        plugin.reloadConfig();
        loadConfiguration();
        plugin.getLogger().info(getMessage("system.reloaded"));
    }
    
    /**
     * Get a message from lang.yml with placeholder replacement
     * @param key Message key
     * @param placeholders Placeholder key-value pairs
     * @return Formatted and colored message
     */
    public String getMessage(String key, String... placeholders) {
        // Use new multi-language helper first
        String message = Lang.get(key, placeholders);
        if (message == null || message.isEmpty()) {
            // fallback to built-in defaults as last resort
            String def = defaultMessageForKey(key);
            if (def == null) {
                return "";
            }
            // apply replacements to default string as well
            for (int i = 0; i + 1 < placeholders.length; i += 2) {
                def = def.replace(placeholders[i], String.valueOf(placeholders[i + 1]));
            }
            return Util.colorize(def);
        }
        return message;
    }

    /**
     * Built-in defaults for important messages when lang.yml is missing keys.
     * Keeps console clean and ensures critical warnings still show.
     */
    private String defaultMessageForKey(String key) {
        switch (key) {
            // Start warnings
            case "command.start.warning":
                return "&c&l⚠ WARNING ⚠&r";
            case "command.start.warningDetails":
                return "&cRunning MCBench Pro may cause temporary server instability and performance degradation.&r";
            case "command.start.warningResponsibility":
                return "&cThe plugin authors accept NO RESPONSIBILITY for any damage to game files, world state, or player data.&r";
            case "command.start.warningProfile":
                return "&7Profile: &e%profile%&r";
            case "command.start.warningMode":
                return "&7Mode: &e%mode%&r";
            case "command.start.warningConfirm":
                return "&eRun '/mcbench confirm' within 60 seconds to proceed, or '/mcbench cancel' to abort.&r";

            // Confirm/Cancel
            case "command.cancel.success":
                return "&aBenchmark cancelled.&r";
            case "command.confirm.expired":
                return "&cConfirmation expired. Benchmark cancelled.&r";
            case "command.confirm.success":
                return "&aConfirmation accepted. Starting benchmark shortly...&r";

            // Bypass notices
            case "command.bypassWarning":
                return "&c&lWARNING: Bypassing RAM requirements - proceeding with caution&r";
            case "command.bypassHint":
                return "&eUse '--bypass' flag to override RAM requirements if you understand the risks&r";

            // Status header
            case "benchmark.status.header":
                return "&a&l=== Server Status ===&r";
            case "benchmark.status.os":
                return "&7OS: &f%os%&r";
            case "benchmark.status.cpu":
                return "&7CPU: &f%cpu%&r";
            case "benchmark.status.ram":
                return "&7RAM: &f%used% MB &7/ &f%max% MB&r";
            case "benchmark.status.serverLoader":
                return "&7Server: &f%loader%&r";
            case "benchmark.status.version":
                return "&7Minecraft: &f%version%&r";
            case "benchmark.status.viaversion":
                return "&7ViaVersion: &f%viaversion%&r";
            case "benchmark.status.countdown":
                return "&eType '/mcbench confirm' within &c%seconds% &eseconds to start, or '/mcbench cancel' to abort.&r";

            // Benchmark lifecycle
            case "benchmark.starting":
                return "&aStarting benchmark...&r";
            case "benchmark.saveAll.executing":
                return "&eExecuting save-all before benchmark...&r";
            case "benchmark.saveAll.completed":
                return "&aWorld save completed.&r";
            case "benchmark.workloadStarted":
                return "&aWorkload started for profile &e%profile%&a.&r";
            case "benchmark.progress.inLoad":
                return "&7In-Load: &fTPS &a%tps% &7| MSPT &f%mspt% &7| RAM &f%ram%% &7| CPU &f%cpu%%&r";
            case "benchmark.progress.progressUpdate":
                return "&7Progress: &f%elapsed%s / %total%s &7(&f%percentage%%%&7) &7| TPS: &f%tps%&r";
            case "benchmark.workloadCompleted":
                return "&aWorkload phase completed. Monitoring recovery...&r";
            case "benchmark.recoveryCompleted":
                return "&aRecovery complete. Generating report...&r";
            case "benchmark.emergencyAbort":
                return "&cEmergency abort: MSPT exceeded threshold (&f%threshold%&cms) for &f%duration% &cseconds.&r";
            case "benchmark.timeoutAbort":
                return "&cGlobal timeout reached (&f%timeout%&cs). Aborting benchmark.&r";
            case "command.stop.success":
                return "&aBenchmark stopped.&r";

            // System messages
            case "system.reloaded":
                return "&aConfiguration reloaded.&r";
            case "system.enabled":
                return "&aMCBench Pro enabled &7(v%version%)&r";
            case "system.platformCheck":
                return "&7Platform: &f%paper% &7| Java: &f%java%&r";
            case "system.disabled":
                return "&eMCBench Pro disabled.&r";

            // Command feedback
            case "command.noPermission":
                return "&cYou do not have permission to use this command.&r";
            case "command.invalidSyntax":
                return "&cInvalid syntax. Use &e/mcbench help&r";
            case "command.benchmarkRunning":
                return "&cA benchmark is already running.&r";
            case "command.confirmationPending":
                return "&eA benchmark confirmation is already pending.&r";
            case "command.unknownProfile":
                return "&cUnknown profile: &f%profile%&r";
            case "command.noBenchmarkRunning":
                return "&eNo benchmark is currently running.&r";

            // Help
            case "command.help.header":
                return "&aMCBench Pro Commands:&r";
            case "command.help.start":
                return "&e/mcbench start <profile> [safe] [--bypass] &7- Start a benchmark&r";
            case "command.help.stop":
                return "&e/mcbench stop &7- Stop the running benchmark&r";
            case "command.help.confirm":
                return "&e/mcbench confirm &7- Confirm starting the benchmark&r";
            case "command.help.cancel":
                return "&e/mcbench cancel &7- Cancel pending confirmation&r";
            case "command.help.check":
                return "&e/mcbench check &7- Run diagnostics&r";
            case "command.help.reload":
                return "&e/mcbench reload &7- Reload config and language&r";
            case "command.help.profiles":
                return "&7Profiles: minimum, normal, extreme&r";
            case "command.help.safeMode":
                return "&7Add 'safe' to kick players and block joins during benchmark&r";
            case "command.help.bypass":
                return "&7Use '--bypass' to skip RAM checks (console only)&r";

            // RAM warnings
            case "ramWarning.minimum":
                return "&cInsufficient RAM for Minimum profile. &7Current: &f%current% &7Required: &f%required%&r";
            case "ramWarning.normal":
                return "&cInsufficient RAM for Normal profile. &7Current: &f%current% &7Required: &f%required%&r";
            case "ramWarning.extreme":
                return "&cInsufficient RAM for Extreme profile. &7Current: &f%current% &7Required: &f%required%&r";

            // Report
            case "report.export.success":
                return "&aReport exported: &f%file%&r";
            case "report.header":
                return "&a&l=== Benchmark Report ===&r";
            case "report.profile":
                return "&7Profile: &f%profile%&r";
            case "report.mode":
                return "&7Mode: &f%mode%&r";
            case "report.duration":
                return "&7Workload Duration: &f%duration%s&r";
            case "report.baseline.header":
                return "&aBaseline Metrics:&r";
            case "report.baseline.tps":
                return "&7TPS: &f%tps%&r";
            case "report.baseline.mspt":
                return "&7MSPT: &f%mspt%&r";
            case "report.baseline.cpu":
                return "&7CPU: &f%cpu%%%&r";
            case "report.baseline.ram":
                return "&7RAM: &f%ram%%%&r";
            case "report.afterload.header":
                return "&aAfter Load Metrics:&r";
            case "report.afterload.tps":
                return "&7TPS: &f%tps%&r";
            case "report.afterload.mspt":
                return "&7MSPT: &f%mspt%&r";
            case "report.afterload.cpu":
                return "&7CPU: &f%cpu%%%&r";
            case "report.afterload.ram":
                return "&7RAM: &f%ram%%%&r";
            case "report.recovery.time":
                return "&7Recovery Time: &f%time%&r";
            case "report.recovery.score":
                return "&7Final Score: &a%score% &7(&f%formula%&7)&r";
            case "report.recovery.bonus":
                return "&7RAM Bonus: &f%bonus% &7(from %jvmMb% MB * 1.75)&r";
            case "report.recovery.benchmarkPoint":
                return "&7Benchmark Point: &f%point%&r";
            case "report.system.header":
                return "&aSystem Information:&r";
            case "report.system.java":
                return "&7Java: &f%java%&r";
            case "report.system.minecraft":
                return "&7Minecraft: &f%minecraft%&r";
            case "report.system.jvmRam":
                return "&7%jvm%&r";
            case "report.system.hardware":
                return "&7Hardware: &f%hardware%&r";
            case "report.analysis.header":
                return "&aAnalysis:&r";
            case "report.analysis.chunks":
                return "&7Loaded Chunks: &f%chunks%&r";
            case "report.analysis.entities":
                return "&7Total Entities: &f%entities%&r";
            case "report.analysis.topChunks":
                return "&7Top Entity-Dense Chunks:&r";
            case "report.recommendations.header":
                return "&aRecommendations:&r";
            case "report.recommendations.noRecommendations":
                return "&7Your server looks optimal for this test.&r";

            default:
                return null;
        }
    }
    
    // Configuration getters
    public int getGlobalTimeoutSeconds() {
        return config.getInt("settings.globalTimeoutSeconds", 300);
    }
    
    public double getBaseScore() {
        return config.getDouble("settings.baseScore", 1000.0);
    }
    
    public double getEmergencyMsptThreshold() {
        return config.getDouble("settings.emergency.msptThreshold", 1000.0);
    }
    
    public int getEmergencyDurationSeconds() {
        return config.getInt("settings.emergency.durationSeconds", 10);
    }
    
    public boolean isExportEnabled() {
        return config.getBoolean("settings.export.enableResultFile", true);
    }
    
    public String getExportFormat() {
        return config.getString("settings.export.resultFileFormat", "txt");
    }
    
    public String getSafeModeKickMessage() {
        return config.getString("settings.safeMode.kickMessage", 
            "MCBench Pro — server benchmarking in progress");
    }
    
    public boolean shouldClearInventories() {
        return config.getBoolean("settings.safeMode.clearInventories", true);
    }
    
    public boolean shouldClearDroppedItems() {
        return config.getBoolean("settings.safeMode.clearDroppedItems", true);
    }
    
    public boolean shouldPerformWorldCleanup() {
        return config.getBoolean("settings.safeMode.performWorldCleanup", false);
    }
    
    /**
     * Get profile configuration
     * @param profileName Profile name (minimum, normal, extreme)
     * @return ProfileConfig object or null if not found
     */
    public ProfileConfig getProfile(String profileName) {
        ConfigurationSection section = config.getConfigurationSection("profiles." + profileName);
        if (section == null) {
            return null;
        }
        
        return new ProfileConfig(
            section.getInt("durationSeconds", 120),
            section.getDouble("intensityMultiplier", 1.0),
            section.getInt("tickInterval", 1),
            section.getInt("loopCountPerTick", 1000000),
            section.getDouble("emergencyMsptThreshold", 1000.0)
        );
    }
    
    /**
     * Get available profile names
     * @return Array of profile names
     */
    public String[] getAvailableProfiles() {
        ConfigurationSection profilesSection = config.getConfigurationSection("profiles");
        if (profilesSection == null) {
            return new String[]{"minimum", "normal", "extreme"};
        }
        java.util.Set<String> keys = profilesSection.getKeys(false);
        return keys.toArray(new String[keys.size()]);
    }
    
    // Recommendation thresholds
    public int getHighEntityCountThreshold() {
        return config.getInt("recommendations.thresholds.highEntityCount", 1000);
    }
    
    public int getHighChunkCountThreshold() {
        return config.getInt("recommendations.thresholds.highChunkCount", 500);
    }
    
    public double getLowTpsThreshold() {
        return config.getDouble("recommendations.thresholds.lowTps", 18.0);
    }
    
    public double getHighMsptThreshold() {
        return config.getDouble("recommendations.thresholds.highMspt", 30.0);
    }
    
    public double getHighRamUsageThreshold() {
        return config.getDouble("recommendations.thresholds.highRamUsage", 80.0);
    }
    
    public double getHighCpuUsageThreshold() {
        return config.getDouble("recommendations.thresholds.highCpuUsage", 85.0);
    }
    
    // Benchmark Point calculation
    public double getBenchmarkPointDivisor() {
        return config.getDouble("benchmarkPoint.divisor", 50000.0);
    }
    
    public double getTargetTps() {
        return config.getDouble("benchmarkPoint.targetTps", 20.0);
    }
    
    public double getTargetMspt() {
        return config.getDouble("benchmarkPoint.targetMspt", 25.0);
    }

    // Scoring configuration
    public int getProfileBasePoints(String profile) {
        return config.getInt("scoring." + profile + ".profileBasePoints", 1000);
    }

    public int getPenaltyPerSecond(String profile) {
        return config.getInt("scoring." + profile + ".penaltyPerSecond", 10);
    }
    
    /**
     * Profile configuration data class
     */
    public static class ProfileConfig {
        private final int durationSeconds;
        private final double intensityMultiplier;
        private final int tickInterval;
        private final int loopCountPerTick;
        private final double emergencyMsptThreshold;
        
        public ProfileConfig(int durationSeconds, double intensityMultiplier, 
                           int tickInterval, int loopCountPerTick, 
                           double emergencyMsptThreshold) {
            this.durationSeconds = durationSeconds;
            this.intensityMultiplier = intensityMultiplier;
            this.tickInterval = tickInterval;
            this.loopCountPerTick = loopCountPerTick;
            this.emergencyMsptThreshold = emergencyMsptThreshold;
        }
        
        public int getDurationSeconds() { return durationSeconds; }
        public double getIntensityMultiplier() { return intensityMultiplier; }
        public int getTickInterval() { return tickInterval; }
        public int getLoopCountPerTick() { return loopCountPerTick; }
        public double getEmergencyMsptThreshold() { return emergencyMsptThreshold; }
    }
}
