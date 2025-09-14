package online.chatchai.github.mcbench.report;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

import online.chatchai.github.mcbench.Main;
import online.chatchai.github.mcbench.benchmark.BenchmarkResult;
import online.chatchai.github.mcbench.config.ConfigManager;
import online.chatchai.github.mcbench.util.Util;

/**
 * Report exporter for MCBench Pro
 * Handles exporting benchmark results to various file formats
 */
public class ReportExporter {
    
    private final Main plugin;
    private final ConfigManager configManager;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    
    public ReportExporter(Main plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }
    
    /**
     * Export benchmark result to file
     * @param result Benchmark result to export
     */
    public void exportReport(BenchmarkResult result) {
        try {
            String format = configManager.getExportFormat().toLowerCase();
            
            switch (format) {
                case "txt":
                    exportToText(result);
                    break;
                case "json":
                    exportToJson(result);
                    break;
                case "yaml":
                case "yml":
                    exportToYaml(result);
                    break;
                default:
                    plugin.getLogger().warning("Unknown export format: " + format + ". Using TXT format.");
                    exportToText(result);
                    break;
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to export benchmark report", e);
        }
    }
    
    /**
     * Export to text format
     */
    private void exportToText(BenchmarkResult result) throws IOException {
        File file = createReportFile("txt");
        
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(generateTextReport(result));
            writer.flush();
        }
        
        plugin.getLogger().info(configManager.getMessage("report.export.success", 
            "%file%", file.getAbsolutePath()));
    }
    
    /**
     * Export to JSON format
     */
    private void exportToJson(BenchmarkResult result) throws IOException {
        File file = createReportFile("json");
        
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(generateJsonReport(result));
            writer.flush();
        }
        
        plugin.getLogger().info(configManager.getMessage("report.export.success", 
            "%file%", file.getAbsolutePath()));
    }
    
    /**
     * Export to YAML format
     */
    private void exportToYaml(BenchmarkResult result) throws IOException {
        File file = createReportFile("yml");
        
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(generateYamlReport(result));
            writer.flush();
        }
        
        plugin.getLogger().info(configManager.getMessage("report.export.success", 
            "%file%", file.getAbsolutePath()));
    }
    
    /**
     * Create report file with timestamp
     */
    private File createReportFile(String extension) throws IOException {
        File reportsDir = new File(plugin.getDataFolder(), "reports");
        if (!reportsDir.exists()) {
            reportsDir.mkdirs();
        }
        
        String timestamp = dateFormat.format(new Date(System.currentTimeMillis()));
        String filename = String.format("mcbench-report_%s.%s", timestamp, extension);
        
        return new File(reportsDir, filename);
    }
    
    /**
     * Generate text report content
     */
    private String generateTextReport(BenchmarkResult result) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("=".repeat(60)).append("\n");
        sb.append("MCBench Pro Benchmark Report\n");
        sb.append("Generated: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n");
        sb.append("=".repeat(60)).append("\n\n");
        
        // Basic information
        sb.append("BENCHMARK INFORMATION\n");
        sb.append("-".repeat(30)).append("\n");
        sb.append("Profile: ").append(result.getProfileName()).append("\n");
        sb.append("Mode: ").append(result.getMode()).append("\n");
        sb.append("Workload Duration: ").append(String.format("%.2f seconds", result.getWorkloadDuration())).append("\n");
        sb.append("Recovery Duration: ").append(String.format("%.2f seconds", result.getRecoveryDuration())).append("\n");
        sb.append("Status: ").append(result.isAborted() ? "ABORTED" : "COMPLETED").append("\n\n");
        
        // Performance metrics
        sb.append("PERFORMANCE METRICS\n");
        sb.append("-".repeat(30)).append("\n");
        if (result.getBaselineMetrics() != null) {
            sb.append("Baseline TPS: ").append(String.format("%.2f", result.getBaselineMetrics().getTps())).append("\n");
            sb.append("Baseline MSPT: ").append(String.format("%.2f ms", result.getBaselineMetrics().getMspt())).append("\n");
            sb.append("Baseline CPU: ").append(String.format("%.2f%%", result.getBaselineMetrics().getSystemCpu())).append("\n");
            sb.append("Baseline RAM: ").append(String.format("%.2f%%", result.getBaselineMetrics().getMemoryUsage())).append("\n");
        }
        
        if (result.getAfterLoadMetrics() != null) {
            sb.append("After Load TPS: ").append(String.format("%.2f", result.getAfterLoadMetrics().getTps())).append("\n");
            sb.append("After Load MSPT: ").append(String.format("%.2f ms", result.getAfterLoadMetrics().getMspt())).append("\n");
            sb.append("After Load CPU: ").append(String.format("%.2f%%", result.getAfterLoadMetrics().getSystemCpu())).append("\n");
            sb.append("After Load RAM: ").append(String.format("%.2f%%", result.getAfterLoadMetrics().getMemoryUsage())).append("\n");
        }
        sb.append("\n");
        
        // Scores
        sb.append("BENCHMARK SCORES\n");
        sb.append("-".repeat(30)).append("\n");
        sb.append("Score: ").append(String.format("%.2f", result.getScore())).append("\n");
                // RAM Bonus breakdown (points from JVM max memory)
                Util.JVMMemoryInfo mem = Util.getJVMMemoryInfo();
                long jvmMb = mem.getMaxMemoryMB();
                double ramBonus = jvmMb * 1.75;
                sb.append("RAM Bonus: ")
                    .append(Util.formatDecimal(ramBonus))
                    .append(" (from ")
                    .append(jvmMb)
                    .append(" MB * 1.75)")
                    .append("\n");
        sb.append("Benchmark Point: ").append(String.format("%.6f", result.getBenchmarkPoint())).append(" (CineBench style)\n\n");
        
        // System information
        if (result.getSystemInfo() != null) {
            sb.append("SYSTEM INFORMATION\n");
            sb.append("-".repeat(30)).append("\n");
            sb.append("Java Version: ").append(result.getSystemInfo().getJavaVersion()).append("\n");
            sb.append("Minecraft Version: ").append(result.getSystemInfo().getMinecraftVersion()).append("\n");
            sb.append("Server Loader: ").append(result.getSystemInfo().getServerLoader()).append("\n");
            sb.append("JVM Memory: ").append(result.getSystemInfo().getJvmMemory()).append("\n");
            sb.append("CPU Model: ").append(result.getSystemInfo().getCpuModel()).append("\n");
            sb.append("Total Memory: ").append(result.getSystemInfo().getTotalMemory()).append("\n");
            sb.append("Operating System: ").append(result.getSystemInfo().getOsName()).append("\n");
            sb.append("ViaVersion: ").append(result.getSystemInfo().hasViaVersion() ? "Yes" : "No").append("\n\n");
        }
        
        // Analysis results
        if (result.getAnalysis() != null) {
            sb.append("CHUNK & ENTITY ANALYSIS\n");
            sb.append("-".repeat(30)).append("\n");
            sb.append("Total Loaded Chunks: ").append(result.getAnalysis().getTotalLoadedChunks()).append("\n");
            sb.append("Total Entities: ").append(result.getAnalysis().getTotalEntities()).append("\n");
            sb.append("Chunks by World: ").append(result.getAnalysis().getFormattedChunksByWorld()).append("\n");
            sb.append("Top Entities: ").append(result.getAnalysis().getFormattedTopEntities(5)).append("\n");
            
            if (!result.getAnalysis().getTopChunksByDensity().isEmpty()) {
                sb.append("Top Entity-Dense Chunks:\n");
                for (String chunk : result.getAnalysis().getFormattedTopChunks()) {
                    sb.append(chunk).append("\n");
                }
            }
            sb.append("\n");
        }
        
        // Recommendations
        if (result.getRecommendations() != null && !result.getRecommendations().isEmpty()) {
            sb.append("PERFORMANCE RECOMMENDATIONS\n");
            sb.append("-".repeat(30)).append("\n");
            for (String recommendation : result.getRecommendations()) {
                sb.append("• ").append(recommendation).append("\n");
            }
            sb.append("\n");
        }
        
        sb.append("=".repeat(60)).append("\n");
        sb.append("End of Report\n");
        sb.append("=".repeat(60));
        
        return sb.toString();
    }
    
    /**
     * Generate JSON report content
     */
    private String generateJsonReport(BenchmarkResult result) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("{\n");
        sb.append("  \"mcbench_report\": {\n");
        sb.append("    \"generated_at\": \"").append(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new Date())).append("\",\n");
        sb.append("    \"profile\": \"").append(result.getProfileName()).append("\",\n");
        sb.append("    \"mode\": \"").append(result.getMode()).append("\",\n");
        sb.append("    \"workload_duration\": ").append(result.getWorkloadDuration()).append(",\n");
        sb.append("    \"recovery_duration\": ").append(result.getRecoveryDuration()).append(",\n");
        sb.append("    \"score\": ").append(result.getScore()).append(",\n");
        sb.append("    \"benchmark_point\": ").append(result.getBenchmarkPoint()).append(",\n");
        sb.append("    \"aborted\": ").append(result.isAborted()).append(",\n");
        
        // Baseline metrics
        if (result.getBaselineMetrics() != null) {
            sb.append("    \"baseline_metrics\": {\n");
            sb.append("      \"tps\": ").append(result.getBaselineMetrics().getTps()).append(",\n");
            sb.append("      \"mspt\": ").append(result.getBaselineMetrics().getMspt()).append(",\n");
            sb.append("      \"system_cpu\": ").append(result.getBaselineMetrics().getSystemCpu()).append(",\n");
            sb.append("      \"memory_usage\": ").append(result.getBaselineMetrics().getMemoryUsage()).append("\n");
            sb.append("    },\n");
        }
        
        // After load metrics
        if (result.getAfterLoadMetrics() != null) {
            sb.append("    \"after_load_metrics\": {\n");
            sb.append("      \"tps\": ").append(result.getAfterLoadMetrics().getTps()).append(",\n");
            sb.append("      \"mspt\": ").append(result.getAfterLoadMetrics().getMspt()).append(",\n");
            sb.append("      \"system_cpu\": ").append(result.getAfterLoadMetrics().getSystemCpu()).append(",\n");
            sb.append("      \"memory_usage\": ").append(result.getAfterLoadMetrics().getMemoryUsage()).append("\n");
            sb.append("    },\n");
        }
        
        // System info
        if (result.getSystemInfo() != null) {
            sb.append("    \"system_info\": {\n");
            sb.append("      \"java_version\": \"").append(result.getSystemInfo().getJavaVersion()).append("\",\n");
            sb.append("      \"minecraft_version\": \"").append(result.getSystemInfo().getMinecraftVersion()).append("\",\n");
            sb.append("      \"server_loader\": \"").append(result.getSystemInfo().getServerLoader()).append("\",\n");
            sb.append("      \"jvm_memory\": \"").append(result.getSystemInfo().getJvmMemory()).append("\",\n");
            sb.append("      \"cpu_model\": \"").append(result.getSystemInfo().getCpuModel()).append("\",\n");
            sb.append("      \"total_memory\": \"").append(result.getSystemInfo().getTotalMemory()).append("\",\n");
            sb.append("      \"os_name\": \"").append(result.getSystemInfo().getOsName()).append("\",\n");
            sb.append("      \"has_viaversion\": ").append(result.getSystemInfo().hasViaVersion()).append("\n");
            sb.append("    },\n");
        }
        
        // Analysis
        if (result.getAnalysis() != null) {
            sb.append("    \"analysis\": {\n");
            sb.append("      \"total_chunks\": ").append(result.getAnalysis().getTotalLoadedChunks()).append(",\n");
            sb.append("      \"total_entities\": ").append(result.getAnalysis().getTotalEntities()).append("\n");
            sb.append("    },\n");
        }
        
        // Recommendations
        if (result.getRecommendations() != null) {
            sb.append("    \"recommendations\": [\n");
            for (int i = 0; i < result.getRecommendations().size(); i++) {
                sb.append("      \"").append(result.getRecommendations().get(i)).append("\"");
                if (i < result.getRecommendations().size() - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append("    ]\n");
        }
        
        sb.append("  }\n");
        sb.append("}\n");
        
        return sb.toString();
    }
    
    /**
     * Generate YAML report content
     */
    private String generateYamlReport(BenchmarkResult result) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("mcbench_report:\n");
        sb.append("  generated_at: \"").append(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new Date())).append("\"\n");
        sb.append("  profile: \"").append(result.getProfileName()).append("\"\n");
        sb.append("  mode: \"").append(result.getMode()).append("\"\n");
        sb.append("  workload_duration: ").append(result.getWorkloadDuration()).append("\n");
        sb.append("  recovery_duration: ").append(result.getRecoveryDuration()).append("\n");
        sb.append("  score: ").append(result.getScore()).append("\n");
        sb.append("  benchmark_point: ").append(result.getBenchmarkPoint()).append("\n");
        sb.append("  aborted: ").append(result.isAborted()).append("\n");
        
        // Baseline metrics
        if (result.getBaselineMetrics() != null) {
            sb.append("  baseline_metrics:\n");
            sb.append("    tps: ").append(result.getBaselineMetrics().getTps()).append("\n");
            sb.append("    mspt: ").append(result.getBaselineMetrics().getMspt()).append("\n");
            sb.append("    system_cpu: ").append(result.getBaselineMetrics().getSystemCpu()).append("\n");
            sb.append("    memory_usage: ").append(result.getBaselineMetrics().getMemoryUsage()).append("\n");
        }
        
        // After load metrics
        if (result.getAfterLoadMetrics() != null) {
            sb.append("  after_load_metrics:\n");
            sb.append("    tps: ").append(result.getAfterLoadMetrics().getTps()).append("\n");
            sb.append("    mspt: ").append(result.getAfterLoadMetrics().getMspt()).append("\n");
            sb.append("    system_cpu: ").append(result.getAfterLoadMetrics().getSystemCpu()).append("\n");
            sb.append("    memory_usage: ").append(result.getAfterLoadMetrics().getMemoryUsage()).append("\n");
        }
        
        // System info
        if (result.getSystemInfo() != null) {
            sb.append("  system_info:\n");
            sb.append("    java_version: \"").append(result.getSystemInfo().getJavaVersion()).append("\"\n");
            sb.append("    minecraft_version: \"").append(result.getSystemInfo().getMinecraftVersion()).append("\"\n");
            sb.append("    server_loader: \"").append(result.getSystemInfo().getServerLoader()).append("\"\n");
            sb.append("    jvm_memory: \"").append(result.getSystemInfo().getJvmMemory()).append("\"\n");
            sb.append("    cpu_model: \"").append(result.getSystemInfo().getCpuModel()).append("\"\n");
            sb.append("    total_memory: \"").append(result.getSystemInfo().getTotalMemory()).append("\"\n");
            sb.append("    os_name: \"").append(result.getSystemInfo().getOsName()).append("\"\n");
            sb.append("    has_viaversion: ").append(result.getSystemInfo().hasViaVersion()).append("\n");
        }
        
        // Analysis
        if (result.getAnalysis() != null) {
            sb.append("  analysis:\n");
            sb.append("    total_chunks: ").append(result.getAnalysis().getTotalLoadedChunks()).append("\n");
            sb.append("    total_entities: ").append(result.getAnalysis().getTotalEntities()).append("\n");
        }
        
        // Recommendations
        if (result.getRecommendations() != null && !result.getRecommendations().isEmpty()) {
            sb.append("  recommendations:\n");
            for (String recommendation : result.getRecommendations()) {
                sb.append("    - \"").append(recommendation).append("\"\n");
            }
        }
        
        return sb.toString();
    }
}
