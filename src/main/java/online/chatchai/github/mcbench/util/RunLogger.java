package online.chatchai.github.mcbench.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import online.chatchai.github.mcbench.Main;

/**
 * Utility class for logging benchmark run information to runs.log
 */
public class RunLogger {
    
    private final Main plugin;
    private final File runsLogFile;
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    
    public RunLogger(Main plugin) {
        this.plugin = plugin;
        this.runsLogFile = new File(plugin.getDataFolder(), "runs.log");
        
        // Create the data folder if it doesn't exist
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
    }
    
    /**
     * Log a benchmark run event
     * @param profile Profile name
     * @param safeMode Whether safe mode was used
     * @param initiator Who initiated the benchmark
     * @param result The result of the run
     * @param reason Optional reason for the result
     */
    public void logRun(String profile, boolean safeMode, CommandSender initiator, String result, String reason) {
        String timestamp = ZonedDateTime.now().format(ISO_FORMATTER);
        String initiatorName = getInitiatorName(initiator);
        
        StringBuilder logEntry = new StringBuilder();
        logEntry.append(timestamp)
                .append(" | profile=").append(profile)
                .append(" | safe=").append(safeMode)
                .append(" | initiator=").append(initiatorName)
                .append(" | result=").append(result);
        
        if (reason != null && !reason.isEmpty()) {
            logEntry.append(" | reason=").append(reason);
        }
        
        logEntry.append(System.lineSeparator());
        
        try (FileWriter writer = new FileWriter(runsLogFile, true)) {
            writer.write(logEntry.toString());
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to write to runs.log", e);
        }
    }
    
    /**
     * Log a save-all execution
     * @param timestamp ISO 8601 timestamp
     */
    public void logSaveAll(String timestamp) {
        String logEntry = timestamp + " | action=save-all | status=executed" + System.lineSeparator();
        
        try (FileWriter writer = new FileWriter(runsLogFile, true)) {
            writer.write(logEntry);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to write save-all log to runs.log", e);
        }
    }
    
    /**
     * Log a bypass action
     * @param profile Profile name
     * @param initiator Who used the bypass
     * @param ramRequired Required RAM in MB
     * @param ramCurrent Current RAM in MB
     */
    public void logBypass(String profile, CommandSender initiator, long ramRequired, long ramCurrent) {
        String timestamp = ZonedDateTime.now().format(ISO_FORMATTER);
        String initiatorName = getInitiatorName(initiator);
        
        String logEntry = timestamp + " | action=bypass | profile=" + profile + 
                         " | initiator=" + initiatorName + 
                         " | ram_required=" + ramRequired + 
                         " | ram_current=" + ramCurrent + 
                         System.lineSeparator();
        
        try (FileWriter writer = new FileWriter(runsLogFile, true)) {
            writer.write(logEntry);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to write bypass log to runs.log", e);
        }
    }
    
    /**
     * Log a bypass usage for RAM requirements
     * @param initiatorName Name of who used the bypass
     * @param profile Profile name
     */
    public void logBypassUsage(String initiatorName, String profile) {
        String timestamp = ZonedDateTime.now().format(ISO_FORMATTER);
        
        String logEntry = timestamp + " | action=bypass_used | profile=" + profile + 
                         " | initiator=" + initiatorName + 
                         " | reason=RAM_requirements_bypassed" + 
                         System.lineSeparator();
        
        try (FileWriter writer = new FileWriter(runsLogFile, true)) {
            writer.write(logEntry);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to write bypass usage log to runs.log", e);
        }
    }
    
    /**
     * Get the name of the command initiator
     * @param sender Command sender
     * @return Name or "Console"
     */
    private String getInitiatorName(CommandSender sender) {
        if (sender instanceof ConsoleCommandSender) {
            return "Console";
        } else {
            return sender.getName();
        }
    }
}
