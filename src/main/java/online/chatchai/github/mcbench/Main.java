package online.chatchai.github.mcbench;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import online.chatchai.github.mcbench.benchmark.BenchmarkManager;
import online.chatchai.github.mcbench.command.MCBenchCommand;
import online.chatchai.github.mcbench.config.ConfigManager;
import online.chatchai.github.mcbench.config.Lang;
import online.chatchai.github.mcbench.metrics.MetricSampler;
import online.chatchai.github.mcbench.util.Util;

/**
 * MCBench Pro - Professional-grade controlled server benchmark plugin
 * Main plugin class that handles initialization and shutdown
 */
public class Main extends JavaPlugin implements Listener {
    
    private ConfigManager configManager;
    private BenchmarkManager benchmarkManager;
    private MetricSampler metricSampler;
    
    @Override
    public void onEnable() {
        try {
            // Check platform compatibility
            if (!checkPlatformCompatibility()) {
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            // Initialize configuration
            configManager = new ConfigManager(this);
            // Initialize i18n language system
            String langCode = getConfig().getString("settings.language", "en-US");
            Lang.init(this, langCode);
            
            // Initialize metric sampler
            metricSampler = new MetricSampler(this);
            
            // Initialize benchmark manager
            benchmarkManager = new BenchmarkManager(this, configManager, metricSampler);
            
            // Register commands
            registerCommands();
            
            // Register event listeners
            getServer().getPluginManager().registerEvents(this, this);
            
            // Log successful startup
            String enabledMessage = configManager.getMessage("system.enabled", 
                "%version%", getDescription().getVersion());
            getLogger().info(Util.translateColorCodesForConsole(enabledMessage));
            
            String platformMessage = configManager.getMessage("system.platformCheck",
                "%paper%", Bukkit.getVersion(),
                "%java%", System.getProperty("java.version"));
            getLogger().info(Util.translateColorCodesForConsole(platformMessage));
                
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable MCBench Pro", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        try {
            // Stop any running benchmark
            if (benchmarkManager != null) {
                benchmarkManager.forceStop();
            }
            
            // Shutdown metric sampler
            if (metricSampler != null) {
                metricSampler.shutdown();
            }
            
            String disabledMessage = configManager != null ? 
                configManager.getMessage("system.disabled") : "MCBench Pro disabled";
            getLogger().info(Util.translateColorCodesForConsole(disabledMessage));
                
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Error during plugin shutdown", e);
        }
    }
    
    /**
     * Check if the server platform is compatible with MCBench Pro
     * @return true if compatible, false otherwise
     */
    private boolean checkPlatformCompatibility() {
        // Check Java version (requires 21+)
        try {
            String javaVersion = System.getProperty("java.version");
            int majorVersion = Util.getJavaMajorVersion();
            if (majorVersion < 21) {
                getLogger().log(java.util.logging.Level.SEVERE, "Java 21+ required. Current: {0}", javaVersion);
                return false;
            }
        } catch (Exception e) {
            getLogger().log(java.util.logging.Level.SEVERE, "Failed to check Java version: {0}", e.getMessage());
            return false;
        }
        
        // Check Minecraft version (requires 1.21+)
        try {
            String serverVersion = Bukkit.getVersion();
            if (!Util.isMinecraft121OrNewer(serverVersion)) {
                getLogger().log(java.util.logging.Level.SEVERE, "Minecraft 1.21+ required. Current: {0}", serverVersion);
                return false;
            }
        } catch (Exception e) {
            getLogger().log(java.util.logging.Level.SEVERE, "Failed to check server version: {0}", e.getMessage());
            return false;
        }
        
        // Check if Paper/Pufferfish APIs are available
        try {
            // Try to access Paper-specific TPS methods
            Class.forName("org.bukkit.Bukkit");
            Bukkit.class.getMethod("getTPS");
            Bukkit.class.getMethod("getAverageTickTime");
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            getLogger().severe("Paper/Pufferfish API not available. This plugin requires Paper-based servers.");
            return false;
        }
        
        return true;
    }
    
    /**
     * Register plugin commands
     */
    private void registerCommands() {
        PluginCommand command = getCommand("mcbench");
        if (command != null) {
            MCBenchCommand mcBenchCommand = new MCBenchCommand(this, configManager, benchmarkManager);
            command.setExecutor(mcBenchCommand);
            command.setTabCompleter(mcBenchCommand);
        }
    }
    
    // Getters for other components
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public BenchmarkManager getBenchmarkManager() {
        return benchmarkManager;
    }
    
    public MetricSampler getMetricSampler() {
        return metricSampler;
    }
    
    /**
     * Handle player login event - block players during safe mode benchmark
     */
    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (benchmarkManager != null && benchmarkManager.isBenchmarkRunning() && benchmarkManager.isSafeMode()) {
            String kickMessage = configManager.getSafeModeKickMessage();
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, kickMessage);
        }
    }
}
