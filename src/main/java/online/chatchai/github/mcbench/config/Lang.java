package online.chatchai.github.mcbench.config;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import online.chatchai.github.mcbench.Main;
import online.chatchai.github.mcbench.util.Util;

/**
 * Language helper for multi-language message loading.
 * Loads the selected language from plugins/MCBench Pro/lang/<code>.yml with fallback to en-US.yml.
 */
public final class Lang {
    private static FileConfiguration current;
    private static FileConfiguration fallbackEnUS;
    private static String currentCode = "en-US";

    private Lang() {}

    /**
     * Initialize language system. Ensures default files exist and loads selected + fallback.
     */
    public static void init(Main plugin, String languageCode) {
        try {
            // Ensure lang directory and default files exist
            File langDir = new File(plugin.getDataFolder(), "lang");
            if (!langDir.exists()) {
                langDir.mkdirs();
            }

            ensureResource(plugin, "lang/en-US.yml");
            ensureResource(plugin, "lang/th-TH.yml");
            ensureResource(plugin, "lang/cn-CN.yml");

            // Load fallback English
            File enFile = new File(langDir, "en-US.yml");
            sanitizeYamlFile(plugin, enFile);
            fallbackEnUS = YamlConfiguration.loadConfiguration(enFile);

            // Load selected language
            File selected = new File(langDir, languageCode + ".yml");
            if (!selected.exists()) {
                plugin.getLogger().warning("Language file not found: " + selected.getName() + ", falling back to en-US.yml");
                current = fallbackEnUS;
                currentCode = "en-US";
            } else {
                sanitizeYamlFile(plugin, selected);
                current = YamlConfiguration.loadConfiguration(selected);
                currentCode = languageCode;
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to initialize language files. Falling back to defaults.", e);
            current = new YamlConfiguration();
            fallbackEnUS = new YamlConfiguration();
            currentCode = "en-US";
        }
    }

    private static void ensureResource(Main plugin, String resourcePath) {
        File out = new File(plugin.getDataFolder(), resourcePath);
        if (!out.exists()) {
            plugin.saveResource(resourcePath, false);
        }
    }

    public static String code() {
        return currentCode;
    }

    /**
     * Get a localized string with &-color support and placeholder pairs.
     * replacements are pairs: "%key%", "value".
     */
    public static String get(String key, String... replacements) {
        String value = current != null ? current.getString(key, null) : null;
        if (value == null && fallbackEnUS != null) {
            value = fallbackEnUS.getString(key, null);
        }
        if (value == null) {
            value = ""; // avoid noisy missing logs
        }
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            value = value.replace(replacements[i], String.valueOf(replacements[i + 1]));
        }
        return Util.colorize(value);
    }

    /**
     * Replace tab characters with spaces in YAML files to avoid SnakeYAML parse errors.
     */
    private static void sanitizeYamlFile(Main plugin, File file) {
        try {
            if (!file.exists()) return;
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            if (content.indexOf('\t') >= 0) {
                String fixed = content.replace("\t", "  ");
                Files.writeString(file.toPath(), fixed, StandardCharsets.UTF_8);
                plugin.getLogger().info("Sanitized tabs in language file: " + file.getName());
            }
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to sanitize language file: " + file.getName(), ex);
        }
    }
}
