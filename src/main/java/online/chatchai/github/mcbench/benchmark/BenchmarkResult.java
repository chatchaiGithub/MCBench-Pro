package online.chatchai.github.mcbench.benchmark;

import java.util.List;

import online.chatchai.github.mcbench.analysis.ChunkEntityAnalyzer;
import online.chatchai.github.mcbench.config.ConfigManager;
import online.chatchai.github.mcbench.metrics.MetricSampler;
import online.chatchai.github.mcbench.util.Util;

/**
 * Benchmark result data class for MCBench Pro
 * Contains all data from a completed or aborted benchmark run
 */
public class BenchmarkResult {
    
    private final String profileName;
    private final String mode;
    private final double workloadDuration;
    private final double recoveryDuration;
    private final double score;
    private final double benchmarkPoint;
    private final MetricSampler.MetricSnapshot baselineMetrics;
    private final MetricSampler.MetricSnapshot afterLoadMetrics;
    private final BenchmarkManager.SystemInfo systemInfo;
    private final ChunkEntityAnalyzer.AnalysisResult analysis;
    private final List<String> recommendations;
    private final boolean aborted;
    private final long timestamp;
    
    public BenchmarkResult(String profileName, String mode, double workloadDuration,
                          double recoveryDuration, double score, double benchmarkPoint,
                          MetricSampler.MetricSnapshot baselineMetrics,
                          MetricSampler.MetricSnapshot afterLoadMetrics,
                          BenchmarkManager.SystemInfo systemInfo,
                          ChunkEntityAnalyzer.AnalysisResult analysis,
                          List<String> recommendations, boolean aborted, long timestamp) {
        this.profileName = profileName;
        this.mode = mode;
        this.workloadDuration = workloadDuration;
        this.recoveryDuration = recoveryDuration;
        this.score = score;
        this.benchmarkPoint = benchmarkPoint;
        this.baselineMetrics = baselineMetrics;
        this.afterLoadMetrics = afterLoadMetrics;
        this.systemInfo = systemInfo;
        this.analysis = analysis;
        this.recommendations = recommendations;
        this.aborted = aborted;
        this.timestamp = timestamp;
    }
    
    /**
     * Format console report using configuration messages
     * @param configManager Configuration manager for message formatting
     * @return Formatted console report string
     */
    public String formatConsoleReport(ConfigManager configManager) {
        StringBuilder sb = new StringBuilder();
        
        // Header
        sb.append(configManager.getMessage("report.header")).append("\n");
        sb.append(configManager.getMessage("report.profile", "%profile%", profileName)).append("\n");
        sb.append(configManager.getMessage("report.mode", "%mode%", mode)).append("\n");
        sb.append(configManager.getMessage("report.duration", 
            "%duration%", Util.formatDecimal(workloadDuration))).append("\n");
        
        if (aborted) {
            sb.append("&c&lSTATUS: ABORTED\n");
        }
        sb.append("\n");
        
        // Baseline metrics
        if (baselineMetrics != null) {
            sb.append(configManager.getMessage("report.baseline.header")).append("\n");
            sb.append(configManager.getMessage("report.baseline.tps", 
                "%tps%", Util.formatDecimal(baselineMetrics.getTps()))).append("\n");
            sb.append(configManager.getMessage("report.baseline.mspt", 
                "%mspt%", Util.formatDecimal(baselineMetrics.getMspt()))).append("\n");
            sb.append(configManager.getMessage("report.baseline.cpu", 
                "%cpu%", Util.formatDecimal(baselineMetrics.getSystemCpu()))).append("\n");
            sb.append(configManager.getMessage("report.baseline.ram", 
                "%ram%", Util.formatDecimal(baselineMetrics.getMemoryUsage()))).append("\n");
            sb.append("\n");
        }
        
        // After load metrics
        if (afterLoadMetrics != null) {
            sb.append(configManager.getMessage("report.afterload.header")).append("\n");
            sb.append(configManager.getMessage("report.afterload.tps", 
                "%tps%", Util.formatDecimal(afterLoadMetrics.getTps()))).append("\n");
            sb.append(configManager.getMessage("report.afterload.mspt", 
                "%mspt%", Util.formatDecimal(afterLoadMetrics.getMspt()))).append("\n");
            sb.append(configManager.getMessage("report.afterload.cpu", 
                "%cpu%", Util.formatDecimal(afterLoadMetrics.getSystemCpu()))).append("\n");
            sb.append(configManager.getMessage("report.afterload.ram", 
                "%ram%", Util.formatDecimal(afterLoadMetrics.getMemoryUsage()))).append("\n");
            sb.append("\n");
        }
        
        // Recovery and scores
        if (!aborted) {
            String recoveryTime = Util.formatTime(recoveryDuration);
            // Pull scoring config and JVM RAM to show exact breakdown
            String prof = profileName != null ? profileName : "normal";
            int base = configManager.getProfileBasePoints(prof);
            int perSec = configManager.getPenaltyPerSecond(prof);
            Util.JVMMemoryInfo jvm = Util.getJVMMemoryInfo();
            long jvmMb = jvm.getMaxMemoryMB();
            double ramBonus = jvmMb * 1.75;

            String scoreFormula = String.format(
                "%d - (%s * %d) + (%d * 1.75)",
                base,
                Util.formatDecimal(recoveryDuration),
                perSec,
                jvmMb
            );

            sb.append(configManager.getMessage("report.recovery.time", "%time%", recoveryTime)).append("\n");
            sb.append(configManager.getMessage("report.recovery.score", 
                "%score%", Util.formatDecimal(score),
                "%formula%", scoreFormula)).append("\n");
            sb.append(configManager.getMessage("report.recovery.bonus", 
                "%jvmMb%", String.valueOf(jvmMb),
                "%bonus%", Util.formatDecimal(ramBonus))).append("\n");
            sb.append(configManager.getMessage("report.recovery.benchmarkPoint", 
                "%point%", String.format("%.6f", benchmarkPoint))).append("\n");
            sb.append("\n");
        }
        
        // System information
        if (systemInfo != null) {
            sb.append(configManager.getMessage("report.system.header")).append("\n");
            sb.append(configManager.getMessage("report.system.java", 
                "%java%", systemInfo.getJavaVersion())).append("\n");
            sb.append(configManager.getMessage("report.system.minecraft", 
                "%minecraft%", systemInfo.getMinecraftVersion())).append("\n");
            sb.append(configManager.getMessage("report.system.jvmRam", 
                "%jvm%", systemInfo.getJvmMemory())).append("\n");
            sb.append(configManager.getMessage("report.system.hardware", 
                "%hardware%", systemInfo.getCpuModel() + " / " + systemInfo.getTotalMemory())).append("\n");
            sb.append("\n");
        }
        
        // Analysis
        if (analysis != null) {
            sb.append(configManager.getMessage("report.analysis.header")).append("\n");
            sb.append(configManager.getMessage("report.analysis.chunks", 
                "%chunks%", String.valueOf(analysis.getTotalLoadedChunks()))).append("\n");
            sb.append(configManager.getMessage("report.analysis.entities", 
                "%entities%", String.valueOf(analysis.getTotalEntities()))).append("\n");
            
            if (!analysis.getTopChunksByDensity().isEmpty()) {
                sb.append(configManager.getMessage("report.analysis.topChunks")).append("\n");
                for (String chunk : analysis.getFormattedTopChunks()) {
                    sb.append(chunk).append("\n");
                }
            }
            sb.append("\n");
        }
        
        // Recommendations
        if (recommendations != null && !recommendations.isEmpty()) {
            sb.append(configManager.getMessage("report.recommendations.header")).append("\n");
            if (recommendations.size() == 1 && recommendations.get(0).contains("optimal")) {
                sb.append(configManager.getMessage("report.recommendations.noRecommendations")).append("\n");
            } else {
                for (String recommendation : recommendations) {
                    sb.append("• ").append(recommendation).append("\n");
                }
            }
        }
        
        return sb.toString();
    }
    
    // Getters
    public String getProfileName() { return profileName; }
    public String getMode() { return mode; }
    public double getWorkloadDuration() { return workloadDuration; }
    public double getRecoveryDuration() { return recoveryDuration; }
    public double getScore() { return score; }
    public double getBenchmarkPoint() { return benchmarkPoint; }
    public MetricSampler.MetricSnapshot getBaselineMetrics() { return baselineMetrics; }
    public MetricSampler.MetricSnapshot getAfterLoadMetrics() { return afterLoadMetrics; }
    public BenchmarkManager.SystemInfo getSystemInfo() { return systemInfo; }
    public ChunkEntityAnalyzer.AnalysisResult getAnalysis() { return analysis; }
    public List<String> getRecommendations() { return recommendations; }
    public boolean isAborted() { return aborted; }
    public long getTimestamp() { return timestamp; }
}
