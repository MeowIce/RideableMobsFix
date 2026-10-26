package me.BigBou.rideableMobs.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {
    private final JavaPlugin plugin;
    private final Map<String, String> messageCache = new HashMap<>();
    private FileConfiguration langConfig;
    private FileConfiguration configFallback;
    private FileConfiguration defaultFallback;
    private String language;

    public LanguageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.language = plugin.getConfig().getString("language", "en");
        this.messageCache.clear();
        this.langConfig = loadLanguageFile(language);
        this.configFallback = plugin.getConfig();

        InputStream defIn = plugin.getResource("lang/en.yml");
        if (defIn != null) {
            this.defaultFallback = YamlConfiguration.loadConfiguration(new InputStreamReader(defIn, StandardCharsets.UTF_8));
        }
    }

    private FileConfiguration loadLanguageFile(String lang) {
        File dataFolder = plugin.getDataFolder();
        File langFolder = new File(dataFolder, "lang");
        File langFile = new File(langFolder, lang + ".yml");

        if (!langFile.exists()) {
            langFolder.mkdirs();
            InputStream in = plugin.getResource("lang/" + lang + ".yml");
            if (in != null) {
                plugin.saveResource("lang/" + lang + ".yml", false);
            }
        }

        FileConfiguration config = new YamlConfiguration();
        if (langFile.exists()) {
            try {
                config.load(langFile);
            } catch (Exception e) {
                plugin.getLogger().warning("Could not load language file: " + langFile.getName());
            }
        }

        return config;
    }

    public String getLanguage() {
        return language;
    }

    public String getMessage(String key) {
        return messageCache.computeIfAbsent(key, k -> {
            if (langConfig != null && langConfig.contains(k)) {
                return langConfig.getString(k);
            }
            if (configFallback != null && configFallback.contains(k)) {
                return configFallback.getString(k);
            }
            if (defaultFallback != null && defaultFallback.contains(k)) {
                return defaultFallback.getString(k);
            }
            return "<" + k + ">";
        });
    }

    public String getMessage(String key, String... replacements) {
        String message = getMessage(key);
        if (replacements != null) {
            for (int i = 0; i + 1 < replacements.length; i += 2) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }
        return message;
    }

    public Component getComponent(String key, String... replacements) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(getMessage(key, replacements));
    }

    public void send(CommandSender sender, String key, String... replacements) {
        sender.sendMessage(getComponent(key, replacements));
    }
}
