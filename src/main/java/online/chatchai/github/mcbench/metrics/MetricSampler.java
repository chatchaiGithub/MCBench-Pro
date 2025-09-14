package online.chatchai.github.mcbench.metrics;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;

import com.sun.management.OperatingSystemMXBean;

import online.chatchai.github.mcbench.Main;
import online.chatchai.github.mcbench.util.Util;

/**
 * Metric sampler for MCBench Pro
 * Collects real-time TPS, MSPT, CPU, and memory metrics using Paper APIs
 */
public class MetricSampler {
    
    private final Main plugin;
    private final OperatingSystemMXBean osMXBean;
    private final MemoryMXBean memoryMXBean;
    private final Runtime runtime;
    
    // CPU monitoring
    private final ScheduledExecutorService cpuMonitorExecutor;
    private final double[] recentSystemCpuUsage = new double[20];
    private final double[] recentProcessCpuUsage = new double[20];
    private int cpuSampleIndex = 0;
    private volatile double recentSystemCpuSnapshot = 0.0;
    private volatile double recentProcessCpuSnapshot = 0.0;
    private ScheduledFuture<?> cpuMonitorTask;
    
    public MetricSampler(Main plugin) {
        this.plugin = plugin;
        this.osMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
        this.runtime = Runtime.getRuntime();
        
        // Initialize CPU monitoring
        this.cpuMonitorExecutor = Executors.newSingleThreadScheduledExecutor(
            r -> new Thread(r, "MCBench-CPU-Monitor"));
        
        startCpuMonitoring();
    }
    
    /**
     * Start CPU monitoring task
     */
    private void startCpuMonitoring() {
        cpuMonitorTask = cpuMonitorExecutor.scheduleAtFixedRate(
            this::recordCpuUsage, 0L, 500L, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Record CPU usage samples
     */
    private void recordCpuUsage() {
        try {
            recentSystemCpuUsage[cpuSampleIndex] = getCurrentSystemCpuLoad();
            recentProcessCpuUsage[cpuSampleIndex] = getCurrentProcessCpuLoad();
            
            recentSystemCpuSnapshot = calculateAverageCpu(recentSystemCpuUsage);
            recentProcessCpuSnapshot = calculateAverageCpu(recentProcessCpuUsage);
            
            cpuSampleIndex = (cpuSampleIndex + 1) % 20;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to record CPU usage: " + e.getMessage());
        }
    }
    
    /**
     * Calculate average CPU usage from samples
     */
    private double calculateAverageCpu(double[] samples) {
        double sum = 0.0;
        int count = 0;
        
        for (double sample : samples) {
            if (sample > 0.0 && !Double.isNaN(sample)) {
                sum += sample;
                count++;
            }
        }
        
        if (count == 0) return 0.0;
        return Math.round((sum / count) * 100.0) / 100.0;
    }
    
    /**
     * Get current system CPU load as percentage
     */
    private double getCurrentSystemCpuLoad() {
        try {
            double load = osMXBean.getSystemCpuLoad();
            return Double.isNaN(load) ? 0.0 : load * 100.0;
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    /**
     * Get current process CPU load as percentage
     */
    private double getCurrentProcessCpuLoad() {
        try {
            double load = osMXBean.getProcessCpuLoad();
            return Double.isNaN(load) ? 0.0 : load * 100.0;
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    /**
     * Get real TPS from Paper/Pufferfish API
     * @return Current TPS
     */
    public double getTPS() {
        try {
            double[] tpsArray = Bukkit.getTPS();
            // Use 1-minute TPS (index 0) for most accurate current reading
            return tpsArray.length > 0 ? Math.min(20.0, tpsArray[0]) : 20.0;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get TPS: " + e.getMessage());
            return 20.0; // Fallback value
        }
    }
    
    /**
     * Get real MSPT from Paper/Pufferfish API
     * @return Current MSPT in milliseconds
     */
    public double getMSPT() {
        try {
            return Bukkit.getAverageTickTime();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get MSPT: " + e.getMessage());
            return 50.0; // Fallback value (50ms = 20 TPS)
        }
    }
    
    /**
     * Get system CPU usage percentage
     * @return System CPU usage (0-100)
     */
    public double getSystemCpuUsage() {
        return recentSystemCpuSnapshot;
    }
    
    /**
     * Get process CPU usage percentage
     * @return Process CPU usage (0-100)
     */
    public double getProcessCpuUsage() {
        return recentProcessCpuSnapshot;
    }
    
    /**
     * Get JVM memory usage percentage
     * @return Memory usage percentage (0-100)
     */
    public double getMemoryUsagePercentage() {
        try {
            long used = runtime.totalMemory() - runtime.freeMemory();
            long max = runtime.maxMemory();
            return Util.calculatePercentage(used, max);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get memory usage: " + e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * Get used JVM memory in MB
     * @return Used memory in MB
     */
    public long getUsedMemoryMB() {
        try {
            long used = runtime.totalMemory() - runtime.freeMemory();
            return used / 1024 / 1024;
        } catch (Exception e) {
            return 0L;
        }
    }
    
    /**
     * Get max JVM memory in MB
     * @return Max memory in MB
     */
    public long getMaxMemoryMB() {
        try {
            return runtime.maxMemory() / 1024 / 1024;
        } catch (Exception e) {
            return 0L;
        }
    }
    
    /**
     * Get total system memory in MB
     * @return Total system memory in MB
     */
    public long getTotalSystemMemoryMB() {
        try {
            return osMXBean.getTotalPhysicalMemorySize() / 1024 / 1024;
        } catch (Exception e) {
            return 0L;
        }
    }
    
    /**
     * Get system architecture
     * @return System architecture string
     */
    public String getSystemArchitecture() {
        return osMXBean.getArch();
    }
    
    /**
     * Get number of available processors
     * @return Number of processors
     */
    public int getAvailableProcessors() {
        return osMXBean.getAvailableProcessors();
    }
    
    /**
     * Create a snapshot of current metrics
     * @return MetricSnapshot containing current values
     */
    public MetricSnapshot createSnapshot() {
        return new MetricSnapshot(
            getTPS(),
            getMSPT(),
            getSystemCpuUsage(),
            getProcessCpuUsage(),
            getMemoryUsagePercentage(),
            getUsedMemoryMB(),
            getMaxMemoryMB(),
            System.currentTimeMillis()
        );
    }
    
    /**
     * Shutdown the metric sampler
     */
    public void shutdown() {
        try {
            if (cpuMonitorTask != null) {
                cpuMonitorTask.cancel(false);
            }
            if (cpuMonitorExecutor != null) {
                cpuMonitorExecutor.shutdown();
                if (!cpuMonitorExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    cpuMonitorExecutor.shutdownNow();
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error shutting down metric sampler: " + e.getMessage());
        }
    }
    
    /**
     * Metric snapshot data class
     */
    public static class MetricSnapshot {
        private final double tps;
        private final double mspt;
        private final double systemCpu;
        private final double processCpu;
        private final double memoryUsage;
        private final long usedMemoryMB;
        private final long maxMemoryMB;
        private final long timestamp;
        
        public MetricSnapshot(double tps, double mspt, double systemCpu, 
                            double processCpu, double memoryUsage, 
                            long usedMemoryMB, long maxMemoryMB, long timestamp) {
            this.tps = tps;
            this.mspt = mspt;
            this.systemCpu = systemCpu;
            this.processCpu = processCpu;
            this.memoryUsage = memoryUsage;
            this.usedMemoryMB = usedMemoryMB;
            this.maxMemoryMB = maxMemoryMB;
            this.timestamp = timestamp;
        }
        
        // Getters
        public double getTps() { return tps; }
        public double getMspt() { return mspt; }
        public double getSystemCpu() { return systemCpu; }
        public double getProcessCpu() { return processCpu; }
        public double getMemoryUsage() { return memoryUsage; }
        public long getUsedMemoryMB() { return usedMemoryMB; }
        public long getMaxMemoryMB() { return maxMemoryMB; }
        public long getTimestamp() { return timestamp; }
    }
}
