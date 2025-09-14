package online.chatchai.github.mcbench.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;

import online.chatchai.github.mcbench.Main;
import online.chatchai.github.mcbench.benchmark.BenchmarkManager;
import online.chatchai.github.mcbench.config.ConfigManager;
import online.chatchai.github.mcbench.diagnostics.DiagnosticsEngine;
import online.chatchai.github.mcbench.util.RunLogger;
import online.chatchai.github.mcbench.verification.RAMVerifier;

/**
 * Command executor and tab completer for MCBench Pro
 * Handles all /mcbench commands with proper validation and tab completion
 */
public class MCBenchCommand implements CommandExecutor, TabCompleter {
    
    private final Main plugin;
    private final ConfigManager configManager;
    private final BenchmarkManager benchmarkManager;
    private final DiagnosticsEngine diagnosticsEngine;
    private final RAMVerifier ramVerifier;
    private final RunLogger runLogger;
    
    public MCBenchCommand(Main plugin, ConfigManager configManager, BenchmarkManager benchmarkManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.benchmarkManager = benchmarkManager;
        this.diagnosticsEngine = new DiagnosticsEngine(plugin);
        this.ramVerifier = new RAMVerifier(plugin);
        this.runLogger = new RunLogger(plugin);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check basic permission
        if (!sender.hasPermission("mcbenchpro.command")) {
            // Only show permission error to console users
            if (sender instanceof ConsoleCommandSender) {
                sender.sendMessage(configManager.getMessage("command.noPermission"));
            }
            return true;
        }
        
        // Handle empty command
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "start":
                return handleStartCommand(sender, args);
            case "stop":
                return handleStopCommand(sender);
            case "confirm":
                return handleConfirmCommand(sender);
            case "cancel":
                return handleCancelCommand(sender);
            case "check":
                return handleCheckCommand(sender);
            case "reload":
                return handleReloadCommand(sender);
            case "help":
                sendHelpMessage(sender);
                return true;
            default:
                // Only show invalid syntax error to console users
                if (sender instanceof ConsoleCommandSender) {
                    sender.sendMessage(configManager.getMessage("command.invalidSyntax"));
                }
                return true;
        }
    }

    /**
     * Handle the reload command
     */
    private boolean handleReloadCommand(CommandSender sender) {
        // Only respond in console
        if (!(sender instanceof ConsoleCommandSender)) {
            return true;
        }

        try {
            // Reload config first to pick up new language
            plugin.reloadConfig();
            String langCode = plugin.getConfig().getString("settings.language", "en-US");
            online.chatchai.github.mcbench.config.Lang.init(plugin, langCode);

            // Reload ConfigManager and other dependent configs
            configManager.reload();

            sender.sendMessage(configManager.getMessage("system.reloaded"));
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to reload configuration", e);
        }

        return true;
    }
    
    /**
     * Handle the start command
     */
    private boolean handleStartCommand(CommandSender sender, String[] args) {
        // Allow both console and players, but only show messages to console for players
        boolean isPlayer = !(sender instanceof ConsoleCommandSender);
        
        // Check if benchmark is already running
        if (benchmarkManager.isBenchmarkRunning()) {
            if (!isPlayer) {
                sender.sendMessage(configManager.getMessage("command.benchmarkRunning"));
            }
            return true;
        }
        
        // Check if confirmation is pending
        if (benchmarkManager.isAwaitingConfirmation()) {
            if (!isPlayer) {
                sender.sendMessage(configManager.getMessage("command.confirmationPending"));
            }
            return true;
        }
        
        // Parse arguments: /mcbench start <profile> [safe] [--bypass]
        if (args.length < 2) {
            if (!isPlayer) {
                sender.sendMessage(configManager.getMessage("command.invalidSyntax"));
            }
            return true;
        }
        
        String profileName = args[1];
        boolean safeMode = false;
        boolean bypassRAM = false;
        
        // Parse additional arguments
        for (int i = 2; i < args.length; i++) {
            String arg = args[i].toLowerCase();
            if (arg.equals("safe")) {
                safeMode = true;
            } else if (arg.equals("--bypass")) {
                bypassRAM = true;
            }
        }
        
        // Validate profile
        ConfigManager.ProfileConfig profile = configManager.getProfile(profileName);
        if (profile == null) {
            if (!isPlayer) {
                String availableProfiles = String.join(", ", configManager.getAvailableProfiles());
                sender.sendMessage(configManager.getMessage("command.unknownProfile", 
                    "%profile%", profileName));
                sender.sendMessage("Available profiles: " + availableProfiles);
            }
            return true;
        }
        
        // Check RAM requirements (unless bypassed)
        if (!bypassRAM) {
            RAMVerifier.VerificationResult ramCheck = ramVerifier.verifyRAMRequirements(profileName);
            if (!ramCheck.sufficient) {
                if (!isPlayer) {
                    String warningMessage = configManager.getMessage(ramVerifier.getRAMMessageKey(profileName), 
                        "%current%", ramCheck.getFormattedCurrentRAM(),
                        "%required%", ramCheck.getFormattedRequiredRAM());
                    sender.sendMessage(warningMessage);
                    sender.sendMessage(configManager.getMessage("command.bypassHint"));
                }
                return true;
            }
        } else {
            // Log bypass usage
            runLogger.logBypassUsage(sender.getName(), profileName);
            if (!isPlayer) {
                sender.sendMessage(configManager.getMessage("command.bypassWarning"));
            }
        }
        
        // Start confirmation process
        boolean success = benchmarkManager.startBenchmarkConfirmation(profileName, safeMode);
        if (!success && !isPlayer) {
            sender.sendMessage("Failed to start benchmark confirmation process.");
        }
        
        return true;
    }
    
    /**
     * Handle the stop command
     */
    private boolean handleStopCommand(CommandSender sender) {
        // Allow both console and players, but only show messages to console for players
        boolean isPlayer = !(sender instanceof ConsoleCommandSender);
        
        // Check if benchmark is running
        if (!benchmarkManager.isBenchmarkRunning() && !benchmarkManager.isAwaitingConfirmation()) {
            if (!isPlayer) {
                sender.sendMessage(configManager.getMessage("command.noBenchmarkRunning"));
            }
            return true;
        }
        
        // Force stop the benchmark
        benchmarkManager.forceStop();
        
        return true;
    }
    
    /**
     * Handle the confirm command
     */
    private boolean handleConfirmCommand(CommandSender sender) {
        // Allow both console and players, but only show messages to console for players
        boolean isPlayer = !(sender instanceof ConsoleCommandSender);
        
        // Check if confirmation is pending
        if (!benchmarkManager.isAwaitingConfirmation()) {
            if (!isPlayer) {
                sender.sendMessage("No benchmark confirmation is pending.");
            }
            return true;
        }
        
        // Confirm the benchmark
        boolean success = benchmarkManager.confirmBenchmark();
        if (!success && !isPlayer) {
            sender.sendMessage("Failed to confirm benchmark.");
        }
        
        return true;
    }
    
    /**
     * Handle the check command - run diagnostics
     */
    private boolean handleCheckCommand(CommandSender sender) {
        // Allow both console and players, but only show messages to console for players
        boolean isPlayer = !(sender instanceof ConsoleCommandSender);
        
        if (isPlayer) {
            return true; // Don't show anything to players
        }
        
        try {
            // Run diagnostics
            DiagnosticsEngine.DiagnosticsResult result = diagnosticsEngine.runDiagnostics();
            
            // Display results to console
            sender.sendMessage("§a========== MCBench Pro - Server Diagnostics ==========");
            
            // JVM Memory Information
            sender.sendMessage("§eJVM Memory Information:");
            sender.sendMessage("  §fMax Memory: " + result.jvmMemoryInfo.getFormattedMaxMemory());
            sender.sendMessage("  §fIn-use Memory: " + result.jvmMemoryInfo.getFormattedUsedMemory());
            sender.sendMessage("  §fMin/Base Allocation: " + result.jvmMemoryInfo.getFormattedMinMemory());
            
            // World Statistics
            sender.sendMessage("§eWorld Statistics:");
            for (java.util.Map.Entry<String, Integer> entry : result.worldStats.entrySet()) {
                if (!entry.getKey().equals("TOTAL")) {
                    sender.sendMessage("  §f" + entry.getKey() + ": " + entry.getValue() + " chunks");
                }
            }
            sender.sendMessage("  §fTotal Loaded Chunks: " + result.worldStats.get("TOTAL"));
            
            // Entity Analysis
            sender.sendMessage("§eEntity Analysis:");
            sender.sendMessage("  §fTotal Entities: " + result.entityStats.totalEntities);
            sender.sendMessage("  §fDropped Items: " + result.entityStats.droppedItems);
            sender.sendMessage("  §fItem Frames: " + result.entityStats.itemFrames);
            sender.sendMessage("  §fGlow Item Frames: " + result.entityStats.glowItemFrames);
            sender.sendMessage("  §fArmor Stands: " + result.entityStats.armorStands);
            
            // Performance Hotspots
            sender.sendMessage("§ePerformance Hotspots:");
            if (result.hotspots.isEmpty()) {
                sender.sendMessage("  §aNo performance hotspots detected");
            } else {
                for (DiagnosticsEngine.Hotspot hotspot : result.hotspots) {
                    sender.sendMessage("  §cChunk [" + hotspot.world + "] " + hotspot.chunkX + "," + hotspot.chunkZ + 
                                     ": " + hotspot.count + " " + hotspot.entityType);
                }
            }
            
            // Recommendations
            sender.sendMessage("§eRecommendations:");
            for (String recommendation : result.recommendations) {
                sender.sendMessage("  §f• " + recommendation);
            }
            
            // Player Recommendation
            sender.sendMessage("§eSuggested Max Players: §a" + result.playerRecommendation);
            
            // Export to file
            diagnosticsEngine.exportDiagnostics(result);
            sender.sendMessage("§aFull diagnostics report exported to plugins/MCBench Pro/diagnostics_[timestamp].txt");
            
        } catch (Exception e) {
            sender.sendMessage("§cError running diagnostics: " + e.getMessage());
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Error running diagnostics", e);
        }
        
        return true;
    }
    
    /**
     * Handle the cancel command
     */
    private boolean handleCancelCommand(CommandSender sender) {
        // Allow both console and players, but only show messages to console for players
        boolean isPlayer = !(sender instanceof ConsoleCommandSender);
        
        // Check if confirmation is pending
        if (!benchmarkManager.isAwaitingConfirmation()) {
            if (!isPlayer) {
                sender.sendMessage("No benchmark confirmation is pending.");
            }
            return true;
        }
        
        // Cancel the confirmation
        boolean success = benchmarkManager.cancelConfirmation();
        if (!success && !isPlayer) {
            sender.sendMessage("Failed to cancel benchmark confirmation.");
        }
        
        return true;
    }
    
    /**
     * Send help message
     */
    private void sendHelpMessage(CommandSender sender) {
        // Only show help to console users
        if (!(sender instanceof ConsoleCommandSender)) {
            return;
        }
        
        sender.sendMessage(configManager.getMessage("command.help.header"));
        sender.sendMessage(configManager.getMessage("command.help.start"));
        sender.sendMessage(configManager.getMessage("command.help.stop"));
        sender.sendMessage(configManager.getMessage("command.help.confirm"));
        sender.sendMessage(configManager.getMessage("command.help.cancel"));
        sender.sendMessage(configManager.getMessage("command.help.check"));
    sender.sendMessage(configManager.getMessage("command.help.reload"));
        sender.sendMessage(configManager.getMessage("command.help.profiles"));
        sender.sendMessage(configManager.getMessage("command.help.safeMode"));
        sender.sendMessage(configManager.getMessage("command.help.bypass"));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // Check permission
        if (!sender.hasPermission("mcbenchpro.command")) {
            return Collections.emptyList();
        }
        
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First argument: subcommands
            List<String> subCommands = Arrays.asList("start", "stop", "confirm", "cancel", "check", "reload", "help");
            String input = args[0].toLowerCase();
            
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(input)) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            // Second argument for start: profile names
            String input = args[1].toLowerCase();
            String[] profiles = configManager.getAvailableProfiles();
            
            for (String profile : profiles) {
                if (profile.toLowerCase().startsWith(input)) {
                    completions.add(profile);
                }
            }
        } else if (args.length >= 3 && args[0].equalsIgnoreCase("start")) {
            // Third+ argument for start: "safe" and "--bypass" options
            String input = args[args.length - 1].toLowerCase();
            List<String> options = Arrays.asList("safe", "--bypass");
            
            for (String option : options) {
                if (option.startsWith(input) && !Arrays.asList(args).contains(option)) {
                    completions.add(option);
                }
            }
        }
        
        return completions;
    }
}
