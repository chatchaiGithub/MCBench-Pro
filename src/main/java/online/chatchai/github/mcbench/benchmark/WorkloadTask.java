package online.chatchai.github.mcbench.benchmark;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.scheduler.BukkitRunnable;

import online.chatchai.github.mcbench.Main;
import online.chatchai.github.mcbench.config.ConfigManager;

/**
 * CPU-intensive workload task for MCBench Pro
 * Performs deterministic heavy computations to stress test the server
 */
public class WorkloadTask extends BukkitRunnable {
    
    private final Main plugin;
    private final ConfigManager.ProfileConfig profile;
    private final BenchmarkManager benchmarkManager;
    private final long startTime;
    
    // Reusable objects to prevent allocations
    private final SecureRandom secureRandom = new SecureRandom();
    private final double[][] matrixA = new double[50][50];
    private final double[][] matrixB = new double[50][50];
    private final double[][] matrixResult = new double[50][50];
    
    // Workload state
    private long tickCount = 0;
    private boolean isRunning = true;
    
    public WorkloadTask(Main plugin, ConfigManager.ProfileConfig profile, 
                       BenchmarkManager benchmarkManager) {
        this.plugin = plugin;
        this.profile = profile;
        this.benchmarkManager = benchmarkManager;
        this.startTime = System.currentTimeMillis();
        
        // Initialize matrices for reuse
        initializeMatrices();
    }
    
    /**
     * Initialize matrices with random values for computation reuse
     */
    private void initializeMatrices() {
        for (int i = 0; i < 50; i++) {
            for (int j = 0; j < 50; j++) {
                matrixA[i][j] = ThreadLocalRandom.current().nextDouble(0.1, 10.0);
                matrixB[i][j] = ThreadLocalRandom.current().nextDouble(0.1, 10.0);
            }
        }
    }
    
    @Override
    public void run() {
        try {
            if (!isRunning) {
                cancel();
                return;
            }
            
            // Check if duration has elapsed
            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            if (elapsed >= profile.getDurationSeconds()) {
                benchmarkManager.onWorkloadCompleted();
                cancel();
                return;
            }
            
            // Perform CPU-intensive workload
            performWorkload();
            tickCount++;
            
        } catch (Exception e) {
            plugin.getLogger().severe("Workload execution error: " + e.getMessage());
            benchmarkManager.onWorkloadError(e);
            cancel();
        }
    }
    
    /**
     * Perform the actual CPU-intensive workload
     */
    private void performWorkload() {
        int adjustedLoopCount = (int) (profile.getLoopCountPerTick() * profile.getIntensityMultiplier());
        
        // Split workload into different types of computations
        int mathLoops = adjustedLoopCount / 4;
        int primeLoops = adjustedLoopCount / 8;
        int matrixOps = adjustedLoopCount / 16;
        int remainingLoops = adjustedLoopCount - mathLoops - primeLoops - matrixOps;
        
        // Mathematical computations (square roots, trigonometry)
        performMathOperations(mathLoops);
        
        // Prime number checking
        performPrimeChecking(primeLoops);
        
        // Matrix multiplication
        performMatrixOperations(matrixOps);
        
        // Additional CPU-bound operations
        performAdditionalOperations(remainingLoops);
    }
    
    /**
     * Perform mathematical operations (sqrt, sin, cos, log)
     */
    private void performMathOperations(int count) {
        double accumulator = 1.0;
        
        for (int i = 0; i < count; i++) {
            double value = (i % 1000) + 1.0;
            
            // Mix of mathematical operations
            accumulator += Math.sqrt(value);
            accumulator += Math.sin(value / 100.0);
            accumulator += Math.cos(value / 100.0);
            accumulator += Math.log(value);
            accumulator += Math.pow(value, 0.5);
            
            // Prevent overflow
            if (accumulator > 1e10) {
                accumulator = accumulator % 1e6;
            }
        }
        
        // Ensure the compiler doesn't optimize away the computation
        if (accumulator < 0) {
            plugin.getLogger().info("Negative accumulator: " + accumulator);
        }
    }
    
    /**
     * Perform prime number checking operations
     */
    private void performPrimeChecking(int count) {
        int primeCount = 0;
        
        for (int i = 0; i < count; i++) {
            // Generate a number to test for primality
            int candidate = 1000 + (i % 10000);
            
            if (isProbablyPrime(candidate)) {
                primeCount++;
            }
        }
        
        // Ensure the computation isn't optimized away
        if (primeCount < 0) {
            plugin.getLogger().info("Negative prime count: " + primeCount);
        }
    }
    
    /**
     * Simple probabilistic primality test
     */
    private boolean isProbablyPrime(int n) {
        if (n <= 1) return false;
        if (n <= 3) return true;
        if (n % 2 == 0 || n % 3 == 0) return false;
        
        // Check for factors up to sqrt(n)
        for (int i = 5; i * i <= n; i += 6) {
            if (n % i == 0 || n % (i + 2) == 0) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Perform matrix multiplication operations
     */
    private void performMatrixOperations(int count) {
        for (int op = 0; op < count; op++) {
            // Perform matrix multiplication: result = A * B
            for (int i = 0; i < 50; i++) {
                for (int j = 0; j < 50; j++) {
                    matrixResult[i][j] = 0.0;
                    for (int k = 0; k < 50; k++) {
                        matrixResult[i][j] += matrixA[i][k] * matrixB[k][j];
                    }
                }
            }
            
            // Rotate matrices to vary computation
            if (op % 10 == 0) {
                rotateMatrix(matrixA);
            }
        }
    }
    
    /**
     * Rotate matrix 90 degrees for variation in computation
     */
    private void rotateMatrix(double[][] matrix) {
        // Simple rotation logic
        for (int i = 0; i < 25; i++) {
            for (int j = i; j < 50 - i - 1; j++) {
                double temp = matrix[i][j];
                matrix[i][j] = matrix[50 - 1 - j][i];
                matrix[50 - 1 - j][i] = matrix[50 - 1 - i][50 - 1 - j];
                matrix[50 - 1 - i][50 - 1 - j] = matrix[j][50 - 1 - i];
                matrix[j][50 - 1 - i] = temp;
            }
        }
    }
    
    /**
     * Perform additional CPU-bound operations
     */
    private void performAdditionalOperations(int count) {
        for (int i = 0; i < count; i++) {
            // String operations (memory + CPU)
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < 100; j++) {
                sb.append("benchmark").append(j);
            }
            
            // Hash the string for additional computation
            int hash = sb.toString().hashCode();
            
            // BigInteger operations for CPU intensity
            if (i % 100 == 0) {
                BigInteger big1 = BigInteger.valueOf(hash);
                BigInteger big2 = BigInteger.valueOf(i + 1);
                BigInteger result = big1.multiply(big2).add(BigInteger.ONE);
                
                // Ensure computation isn't optimized away
                if (result.compareTo(BigInteger.ZERO) < 0) {
                    plugin.getLogger().info("Negative BigInteger: " + result);
                }
            }
        }
    }
    
    /**
     * Stop the workload task
     */
    public void stopWorkload() {
        isRunning = false;
        if (!isCancelled()) {
            cancel();
        }
    }
    
    /**
     * Get elapsed time in seconds
     */
    public long getElapsedSeconds() {
        return (System.currentTimeMillis() - startTime) / 1000;
    }
    
    /**
     * Get total duration in seconds
     */
    public int getTotalDurationSeconds() {
        return profile.getDurationSeconds();
    }
    
    /**
     * Get completion percentage
     */
    public double getCompletionPercentage() {
        long elapsed = getElapsedSeconds();
        return Math.min(100.0, (elapsed * 100.0) / profile.getDurationSeconds());
    }
    
    /**
     * Get current tick count
     */
    public long getTickCount() {
        return tickCount;
    }
}
