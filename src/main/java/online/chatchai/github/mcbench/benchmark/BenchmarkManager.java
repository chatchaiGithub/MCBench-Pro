package online.chatchai.github.mcbench.benchmark;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import online.chatchai.github.mcbench.Main;
import online.chatchai.github.mcbench.analysis.ChunkEntityAnalyzer;
import online.chatchai.github.mcbench.config.ConfigManager;
import online.chatchai.github.mcbench.metrics.MetricSampler;
import online.chatchai.github.mcbench.recommendation.RecommendationEngine;
import online.chatchai.github.mcbench.report.ReportExporter;
import online.chatchai.github.mcbench.scoring.ScoreCalculator;
import online.chatchai.github.mcbench.util.RunLogger;
import online.chatchai.github.mcbench.util.Util;

/**
 * Main benchmark manager for MCBench Pro
 * Orchestrates the entire benchmark process including workload, recovery monitoring, and reporting
 */
public class BenchmarkManager {
    
    private final Main plugin;
    private final ConfigManager configManager;
    private final MetricSampler metricSampler;
    private final ChunkEntityAnalyzer entityAnalyzer;
    private final RecommendationEngine recommendationEngine;
    private final ReportExporter reportExporter;
    private final RunLogger runLogger;
    private final ScoreCalculator scoreCalculator;
    
    // Benchmark state
    private BenchmarkState currentState = BenchmarkState.IDLE;
    private ConfigManager.ProfileConfig currentProfile;
    private boolean safeMode = false;
    private String currentProfileName;
    
    // Confirmation system
    private BukkitTask confirmationTask;
    private final AtomicBoolean awaitingConfirmation = new AtomicBoolean(false);
    private final AtomicInteger confirmationCountdown = new AtomicInteger(60);
    
    // Benchmark tasks and monitoring
    private WorkloadTask workloadTask;
    private BukkitTask recoveryMonitorTask;
    private BukkitTask progressLogTask;
    private BukkitTask emergencyMonitorTask;
    private BukkitTask globalTimeoutTask;
    
    // Metrics and timing
    private MetricSampler.MetricSnapshot baselineMetrics;
    private MetricSampler.MetricSnapshot afterLoadMetrics;
    private long workloadStartTime;
    private long workloadEndTime;
    private long recoveryStartTime;
    private long recoveryEndTime;
    private long benchmarkStartTime;
    
    // Emergency monitoring
    private long emergencyMsptStartTime = 0;
    private final AtomicInteger emergencyMsptCount = new AtomicInteger(0);
    
    public BenchmarkManager(Main plugin, ConfigManager configManager, MetricSampler metricSampler) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.metricSampler = metricSampler;
        this.entityAnalyzer = new ChunkEntityAnalyzer(plugin);
        this.recommendationEngine = new RecommendationEngine(configManager);
        this.reportExporter = new ReportExporter(plugin, configManager);
        this.runLogger = new RunLogger(plugin);
        this.scoreCalculator = new ScoreCalculator(plugin);
    }
    
    /**
     * Start benchmark confirmation process
     */
    public boolean startBenchmarkConfirmation(String profileName, boolean safeMode) {
        if (awaitingConfirmation.get() || currentState != BenchmarkState.IDLE) {
            return false;
        }
        
        ConfigManager.ProfileConfig profile = configManager.getProfile(profileName);
        if (profile == null) {
            return false;
        }
        
        this.currentProfile = profile;
        this.currentProfileName = profileName;
        this.safeMode = safeMode;
        this.awaitingConfirmation.set(true);
        this.confirmationCountdown.set(60);
        
        // Show warning messages
        showBenchmarkWarning();
        
        // Start confirmation countdown
        startConfirmationCountdown();
        
        return true;
    }
    
    /**
     * Show benchmark warning messages (console only)
     */
    private void showBenchmarkWarning() {
        String mode = safeMode ? "Safe Mode" : "Normal Mode";
        
        // Console warning only - no player broadcasts
        plugin.getLogger().warning(Util.formatConsoleMessage(configManager.getMessage("command.start.warning")));
        plugin.getLogger().warning(Util.formatConsoleMessage(configManager.getMessage("command.start.warningDetails")));
        plugin.getLogger().warning(Util.formatConsoleMessage(configManager.getMessage("command.start.warningResponsibility")));
        plugin.getLogger().info(Util.formatConsoleMessage(configManager.getMessage("command.start.warningProfile", 
            "%profile%", currentProfileName)));
        plugin.getLogger().info(Util.formatConsoleMessage(configManager.getMessage("command.start.warningMode", 
            "%mode%", mode)));
        plugin.getLogger().info(Util.formatConsoleMessage(configManager.getMessage("command.start.warningConfirm")));
    }
    
    /**
     * Start confirmation countdown with server status display
     */
    private void startConfirmationCountdown() {
        confirmationTask = new BukkitRunnable() {
            @Override
            public void run() {
                int countdown = confirmationCountdown.decrementAndGet();
                
                if (countdown <= 0) {
                    // Timeout - cancel confirmation
                    cancelConfirmation();
                    plugin.getLogger().info(Util.formatConsoleMessage(configManager.getMessage("command.confirm.expired")));
                    cancel();
                    return;
                }
                
                // Show server status every 10 seconds and during final countdown
                if (countdown % 10 == 0 || countdown <= 5) {
                    showServerStatus(countdown);
                }
                
                // Audible countdown for final 5 seconds
                if (countdown <= 5) {
                    plugin.getLogger().log(Level.INFO, () -> "Confirmation countdown: " + countdown + " seconds");
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Run every second
    }
    
    /**
     * Show server status with live information (console only)
     */
    private void showServerStatus(int countdown) {
        MetricSampler.MetricSnapshot metrics = metricSampler.createSnapshot();

        String statusMessage = String.join("\n",
            Util.formatConsoleMessage(configManager.getMessage("benchmark.status.header")),
            Util.formatConsoleMessage(configManager.getMessage("benchmark.status.os", "%os%", Util.getOSName())),
            Util.formatConsoleMessage(configManager.getMessage("benchmark.status.cpu", "%cpu%", Util.getCPUModel())),
            Util.formatConsoleMessage(configManager.getMessage("benchmark.status.ram",
                "%used%", String.valueOf(metrics.getUsedMemoryMB()),
                "%max%", String.valueOf(metrics.getMaxMemoryMB()))),
            Util.formatConsoleMessage(configManager.getMessage("benchmark.status.serverLoader", "%loader%", Util.getServerLoader())),
            Util.formatConsoleMessage(configManager.getMessage("benchmark.status.version", "%version%", Bukkit.getVersion())),
            Util.formatConsoleMessage(configManager.getMessage("benchmark.status.viaversion", "%viaversion%", String.valueOf(Util.hasViaVersion())))
        );

        plugin.getLogger().info(statusMessage);
        plugin.getLogger().info(Util.formatConsoleMessage(configManager.getMessage("benchmark.status.countdown",
            "%seconds%", String.valueOf(countdown))));
    }
    
    /**
     * Confirm and start the benchmark
     */
    public boolean confirmBenchmark() {
        if (!awaitingConfirmation.get()) {
            return false;
        }
        
        cancelConfirmationTask();
        
        plugin.getLogger().info(Util.formatConsoleMessage(configManager.getMessage("command.confirm.success")));
        
        // Start benchmark after 3 seconds
        Bukkit.getScheduler().runTaskLater(plugin, this::executeBenchmark, 60L);
        
        return true;
    }
    
    /**
     * Cancel pending confirmation
     */
    public boolean cancelConfirmation() {
        if (!awaitingConfirmation.get()) {
            return false;
        }
        
        cancelConfirmationTask();
        plugin.getLogger().info(Util.formatConsoleMessage(configManager.getMessage("command.cancel.success")));
        
        return true;
    }
    
    /**
     * Cancel confirmation countdown task
     */
    private void cancelConfirmationTask() {
        awaitingConfirmation.set(false);
        if (confirmationTask != null) {
            confirmationTask.cancel();
            confirmationTask = null;
        }
    }
    
    /**
     * Execute the actual benchmark
     */
    private void executeBenchmark() {
        try {
            currentState = BenchmarkState.PREPARING;
            benchmarkStartTime = System.currentTimeMillis();
            
            plugin.getLogger().info(Util.formatConsoleMessage(configManager.getMessage("benchmark.starting")));
            
            // Execute save-all before benchmark
            executeSaveAll();
            
            // Record baseline metrics
            baselineMetrics = metricSampler.createSnapshot();
            
            // Execute safe mode operations if requested
            if (safeMode) {
                executeSafeModeOperations();
            }
            
            // Start workload
            startWorkload();
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to execute benchmark", e);
            currentState = BenchmarkState.IDLE;
        }
    }
    
    /**
     * Execute save-all command before benchmark
     */
    private void executeSaveAll() {
        plugin.getLogger().info(Util.formatConsoleMessage(configManager.getMessage("benchmark.saveAll.executing")));
        
        // Execute save-all command
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "save-all");
        
        // Log to runs.log with timestamp
        String timestamp = java.time.ZonedDateTime.now().format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        runLogger.logSaveAll(timestamp);
        
        plugin.getLogger().info(Util.formatConsoleMessage(configManager.getMessage("benchmark.saveAll.completed")));
    }
    
    /**
     * Execute safe mode operations (kick players only)
     */
    private void executeSafeModeOperations() {
        String kickMessage = configManager.getSafeModeKickMessage();
        
        // Kick all players
        List<Player> playersToKick = new ArrayList<>(Bukkit.getOnlinePlayers());
        for (Player player : playersToKick) {
            player.kickPlayer(kickMessage);
        }
        
    plugin.getLogger().log(Level.INFO, "Safe mode: Kicked {0} players", playersToKick.size());
        plugin.getLogger().info("Safe mode: Players will be blocked from joining during benchmark");
        
        // Only clear dropped items if configured (keep player inventories intact)
        if (configManager.shouldClearDroppedItems()) {
            // Clear dropped items from all worlds
            Bukkit.getWorlds().forEach(world -> {
                world.getEntities().stream()
                    .filter(entity -> entity.getType().name().equals("DROPPED_ITEM"))
                    .forEach(entity -> entity.remove());
            });
            plugin.getLogger().info("Safe mode: Cleared dropped items from all worlds");
        }
    }
    
    /**
     * Start the CPU workload
     */
    private void startWorkload() {
        currentState = BenchmarkState.WORKLOAD;
        workloadStartTime = System.currentTimeMillis();
        
        plugin.getLogger().info(Util.formatConsoleMessage(configManager.getMessage("benchmark.workloadStarted", 
            "%profile%", currentProfileName)));
        
        // Create and start workload task
        workloadTask = new WorkloadTask(plugin, currentProfile, this);
        workloadTask.runTaskTimer(plugin, 0L, currentProfile.getTickInterval());
        
        // Start progress logging
        startProgressLogging();
        
        // Start emergency monitoring
        startEmergencyMonitoring();
        
        // Start global timeout monitoring
        startGlobalTimeoutMonitoring();
    }
    
    /**
     * Start progress logging task
     */
    private void startProgressLogging() {
        progressLogTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (currentState != BenchmarkState.WORKLOAD) {
                    cancel();
                    return;
                }
                
                MetricSampler.MetricSnapshot current = metricSampler.createSnapshot();
                
                // Log every 2 seconds
                plugin.getLogger().info(Util.formatConsoleMessage(configManager.getMessage("benchmark.progress.inLoad",
                    "%tps%", Util.formatDecimal(current.getTps()),
                    "%mspt%", Util.formatDecimal(current.getMspt()),
                    "%ram%", Util.formatDecimal(current.getMemoryUsage()),
                    "%cpu%", Util.formatDecimal(current.getSystemCpu()))));
                
                // Progress update every 10 seconds
                if (workloadTask != null) {
                    long elapsed = workloadTask.getElapsedSeconds();
                    if (elapsed % 10 == 0) {
                        plugin.getLogger().info(Util.formatConsoleMessage(configManager.getMessage("benchmark.progress.progressUpdate",
                            "%elapsed%", String.valueOf(elapsed),
                            "%total%", String.valueOf(workloadTask.getTotalDurationSeconds()),
                            "%percentage%", Util.formatDecimal(workloadTask.getCompletionPercentage()),
                            "%tps%", Util.formatDecimal(current.getTps()))));
                    }
                }
            }
        }.runTaskTimer(plugin, 40L, 40L); // Every 2 seconds
    }
    
    /**
     * Start emergency monitoring for high MSPT
     */
    private void startEmergencyMonitoring() {
        emergencyMonitorTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (currentState == BenchmarkState.IDLE) {
                    cancel();
                    return;
                }
                
                double currentMspt = metricSampler.getMSPT();
                double threshold = currentProfile.getEmergencyMsptThreshold();
                
                if (currentMspt > threshold) {
                    if (emergencyMsptStartTime == 0) {
                        emergencyMsptStartTime = System.currentTimeMillis();
                    }
                    
                    long emergencyDuration = (System.currentTimeMillis() - emergencyMsptStartTime) / 1000;
                    if (emergencyDuration >= configManager.getEmergencyDurationSeconds()) {
                        // Trigger emergency abort
                        emergencyAbort(currentMspt, emergencyDuration);
                        cancel();
                        
                    }
                } else {
                    emergencyMsptStartTime = 0; // Reset timer
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Check every second
    }
    
    /**
     * Start global timeout monitoring
     */
    private void startGlobalTimeoutMonitoring() {
        int timeoutSeconds = configManager.getGlobalTimeoutSeconds();
        globalTimeoutTask = new BukkitRunnable() {
            @Override
            public void run() {
                timeoutAbort(timeoutSeconds);
            }
        }.runTaskLater(plugin, timeoutSeconds * 20L);
    }
    
    /**
     * Called when workload phase completes
     */
    public void onWorkloadCompleted() {
        currentState = BenchmarkState.RECOVERY;
        workloadEndTime = System.currentTimeMillis();
        recoveryStartTime = System.currentTimeMillis();
        
        plugin.getLogger().info(Util.formatConsoleMessage(configManager.getMessage("benchmark.workloadCompleted")));
        
        // Record metrics after workload
        afterLoadMetrics = metricSampler.createSnapshot();
        
        // Cancel progress logging
        if (progressLogTask != null) {
            progressLogTask.cancel();
        }
        
        // Start recovery monitoring
        startRecoveryMonitoring();
    }
    
    /**
     * Start recovery monitoring (wait for TPS/MSPT to return to normal)
     */
    private void startRecoveryMonitoring() {
        recoveryMonitorTask = new BukkitRunnable() {
            @Override
            public void run() {
                double currentTps = metricSampler.getTPS();
                double currentMspt = metricSampler.getMSPT();
                
                double targetTps = configManager.getTargetTps();
                double targetMspt = configManager.getTargetMspt();
                
                if (currentTps >= targetTps && currentMspt <= targetMspt) {
                    // Recovery complete
                    onRecoveryCompleted();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Check every second
    }
    
    /**
     * Called when recovery phase completes
     */
    private void onRecoveryCompleted() {
        currentState = BenchmarkState.REPORTING;
        recoveryEndTime = System.currentTimeMillis();
        
        plugin.getLogger().info(Util.formatConsoleMessage(configManager.getMessage("benchmark.recoveryCompleted")));
        
        // Cancel all monitoring tasks
        cancelAllTasks();
        
        // Generate and display final report
        generateFinalReport();
        
        currentState = BenchmarkState.IDLE;
    }
    
    /**
     * Handle workload execution error
     */
    public void onWorkloadError(Exception error) {
        plugin.getLogger().log(Level.SEVERE, "Workload error occurred", error);
        forceStop();
    }
    
    /**
     * Emergency abort due to high MSPT
     */
    private void emergencyAbort(double mspt, long duration) {
        plugin.getLogger().severe(configManager.getMessage("benchmark.emergencyAbort",
            "%threshold%", Util.formatDecimal(mspt),
            "%duration%", String.valueOf(duration)));
        
        forceStop();
    }
    
    /**
     * Timeout abort due to exceeding global timeout
     */
    private void timeoutAbort(int timeoutSeconds) {
        plugin.getLogger().severe(configManager.getMessage("benchmark.timeoutAbort",
            "%timeout%", String.valueOf(timeoutSeconds)));
        
        forceStop();
    }
    
    /**
     * Force stop the benchmark
     */
    public void forceStop() {
        if (currentState == BenchmarkState.IDLE) {
            return;
        }
        
        plugin.getLogger().info(Util.formatConsoleMessage(configManager.getMessage("command.stop.success")));
        
        // Cancel confirmation if pending
        cancelConfirmationTask();
        
        // Cancel all tasks
        cancelAllTasks();
        
        // Generate aborted report if we have baseline metrics
        if (baselineMetrics != null) {
            generateAbortedReport();
        }
        
        currentState = BenchmarkState.IDLE;
        resetBenchmarkState();
    }
    
    /**
     * Cancel all running tasks
     */
    private void cancelAllTasks() {
        if (workloadTask != null) {
            workloadTask.stopWorkload();
            workloadTask = null;
        }
        
        if (progressLogTask != null) {
            progressLogTask.cancel();
            progressLogTask = null;
        }
        
        if (recoveryMonitorTask != null) {
            recoveryMonitorTask.cancel();
            recoveryMonitorTask = null;
        }
        
        if (emergencyMonitorTask != null) {
            emergencyMonitorTask.cancel();
            emergencyMonitorTask = null;
        }
        
        if (globalTimeoutTask != null) {
            globalTimeoutTask.cancel();
            globalTimeoutTask = null;
        }
    }
    
    /**
     * Reset benchmark state variables
     */
    private void resetBenchmarkState() {
        currentProfile = null;
        currentProfileName = null;
        safeMode = false;
        baselineMetrics = null;
        afterLoadMetrics = null;
        workloadStartTime = 0;
        workloadEndTime = 0;
        recoveryStartTime = 0;
        recoveryEndTime = 0;
        benchmarkStartTime = 0;
        emergencyMsptStartTime = 0;
        emergencyMsptCount.set(0);
    }
    
    /**
     * Generate final benchmark report
     */
    private void generateFinalReport() {
        try {
            BenchmarkResult result = createBenchmarkResult(false);
            displayBenchmarkReport(result);
            
            // Export to file if enabled
            if (configManager.isExportEnabled()) {
                reportExporter.exportReport(result);
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to generate benchmark report", e);
        }
    }
    
    /**
     * Generate aborted benchmark report
     */
    private void generateAbortedReport() {
        try {
            BenchmarkResult result = createBenchmarkResult(true);
            displayBenchmarkReport(result);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to generate aborted benchmark report", e);
        }
    }
    
    /**
     * Create benchmark result object
     */
    private BenchmarkResult createBenchmarkResult(boolean aborted) {
        // Calculate timing
        double workloadDuration = workloadEndTime > 0 ? 
            (workloadEndTime - workloadStartTime) / 1000.0 : 0.0;
        double recoveryDuration = recoveryEndTime > 0 ? 
            (recoveryEndTime - recoveryStartTime) / 1000.0 : 0.0;
        
        // Calculate score (includes JVM RAM bonus via ScoreCalculator)
        long recoverySecsRounded = (long) Math.round(recoveryDuration);
        int finalScore = scoreCalculator
            .calculateScore(currentProfileName != null ? currentProfileName : "normal", recoverySecsRounded)
            .finalScore;
        double score = finalScore;
        double benchmarkPoint = calculateBenchmarkPoint(recoveryDuration);
        
        // Get system information
        SystemInfo systemInfo = collectSystemInfo();
        
        // Analyze chunks and entities
        ChunkEntityAnalyzer.AnalysisResult analysis = entityAnalyzer.performAnalysis();
        
        // Generate recommendations
        List<String> recommendations = recommendationEngine.generateRecommendations(
            baselineMetrics, afterLoadMetrics != null ? afterLoadMetrics : baselineMetrics, analysis);
        
        return new BenchmarkResult(
            currentProfileName,
            safeMode ? "Safe Mode" : "Normal Mode",
            workloadDuration,
            recoveryDuration,
            score,
            benchmarkPoint,
            baselineMetrics,
            afterLoadMetrics,
            systemInfo,
            analysis,
            recommendations,
            aborted,
            System.currentTimeMillis()
        );
    }
    
    // Score calculation moved to ScoreCalculator (includes JVM RAM bonus)
    
    /**
     * Calculate benchmark point (CineBench style)
     */
    private double calculateBenchmarkPoint(double recoverySeconds) {
        if (recoverySeconds <= 0) {
            return 0.0;
        }
        double divisor = configManager.getBenchmarkPointDivisor();
        return recoverySeconds / divisor;
    }
    
    /**
     * Collect system information
     */
    private SystemInfo collectSystemInfo() {
        Util.JVMMemoryInfo memInfo = Util.getJVMMemoryInfo();
        
        return new SystemInfo(
            System.getProperty("java.version"),
            Bukkit.getVersion(),
            memInfo.toString(),
            Util.getCPUModel(),
            Util.formatMemoryGB(metricSampler.getTotalSystemMemoryMB() * 1024 * 1024),
            Util.getOSName(),
            Util.getServerLoader(),
            Util.hasViaVersion()
        );
    }
    
    /**
     * Display benchmark report in console
     */
    private void displayBenchmarkReport(BenchmarkResult result) {
        String report = result.formatConsoleReport(configManager);
        plugin.getLogger().info(Util.translateColorCodesForConsole(report));
    }
    
    // Getters for state checking
    public BenchmarkState getCurrentState() {
        return currentState;
    }
    
    public boolean isAwaitingConfirmation() {
        return awaitingConfirmation.get();
    }
    
    public boolean isBenchmarkRunning() {
        return currentState != BenchmarkState.IDLE;
    }
    
    public boolean isSafeMode() {
        return safeMode;
    }
    
    /**
     * Benchmark state enumeration
     */
    public enum BenchmarkState {
        IDLE,
        PREPARING,
        WORKLOAD,
        RECOVERY,
        REPORTING
    }
    
    /**
     * System information data class
     */
    public static class SystemInfo {
        private final String javaVersion;
        private final String minecraftVersion;
        private final String jvmMemory;
        private final String cpuModel;
        private final String totalMemory;
        private final String osName;
        private final String serverLoader;
        private final boolean hasViaVersion;
        
        public SystemInfo(String javaVersion, String minecraftVersion, String jvmMemory,
                         String cpuModel, String totalMemory, String osName,
                         String serverLoader, boolean hasViaVersion) {
            this.javaVersion = javaVersion;
            this.minecraftVersion = minecraftVersion;
            this.jvmMemory = jvmMemory;
            this.cpuModel = cpuModel;
            this.totalMemory = totalMemory;
            this.osName = osName;
            this.serverLoader = serverLoader;
            this.hasViaVersion = hasViaVersion;
        }
        
        // Getters
        public String getJavaVersion() { return javaVersion; }
        public String getMinecraftVersion() { return minecraftVersion; }
        public String getJvmMemory() { return jvmMemory; }
        public String getCpuModel() { return cpuModel; }
        public String getTotalMemory() { return totalMemory; }
        public String getOsName() { return osName; }
        public String getServerLoader() { return serverLoader; }
        public boolean hasViaVersion() { return hasViaVersion; }
    }
}
