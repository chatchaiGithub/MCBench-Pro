package online.chatchai.github.mcbench.recommendation;

import java.util.ArrayList;
import java.util.List;

import online.chatchai.github.mcbench.analysis.ChunkEntityAnalyzer;
import online.chatchai.github.mcbench.config.ConfigManager;
import online.chatchai.github.mcbench.metrics.MetricSampler;

/**
 * Recommendation engine for MCBench Pro
 * Generates performance tuning recommendations based on benchmark results
 */
public class RecommendationEngine {
    
    private final ConfigManager configManager;
    
    public RecommendationEngine(ConfigManager configManager) {
        this.configManager = configManager;
    }
    
    /**
     * Generate performance recommendations based on benchmark results
     * @param baseline Baseline metrics before benchmark
     * @param afterLoad Metrics after workload
     * @param analysis Chunk and entity analysis results
     * @return List of recommendations
     */
    public List<String> generateRecommendations(MetricSampler.MetricSnapshot baseline,
                                               MetricSampler.MetricSnapshot afterLoad,
                                               ChunkEntityAnalyzer.AnalysisResult analysis) {
        List<String> recommendations = new ArrayList<>();
        
        // Analyze TPS/MSPT performance
        analyzeTpsPerformance(baseline, afterLoad, recommendations);
        
        // Analyze memory usage
        analyzeMemoryUsage(baseline, afterLoad, recommendations);
        
        // Analyze CPU usage
        analyzeCpuUsage(baseline, afterLoad, recommendations);
        
        // Analyze chunk and entity counts
        analyzeChunkEntityCounts(analysis, recommendations);
        
        // If no recommendations were generated, add a positive message
        if (recommendations.isEmpty()) {
            recommendations.add("Server performance is optimal, no recommendations needed");
        }
        
        return recommendations;
    }
    
    /**
     * Analyze TPS and MSPT performance
     */
    private void analyzeTpsPerformance(MetricSampler.MetricSnapshot baseline,
                                     MetricSampler.MetricSnapshot afterLoad,
                                     List<String> recommendations) {
        double baselineTps = baseline.getTps();
        double baselineMspt = baseline.getMspt();
        double afterLoadTps = afterLoad.getTps();
        double afterLoadMspt = afterLoad.getMspt();
        
        double lowTpsThreshold = configManager.getLowTpsThreshold();
        double highMsptThreshold = configManager.getHighMsptThreshold();
        
        if (baselineTps < lowTpsThreshold || baselineMspt > highMsptThreshold) {
            recommendations.add("Consider reducing view-distance to 6-8 for better performance");
            recommendations.add("Consider reducing simulation-distance to 6-8");
            
            if (baselineMspt > 40) {
                recommendations.add("MSPT is high - consider reducing entity counts and plugin load");
            }
        }
        
        // Check recovery performance
        double tpsRecovery = afterLoadTps - baselineTps;
        double msptRecovery = afterLoadMspt - baselineMspt;
        
        if (Math.abs(tpsRecovery) > 2.0 || Math.abs(msptRecovery) > 10.0) {
            recommendations.add("Server shows poor recovery - consider optimizing plugin configuration");
        }
    }
    
    /**
     * Analyze memory usage patterns
     */
    private void analyzeMemoryUsage(MetricSampler.MetricSnapshot baseline,
                                   MetricSampler.MetricSnapshot afterLoad,
                                   List<String> recommendations) {
        double baselineMemory = baseline.getMemoryUsage();
        double afterLoadMemory = afterLoad.getMemoryUsage();
        double highRamThreshold = configManager.getHighRamUsageThreshold();
        
        if (baselineMemory > highRamThreshold) {
            long maxMemoryGB = baseline.getMaxMemoryMB() / 1024;
            long recommendedMemoryGB = Math.max(4, maxMemoryGB + 2);
            
            recommendations.add("High memory usage detected - consider increasing server RAM to " + 
                              recommendedMemoryGB + "GB");
            recommendations.add("Enable garbage collection optimization flags");
        }
        
        // Check memory increase during benchmark
        double memoryIncrease = afterLoadMemory - baselineMemory;
        if (memoryIncrease > 20.0) {
            recommendations.add("Significant memory increase during load - check for memory leaks in plugins");
        }
        
        // Recommend based on absolute memory values
        long usedMemoryMB = baseline.getUsedMemoryMB();
        long maxMemoryMB = baseline.getMaxMemoryMB();
        
        if (usedMemoryMB > maxMemoryMB * 0.85) {
            recommendations.add("Memory usage is very high - increase Xmx setting or reduce plugin count");
        }
    }
    
    /**
     * Analyze CPU usage patterns
     */
    private void analyzeCpuUsage(MetricSampler.MetricSnapshot baseline,
                                MetricSampler.MetricSnapshot afterLoad,
                                List<String> recommendations) {
        double baselineCpu = baseline.getSystemCpu();
        double afterLoadCpu = afterLoad.getSystemCpu();
        double highCpuThreshold = configManager.getHighCpuUsageThreshold();
        
        if (baselineCpu > highCpuThreshold) {
            recommendations.add("High CPU usage detected - consider upgrading server CPU");
            recommendations.add("Review plugin performance and disable unnecessary ones");
        }
        
        // Check CPU handling during benchmark
        double cpuIncrease = afterLoadCpu - baselineCpu;
        if (cpuIncrease > 30.0) {
            recommendations.add("Poor CPU scaling under load - optimize server settings");
        }
        
        // Process-specific CPU recommendations
        double processCpu = baseline.getProcessCpu();
        if (processCpu > 60.0) {
            recommendations.add("Minecraft process using high CPU - check for inefficient plugins");
        }
    }
    
    /**
     * Analyze chunk and entity counts
     */
    private void analyzeChunkEntityCounts(ChunkEntityAnalyzer.AnalysisResult analysis,
                                        List<String> recommendations) {
        int totalChunks = analysis.getTotalLoadedChunks();
        int totalEntities = analysis.getTotalEntities();
        
        int highChunkThreshold = configManager.getHighChunkCountThreshold();
        int highEntityThreshold = configManager.getHighEntityCountThreshold();
        
        if (totalChunks > highChunkThreshold) {
            int recommendedViewDistance = Math.max(6, 10 - (totalChunks / 100));
            recommendations.add("High chunk count (" + totalChunks + ") - reduce view-distance to " + 
                              recommendedViewDistance);
        }
        
        if (totalEntities > highEntityThreshold) {
            recommendations.add("High entity count (" + totalEntities + ") - implement entity cleanup");
            recommendations.add("Consider reducing mob spawn rates in server.properties");
            
            // Analyze entity density hotspots
            if (!analysis.getTopChunksByDensity().isEmpty()) {
                recommendations.add("Entity density hotspots detected - review chunk loading patterns");
            }
        }
        
        // Specific entity type recommendations
        if (analysis.getEntityCountsByType().getOrDefault(org.bukkit.entity.EntityType.ITEM, 0) > 500) {
            recommendations.add("Many dropped items detected - consider item cleanup plugins");
        }
        
        if (analysis.getEntityCountsByType().getOrDefault(org.bukkit.entity.EntityType.EXPERIENCE_ORB, 0) > 1000) {
            recommendations.add("Many experience orbs detected - adjust mob farm designs");
        }
        
        // World-specific recommendations
        analysis.getLoadedChunksByWorld().forEach((world, chunks) -> {
            if (chunks > highChunkThreshold / 2) {
                recommendations.add("World '" + world + "' has high chunk count (" + chunks + 
                                  ") - consider world border or chunk unloading");
            }
        });
    }
    
    /**
     * Generate server.properties recommendations
     */
    public List<String> generateServerPropertiesRecommendations(ChunkEntityAnalyzer.AnalysisResult analysis) {
        List<String> recommendations = new ArrayList<>();
        
        int totalEntities = analysis.getTotalEntities();
        
        if (totalEntities > 2000) {
            recommendations.add("spawn-limits.monsters=50");
            recommendations.add("spawn-limits.animals=8");
            recommendations.add("spawn-limits.water-animals=5");
        }
        
        if (analysis.getTotalLoadedChunks() > 800) {
            recommendations.add("view-distance=7");
            recommendations.add("simulation-distance=7");
        }
        
        return recommendations;
    }
    
    /**
     * Generate plugin-specific recommendations
     */
    public List<String> generatePluginRecommendations(MetricSampler.MetricSnapshot baseline) {
        List<String> recommendations = new ArrayList<>();
        
        if (baseline.getMspt() > 35) {
            recommendations.add("Consider disabling resource-intensive plugins temporarily");
            recommendations.add("Review plugin configurations for performance optimizations");
        }
        
        if (baseline.getMemoryUsage() > 75) {
            recommendations.add("Some plugins may have memory leaks - restart server periodically");
            recommendations.add("Use plugin profiling tools to identify memory-hungry plugins");
        }
        
        return recommendations;
    }
    
    /**
     * Generate JVM optimization recommendations
     */
    public List<String> generateJvmRecommendations(MetricSampler.MetricSnapshot baseline) {
        List<String> recommendations = new ArrayList<>();
        
        long maxMemoryMB = baseline.getMaxMemoryMB();
        
        if (maxMemoryMB < 4096) {
            recommendations.add("Increase Xmx to at least 4GB for better performance");
        }
        
        if (baseline.getMemoryUsage() > 80) {
            recommendations.add("Add JVM flags: -XX:+UseG1GC -XX:+ParallelRefProcEnabled");
            recommendations.add("Consider adding: -XX:MaxGCPauseMillis=200");
        }
        
        return recommendations;
    }
}
