package online.chatchai.github.mcbench.diagnostics;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;

import online.chatchai.github.mcbench.Main;
import online.chatchai.github.mcbench.util.Util;

/**
 * Comprehensive server diagnostics system
 */
public class DiagnosticsEngine {
    
    private final Main plugin;
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    
    public DiagnosticsEngine(Main plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Run comprehensive server diagnostics
     * @return DiagnosticsResult containing all analysis
     */
    public DiagnosticsResult runDiagnostics() {
        DiagnosticsResult result = new DiagnosticsResult();
        
        // JVM Memory information
        result.jvmMemoryInfo = Util.getJVMMemoryInfo();
        
        // World and chunk analysis
        result.worldStats = analyzeWorlds();
        
        // Entity analysis
        result.entityStats = analyzeEntities();
        
        // Hotspot detection
        result.hotspots = detectHotspots();
        
        // Generate recommendations
        result.recommendations = generateRecommendations(result);
        
        // Generate player recommendations
        result.playerRecommendation = generatePlayerRecommendation();
        
        result.timestamp = ZonedDateTime.now().format(ISO_FORMATTER);
        
        return result;
    }
    
    /**
     * Analyze worlds and chunk counts
     */
    private Map<String, Integer> analyzeWorlds() {
        Map<String, Integer> worldStats = new HashMap<>();
        int totalChunks = 0;
        
        for (World world : Bukkit.getWorlds()) {
            int chunkCount = world.getLoadedChunks().length;
            worldStats.put(world.getName(), chunkCount);
            totalChunks += chunkCount;
        }
        
        worldStats.put("TOTAL", totalChunks);
        return worldStats;
    }
    
    /**
     * Analyze entity counts
     */
    private EntityStats analyzeEntities() {
        EntityStats stats = new EntityStats();
        
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                stats.totalEntities++;
                
                if (entity.getType() == EntityType.ITEM) {
                    stats.droppedItems++;
                } else if (entity instanceof ItemFrame) {
                    if (entity.getType() == EntityType.GLOW_ITEM_FRAME) {
                        stats.glowItemFrames++;
                    } else {
                        stats.itemFrames++;
                    }
                } else if (entity instanceof ArmorStand) {
                    stats.armorStands++;
                }
            }
        }
        
        return stats;
    }
    
    /**
     * Detect performance hotspots (chunks with too many entities)
     */
    private List<Hotspot> detectHotspots() {
        List<Hotspot> hotspots = new ArrayList<>();
        
        int itemFrameThreshold = plugin.getConfig().getInt("diagnostics.thresholds.perChunk.itemFrames", 15);
        int glowItemFrameThreshold = plugin.getConfig().getInt("diagnostics.thresholds.perChunk.glowItemFrames", 15);
        int armorStandThreshold = plugin.getConfig().getInt("diagnostics.thresholds.perChunk.armorStands", 5);
        
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                Map<EntityType, Integer> chunkEntityCounts = new HashMap<>();
                
                for (Entity entity : chunk.getEntities()) {
                    EntityType type = entity.getType();
                    chunkEntityCounts.merge(type, 1, Integer::sum);
                }
                
                // Check for hotspots
                int itemFrames = chunkEntityCounts.getOrDefault(EntityType.ITEM_FRAME, 0);
                int glowItemFrames = chunkEntityCounts.getOrDefault(EntityType.GLOW_ITEM_FRAME, 0);
                int armorStands = chunkEntityCounts.getOrDefault(EntityType.ARMOR_STAND, 0);
                
                if (itemFrames > itemFrameThreshold) {
                    hotspots.add(new Hotspot(world.getName(), chunk.getX(), chunk.getZ(), "ITEM_FRAME", itemFrames));
                }
                if (glowItemFrames > glowItemFrameThreshold) {
                    hotspots.add(new Hotspot(world.getName(), chunk.getX(), chunk.getZ(), "GLOW_ITEM_FRAME", glowItemFrames));
                }
                if (armorStands > armorStandThreshold) {
                    hotspots.add(new Hotspot(world.getName(), chunk.getX(), chunk.getZ(), "ARMOR_STAND", armorStands));
                }
            }
        }
        
        return hotspots;
    }
    
    /**
     * Generate performance recommendations
     */
    private List<String> generateRecommendations(DiagnosticsResult result) {
        List<String> recommendations = new ArrayList<>();
        
        long jvmMaxMB = result.jvmMemoryInfo.getMaxMemoryMB();
        int totalChunks = result.worldStats.getOrDefault("TOTAL", 0);
        int droppedItems = result.entityStats.droppedItems;
        int totalEntities = result.entityStats.totalEntities;
        
        // RAM recommendations
        int chunkWarning = plugin.getConfig().getInt("diagnostics.thresholds.loadedChunksWarning", 200);
        int minRamForHighEntities = plugin.getConfig().getInt("diagnostics.thresholds.minRamForHighEntities", 4096);
        
        if (totalChunks > chunkWarning && jvmMaxMB < minRamForHighEntities) {
            recommendations.add("Consider upgrading RAM (current: " + jvmMaxMB + " MB, recommended: 4+ GB)");
        }
        
        // Entity cleanup recommendations
        int droppedItemsWarning = plugin.getConfig().getInt("diagnostics.thresholds.droppedItemsWarning", 30);
        if (droppedItems > droppedItemsWarning) {
            recommendations.add("Install entity cleanup plugin (Clearlagg, ServerBoost, Lagfixer, LagAssist, CMI)");
        }
        
        // Entity count warnings
        int totalEntitiesWarning = plugin.getConfig().getInt("diagnostics.thresholds.totalEntitiesWarning", 120);
        if (totalEntities > totalEntitiesWarning && jvmMaxMB < minRamForHighEntities) {
            recommendations.add("Reduce entities and consider plugin solutions");
        }
        
        // Chunk recommendations
        if (totalChunks > chunkWarning) {
            recommendations.add("Reduce loaded chunks by lowering view-distance");
        }
        
        // Hotspot recommendations
        if (!result.hotspots.isEmpty()) {
            recommendations.add("Clean up entity hotspots at listed coordinates");
        }
        
        // All good message
        if (recommendations.isEmpty()) {
            recommendations.add("Server configuration appears optimal");
        }
        
        return recommendations;
    }
    
    /**
     * Generate player count recommendation based on JVM RAM
     */
    private String generatePlayerRecommendation() {
        long jvmMaxMB = Util.getJVMMemoryInfo().getMaxMemoryMB();
        
        Map<Integer, String> ramToPlayerMap = new HashMap<>();
        ramToPlayerMap.put(4096, "6-7");
        ramToPlayerMap.put(6144, "10-12");
        ramToPlayerMap.put(8192, "15");
        ramToPlayerMap.put(12288, "18-20");
        ramToPlayerMap.put(16384, "28");
        ramToPlayerMap.put(20480, "40");
        
        // Find the appropriate recommendation
        String recommendation = "6-7"; // Default for < 4GB
        for (Map.Entry<Integer, String> entry : ramToPlayerMap.entrySet()) {
            if (jvmMaxMB >= entry.getKey()) {
                recommendation = entry.getValue();
            }
        }
        
        return recommendation;
    }
    
    /**
     * Export diagnostics result to file
     */
    public void exportDiagnostics(DiagnosticsResult result) {
        String timestamp = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        File reportFile = new File(plugin.getDataFolder(), "diagnostics_" + timestamp + ".txt");
        
        try (FileWriter writer = new FileWriter(reportFile)) {
            writer.write("MCBench Pro - Server Diagnostics Report\n");
            writer.write("Generated: " + result.timestamp + "\n");
            writer.write("========================================\n\n");
            
            // JVM Memory Information
            writer.write("JVM Memory Information:\n");
            writer.write("  Max Memory: " + result.jvmMemoryInfo.getFormattedMaxMemory() + "\n");
            writer.write("  In-use Memory: " + result.jvmMemoryInfo.getFormattedUsedMemory() + "\n");
            writer.write("  Min/Base Allocation: " + result.jvmMemoryInfo.getFormattedMinMemory() + "\n\n");
            
            // World Statistics
            writer.write("World Statistics:\n");
            for (Map.Entry<String, Integer> entry : result.worldStats.entrySet()) {
                if (!entry.getKey().equals("TOTAL")) {
                    writer.write("  " + entry.getKey() + ": " + entry.getValue() + " chunks\n");
                }
            }
            writer.write("  Total Loaded Chunks: " + result.worldStats.get("TOTAL") + "\n\n");
            
            // Entity Analysis
            writer.write("Entity Analysis:\n");
            writer.write("  Total Entities: " + result.entityStats.totalEntities + "\n");
            writer.write("  Dropped Items: " + result.entityStats.droppedItems + "\n");
            writer.write("  Item Frames: " + result.entityStats.itemFrames + "\n");
            writer.write("  Glow Item Frames: " + result.entityStats.glowItemFrames + "\n");
            writer.write("  Armor Stands: " + result.entityStats.armorStands + "\n\n");
            
            // Hotspots
            writer.write("Performance Hotspots:\n");
            if (result.hotspots.isEmpty()) {
                writer.write("  No performance hotspots detected\n");
            } else {
                for (Hotspot hotspot : result.hotspots) {
                    writer.write("  Chunk [" + hotspot.world + "] " + hotspot.chunkX + "," + hotspot.chunkZ + 
                               ": " + hotspot.count + " " + hotspot.entityType + "\n");
                }
            }
            writer.write("\n");
            
            // Recommendations
            writer.write("Recommendations:\n");
            for (String recommendation : result.recommendations) {
                writer.write("  • " + recommendation + "\n");
            }
            writer.write("\n");
            
            // Player Recommendation
            writer.write("Suggested Max Players: " + result.playerRecommendation + "\n");
            long jvmMB = result.jvmMemoryInfo.getMaxMemoryMB();
            int foliaThreshold = plugin.getConfig().getInt("playerRecommendations.foliaRecommendationThreshold", 70);
            if (jvmMB >= 20480) { // 20GB+
                writer.write("Note: For player counts " + foliaThreshold + "+, Folia is strongly recommended\n");
            }
            
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to export diagnostics report", e);
        }
    }
    
    /**
     * Diagnostics result data class
     */
    public static class DiagnosticsResult {
        public Util.JVMMemoryInfo jvmMemoryInfo;
        public Map<String, Integer> worldStats;
        public EntityStats entityStats;
        public List<Hotspot> hotspots;
        public List<String> recommendations;
        public String playerRecommendation;
        public String timestamp;
    }
    
    /**
     * Entity statistics data class
     */
    public static class EntityStats {
        public int totalEntities = 0;
        public int droppedItems = 0;
        public int itemFrames = 0;
        public int glowItemFrames = 0;
        public int armorStands = 0;
    }
    
    /**
     * Performance hotspot data class
     */
    public static class Hotspot {
        public final String world;
        public final int chunkX;
        public final int chunkZ;
        public final String entityType;
        public final int count;
        
        public Hotspot(String world, int chunkX, int chunkZ, String entityType, int count) {
            this.world = world;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.entityType = entityType;
            this.count = count;
        }
    }
}
