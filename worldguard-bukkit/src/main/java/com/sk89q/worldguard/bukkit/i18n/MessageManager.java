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
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages internationalized messages for WorldGuard.
 */
public class MessageManager {
    private static final String BUNDLED_FALLBACK_LANGUAGE = "en_US";

    private final Plugin plugin;
    private final Map<String, YamlConfiguration> languages = new HashMap<>();
    private final Map<String, YamlConfiguration> bundledLanguages = new HashMap<>();
    private final Set<String> missingKeys = new HashSet<>();
    private String fallbackLanguage = BUNDLED_FALLBACK_LANGUAGE;
    private String currentLanguage = BUNDLED_FALLBACK_LANGUAGE;
    
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

        loadBundledLanguage(BUNDLED_FALLBACK_LANGUAGE);
        loadBundledLanguage("zh_CN");

        // Load all language files
        File[] langFiles = langDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (langFiles != null) {
            for (File langFile : langFiles) {
                String langCode = langFile.getName().replace(".yml", "");
                try {
                    repairLegacyDuplicateKeys(langFile);
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(langFile);
                    languages.put(langCode, config);
                    plugin.getLogger().info("Loaded language file: " + langCode);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to load language file: " + langCode, e);
                }
            }
        }
    }

    /**
     * Repairs duplicate keys produced by earlier bundled language files before Bukkit parses them.
     */
    private void repairLegacyDuplicateKeys(File langFile) throws IOException {
        String original = Files.readString(langFile.toPath(), StandardCharsets.UTF_8);
        String repaired = mergeDuplicateRootSection(original, "blacklist");
        repaired = repaired.replaceAll("(?m)^  (?:region-define|region-redefine|region-claim|region-info|region-list|region-priority):\\s*\"Usage:.*\"\\R?", "");
        repaired = retainFirstLine(repaired, Pattern.compile("(?m)^  claim-too-large:.*\\R?"));
        if (!original.equals(repaired)) {
            Files.writeString(langFile.toPath(), repaired, StandardCharsets.UTF_8);
            plugin.getLogger().info("Updated duplicate keys in language file: " + langFile.getName());
        }
    }

    private String mergeDuplicateRootSection(String content, String section) {
        Pattern pattern = Pattern.compile("(?m)^" + Pattern.quote(section) + ":\\R((?:^[ \\t]+.*\\R|^\\R)*)");
        Matcher matcher = pattern.matcher(content);
        if (!matcher.find()) {
            return content;
        }
        int firstStart = matcher.start();
        int firstEnd = matcher.end();
        String firstBody = matcher.group(1);
        if (!matcher.find()) {
            return content;
        }

        String withoutFirst = content.substring(0, firstStart) + content.substring(firstEnd);
        Matcher remaining = pattern.matcher(withoutFirst);
        if (!remaining.find()) {
            return content;
        }
        String lineEnding = content.contains("\r\n") ? "\r\n" : "\n";
        return withoutFirst.substring(0, remaining.start()) + section + ":" + lineEnding
                + firstBody + remaining.group(1) + withoutFirst.substring(remaining.end());
    }

    private String retainFirstLine(String content, Pattern pattern) {
        Matcher matcher = pattern.matcher(content);
        StringBuffer result = new StringBuffer();
        boolean found = false;
        while (matcher.find()) {
            if (found) {
                matcher.appendReplacement(result, "");
            } else {
                found = true;
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(result);
        return result.toString();
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
    
    private void loadBundledLanguage(String language) {
        try (InputStream in = plugin.getResource("languages/" + language + ".yml")) {
            if (in != null) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(in, StandardCharsets.UTF_8)
                );
                bundledLanguages.put(language, config);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load bundled language " + language, e);
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
        String message = findMessage(currentLanguage, key);
        if (message == null) {
            message = findMessage(fallbackLanguage, key);
        }
        if (message == null) {
            message = findBundledMessage(BUNDLED_FALLBACK_LANGUAGE, key);
        }
        if (message == null) {
            if (missingKeys.add(key)) {
                plugin.getLogger().warning("Missing translation key: " + key);
            }
            return key;
        }

        return replacePlaceholders(message, placeholders);
    }

    private String findMessage(String language, String key) {
        String message = findIn(languages.get(language), key);
        return message != null ? message : findBundledMessage(language, key);
    }

    private String findBundledMessage(String language, String key) {
        return findIn(bundledLanguages.get(language), key);
    }

    private String findIn(YamlConfiguration language, String key) {
        return language != null && language.isString(key) ? language.getString(key) : null;
    }

    private String replacePlaceholders(String message, Object... placeholders) {
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            String placeholder = String.valueOf(placeholders[i]);
            String value = String.valueOf(placeholders[i + 1]);
            message = message.replace("{" + placeholder + "}", value)
                    .replace("<" + placeholder + ">", value);
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
        String normalized = normalizeLanguage(language);
        if (hasLanguage(normalized)) {
            this.currentLanguage = normalized;
            plugin.getLogger().info("Language set to: " + normalized);
        } else {
            this.currentLanguage = fallbackLanguage;
            plugin.getLogger().warning("Language not found: " + language
                    + "; using fallback " + fallbackLanguage);
        }
    }

    /**
     * Sets the language used when the active language omits a key.
     *
     * @param language language code such as {@code en_US}
     */
    public void setFallbackLanguage(String language) {
        fallbackLanguage = normalizeLanguage(language);
    }

    private boolean hasLanguage(String language) {
        return languages.containsKey(language) || bundledLanguages.containsKey(language);
    }

    private String normalizeLanguage(String language) {
        if (language == null || language.trim().isEmpty()) {
            return BUNDLED_FALLBACK_LANGUAGE;
        }
        return language.trim().replace('-', '_');
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
        return fallbackLanguage;
    }
    
    /**
     * Checks if a message key exists.
     * 
     * @param key The message key
     * @return true if the key exists
     */
    public boolean hasMessage(String key) {
        return findMessage(currentLanguage, key) != null
                || findMessage(fallbackLanguage, key) != null
                || findBundledMessage(BUNDLED_FALLBACK_LANGUAGE, key) != null;
    }
    
    /**
     * Reloads all language files.
     */
    public void reload() {
        languages.clear();
        bundledLanguages.clear();
        missingKeys.clear();
        loadLanguages();
    }
}

