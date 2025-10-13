/*
 * WorldGuard, a suite of tools for Minecraft
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldGuard team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldguard.bukkit.i18n;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

/**
 * Manages internationalized messages for WorldGuard.
 */
public class MessageManager {
    
    private final Plugin plugin;
    private final Map<String, YamlConfiguration> languages = new HashMap<>();
    private String defaultLanguage = "en_US";
    private String currentLanguage = "en_US";
    
    public MessageManager(Plugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Loads all language files from the languages directory.
     */
    public void loadLanguages() {
        File langDir = new File(plugin.getDataFolder(), "languages");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }
        
        // Save default language files if they don't exist
        saveDefaultLanguageFile("en_US.yml");
        saveDefaultLanguageFile("zh_CN.yml");
        
        // Load all language files
        File[] langFiles = langDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (langFiles != null) {
            for (File langFile : langFiles) {
                String langCode = langFile.getName().replace(".yml", "");
                try {
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(langFile);
                    languages.put(langCode, config);
                    plugin.getLogger().info("Loaded language file: " + langCode);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to load language file: " + langCode, e);
                }
            }
        }
        
        // Load default language from resources as fallback
        loadDefaultFromResources();
    }
    
    private void saveDefaultLanguageFile(String fileName) {
        File langFile = new File(plugin.getDataFolder(), "languages/" + fileName);
        if (!langFile.exists()) {
            try (InputStream in = plugin.getResource("languages/" + fileName)) {
                if (in != null) {
                    Files.copy(in, langFile.toPath());
                    plugin.getLogger().info("Created default language file: " + fileName);
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to save default language file: " + fileName, e);
            }
        }
    }
    
    private void loadDefaultFromResources() {
        try (InputStream in = plugin.getResource("languages/" + defaultLanguage + ".yml")) {
            if (in != null) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(in, StandardCharsets.UTF_8)
                );
                languages.putIfAbsent(defaultLanguage, config);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load default language from resources", e);
        }
    }
    
    /**
     * Gets a message by key, with optional placeholder replacements.
     * 
     * @param key The message key
     * @param placeholders Placeholder key-value pairs
     * @return The formatted message
     */
    public String getMessage(String key, Object... placeholders) {
        YamlConfiguration langConfig = languages.get(currentLanguage);
        
        // Fall back to default language if current language doesn't have the key
        if (langConfig == null || !langConfig.contains(key)) {
            langConfig = languages.get(defaultLanguage);
        }
        
        // Return key if message not found
        if (langConfig == null || !langConfig.contains(key)) {
            plugin.getLogger().warning("Missing translation key: " + key);
            return key;
        }
        
        String message = langConfig.getString(key, key);
        
        // Replace placeholders
        if (placeholders.length > 0) {
            for (int i = 0; i < placeholders.length - 1; i += 2) {
                String placeholder = placeholders[i].toString();
                String value = placeholders[i + 1].toString();
                message = message.replace("{" + placeholder + "}", value);
            }
        }
        
        return message;
    }
    
    /**
     * Gets a message by key without placeholder replacement.
     * 
     * @param key The message key
     * @return The message
     */
    public String getMessage(String key) {
        return getMessage(key, new Object[0]);
    }
    
    /**
     * Sets the current language.
     * 
     * @param language The language code (e.g., "en_US", "zh_CN")
     */
    public void setLanguage(String language) {
        if (languages.containsKey(language)) {
            this.currentLanguage = language;
            plugin.getLogger().info("Language set to: " + language);
        } else {
            plugin.getLogger().warning("Language not found: " + language);
        }
    }
    
    /**
     * Gets the current language code.
     * 
     * @return The current language code
     */
    public String getCurrentLanguage() {
        return currentLanguage;
    }
    
    /**
     * Gets the default language code.
     * 
     * @return The default language code
     */
    public String getDefaultLanguage() {
        return defaultLanguage;
    }
    
    /**
     * Checks if a message key exists.
     * 
     * @param key The message key
     * @return true if the key exists
     */
    public boolean hasMessage(String key) {
        YamlConfiguration langConfig = languages.get(currentLanguage);
        if (langConfig != null && langConfig.contains(key)) {
            return true;
        }
        langConfig = languages.get(defaultLanguage);
        return langConfig != null && langConfig.contains(key);
    }
    
    /**
     * Reloads all language files.
     */
    public void reload() {
        languages.clear();
        loadLanguages();
    }
}

