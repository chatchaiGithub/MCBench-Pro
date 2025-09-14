package online.chatchai.github.mcbench.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import online.chatchai.github.mcbench.Main;

/**
 * Chunk and entity analyzer for MCBench Pro
 * Analyzes server world state to provide insights for performance tuning
 */
public class ChunkEntityAnalyzer {
    
    private final Main plugin;
    
    public ChunkEntityAnalyzer(Main plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Perform comprehensive chunk and entity analysis
     * @return Analysis result containing all collected data
     */
    public AnalysisResult performAnalysis() {
        try {
            Map<String, Integer> loadedChunks = analyzeLoadedChunks();
            Map<EntityType, Integer> entityCounts = analyzeEntityCounts();
            List<ChunkEntityDensity> topChunksByDensity = analyzeChunkEntityDensity();
            
            int totalChunks = loadedChunks.values().stream().mapToInt(Integer::intValue).sum();
            int totalEntities = entityCounts.values().stream().mapToInt(Integer::intValue).sum();
            
            return new AnalysisResult(
                totalChunks,
                totalEntities,
                loadedChunks,
                entityCounts,
                topChunksByDensity
            );
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to perform chunk/entity analysis: " + e.getMessage());
            return new AnalysisResult(0, 0, new HashMap<>(), new HashMap<>(), new ArrayList<>());
        }
    }
    
    /**
     * Analyze loaded chunks per world
     * @return Map of world name to loaded chunk count
     */
    private Map<String, Integer> analyzeLoadedChunks() {
        Map<String, Integer> result = new HashMap<>();
        
        for (World world : Bukkit.getWorlds()) {
            try {
                Chunk[] loadedChunks = world.getLoadedChunks();
                result.put(world.getName(), loadedChunks.length);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to analyze chunks for world " + world.getName() + ": " + e.getMessage());
                result.put(world.getName(), 0);
            }
        }
        
        return result;
    }
    
    /**
     * Analyze entity counts by type across all worlds
     * @return Map of entity type to count
     */
    private Map<EntityType, Integer> analyzeEntityCounts() {
        Map<EntityType, Integer> entityCounts = new HashMap<>();
        
        for (World world : Bukkit.getWorlds()) {
            try {
                for (Entity entity : world.getEntities()) {
                    EntityType type = entity.getType();
                    entityCounts.merge(type, 1, Integer::sum);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to analyze entities for world " + world.getName() + ": " + e.getMessage());
            }
        }
        
        return entityCounts;
    }
    
    /**
     * Analyze chunk entity density to find performance hotspots
     * @return List of top 5 chunks by entity density
     */
    private List<ChunkEntityDensity> analyzeChunkEntityDensity() {
        List<ChunkEntityDensity> densities = new ArrayList<>();
        
        for (World world : Bukkit.getWorlds()) {
            try {
                Chunk[] loadedChunks = world.getLoadedChunks();
                
                for (Chunk chunk : loadedChunks) {
                    try {
                        Entity[] entities = chunk.getEntities();
                        if (entities.length > 0) {
                            Map<EntityType, Integer> chunkEntityCounts = new HashMap<>();
                            for (Entity entity : entities) {
                                chunkEntityCounts.merge(entity.getType(), 1, Integer::sum);
                            }
                            
                            densities.add(new ChunkEntityDensity(
                                world.getName(),
                                chunk.getX(),
                                chunk.getZ(),
                                entities.length,
                                chunkEntityCounts
                            ));
                        }
                    } catch (Exception e) {
                        // Skip problematic chunks
                        continue;
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to analyze chunk density for world " + world.getName() + ": " + e.getMessage());
            }
        }
        
        // Return top 5 chunks by entity count
        return densities.stream()
            .sorted((a, b) -> Integer.compare(b.getTotalEntities(), a.getTotalEntities()))
            .limit(5)
            .collect(Collectors.toList());
    }
    
    /**
     * Analysis result data class
     */
    public static class AnalysisResult {
        private final int totalLoadedChunks;
        private final int totalEntities;
        private final Map<String, Integer> loadedChunksByWorld;
        private final Map<EntityType, Integer> entityCountsByType;
        private final List<ChunkEntityDensity> topChunksByDensity;
        
        public AnalysisResult(int totalLoadedChunks, int totalEntities,
                             Map<String, Integer> loadedChunksByWorld,
                             Map<EntityType, Integer> entityCountsByType,
                             List<ChunkEntityDensity> topChunksByDensity) {
            this.totalLoadedChunks = totalLoadedChunks;
            this.totalEntities = totalEntities;
            this.loadedChunksByWorld = loadedChunksByWorld;
            this.entityCountsByType = entityCountsByType;
            this.topChunksByDensity = topChunksByDensity;
        }
        
        // Getters
        public int getTotalLoadedChunks() { return totalLoadedChunks; }
        public int getTotalEntities() { return totalEntities; }
        public Map<String, Integer> getLoadedChunksByWorld() { return loadedChunksByWorld; }
        public Map<EntityType, Integer> getEntityCountsByType() { return entityCountsByType; }
        public List<ChunkEntityDensity> getTopChunksByDensity() { return topChunksByDensity; }
        
        /**
         * Get formatted string of loaded chunks by world
         */
        public String getFormattedChunksByWorld() {
            if (loadedChunksByWorld.isEmpty()) {
                return "No worlds found";
            }
            
            return loadedChunksByWorld.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining(", "));
        }
        
        /**
         * Get formatted string of top entity types
         */
        public String getFormattedTopEntities(int limit) {
            return entityCountsByType.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .map(entry -> entry.getKey().name() + ": " + entry.getValue())
                .collect(Collectors.joining(", "));
        }
        
        /**
         * Get formatted string of top chunks by density
         */
        public List<String> getFormattedTopChunks() {
            return topChunksByDensity.stream()
                .map(chunk -> String.format("  • %s (%d, %d): %d entities", 
                    chunk.getWorldName(), chunk.getChunkX(), chunk.getChunkZ(), chunk.getTotalEntities()))
                .collect(Collectors.toList());
        }
    }
    
    /**
     * Chunk entity density data class
     */
    public static class ChunkEntityDensity {
        private final String worldName;
        private final int chunkX;
        private final int chunkZ;
        private final int totalEntities;
        private final Map<EntityType, Integer> entityBreakdown;
        
        public ChunkEntityDensity(String worldName, int chunkX, int chunkZ,
                                 int totalEntities, Map<EntityType, Integer> entityBreakdown) {
            this.worldName = worldName;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.totalEntities = totalEntities;
            this.entityBreakdown = entityBreakdown;
        }
        
        // Getters
        public String getWorldName() { return worldName; }
        public int getChunkX() { return chunkX; }
        public int getChunkZ() { return chunkZ; }
        public int getTotalEntities() { return totalEntities; }
        public Map<EntityType, Integer> getEntityBreakdown() { return entityBreakdown; }
        
        /**
         * Get formatted breakdown of entities in this chunk
         */
        public String getFormattedEntityBreakdown() {
            return entityBreakdown.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(3) // Top 3 entity types
                .map(entry -> entry.getKey().name() + ":" + entry.getValue())
                .collect(Collectors.joining(", "));
        }
    }
}
