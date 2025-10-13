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

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;

/**
 * Configuration class for internationalization settings.
 */
public class I18nConfig {
    
    private final Plugin plugin;
    private FileConfiguration config;
    
    public I18nConfig(Plugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Loads the i18n configuration.
     * Creates default config file if it doesn't exist.
     */
    public void load() {
        config = plugin.getConfig();
        
        // Set defaults if not present
        if (!config.contains("i18n.language")) {
            config.set("i18n.language", "en_US");
        }
        if (!config.contains("i18n.enable-minimessage")) {
            config.set("i18n.enable-minimessage", true);
        }
        if (!config.contains("i18n.fallback-language")) {
            config.set("i18n.fallback-language", "en_US");
        }
        
        save();
    }
    
    /**
     * Saves the configuration.
     */
    public void save() {
        try {
            config.save(new File(plugin.getDataFolder(), "config.yml"));
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save i18n configuration: " + e.getMessage());
        }
    }
    
    /**
     * Gets the configured language.
     * 
     * @return The language code
     */
    public String getLanguage() {
        return config.getString("i18n.language", "en_US");
    }
    
    /**
     * Gets whether MiniMessage is enabled.
     * 
     * @return true if MiniMessage is enabled
     */
    public boolean isMiniMessageEnabled() {
        return config.getBoolean("i18n.enable-minimessage", true);
    }
    
    /**
     * Gets the fallback language.
     * 
     * @return The fallback language code
     */
    public String getFallbackLanguage() {
        return config.getString("i18n.fallback-language", "en_US");
    }
}

