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

package com.sk89q.worldguard.bukkit;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.config.YamlConfigurationManager;
import com.sk89q.worldedit.util.report.Unreported;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class BukkitConfigurationManager extends YamlConfigurationManager {

    @Unreported private WorldGuardPlugin plugin;
    @Unreported private ConcurrentMap<String, BukkitWorldConfiguration> worlds = new ConcurrentHashMap<>();
    @Unreported private volatile BukkitWorldConfiguration unmanagedWorldConfig;

    private boolean hasCommandBookGodMode;
    boolean extraStats;

    /**
     * Construct the object.
     *
     * @param plugin The plugin instance
     */
    public BukkitConfigurationManager(WorldGuardPlugin plugin) {
        super();
        this.plugin = plugin;
    }

    public Collection<BukkitWorldConfiguration> getWorldConfigs() {
        return worlds.values();
    }

    @Override
    public void load() {
        super.load();
        this.extraStats = getConfig().getBoolean("custom-metrics-charts", true);
    }

    @Override
    public File getDataFolder() {
        return plugin.getDataFolder();
    }

    @Override
    public void copyDefaults() {
        // Create the default configuration file
        plugin.createDefaultConfiguration(new File(plugin.getDataFolder(), "config.yml"), "config.yml");
    }

    @Override
    public void unload() {
        worlds.clear();
        unmanagedWorldConfig = null;
    }

    @Override
    public void postLoad() {
        // Load configurations for each world
        for (World world : WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.GAME_HOOKS).getWorlds()) {
            // Only load configuration for whitelisted worlds
            if (isWorldWhitelisted(world.getName())) {
                get(world);
            }
            // Non-whitelisted worlds are silently ignored
        }
        getConfig().save();
    }

    /**
     * Get the configuration for a world.
     *
     * @param world The world to get the configuration for
     * @return {@code world}'s configuration, or an inert configuration when the world is not whitelisted
     */
    @Override
    public BukkitWorldConfiguration get(World world) {
        return get(world.getName());
    }

    /**
     * Gets a world configuration for API consumers.
     *
     * <p>WorldGuard integrations have historically assumed this method never
     * returns {@code null}. Return a shared inert configuration for worlds
     * outside the whitelist so those integrations continue to work, without
     * creating a world directory or allowing WorldGuard to manage the world.</p>
     *
     * @param worldName the world name
     * @return the managed configuration, or an inert configuration
     */
    public BukkitWorldConfiguration get(String worldName) {
        if (!isWorldWhitelisted(worldName)) {
            return getUnmanagedWorldConfig();
        }

        return getManaged(worldName);
    }

    /**
     * Gets a configuration only when WorldGuard manages the world.
     *
     * @param worldName the world name
     * @return the managed configuration, or {@code null} when the world is not whitelisted
     */
    public BukkitWorldConfiguration getManaged(String worldName) {
        if (!isWorldWhitelisted(worldName)) {
            return null;
        }

        BukkitWorldConfiguration config = worlds.get(worldName);
        BukkitWorldConfiguration newConfig = null;

        while (config == null) {
            if (newConfig == null) {
                File configFile = new File(plugin.getDataFolder(), "worlds/" + worldName + "/config.yml");
                newConfig = new BukkitWorldConfiguration(plugin, worldName, this.getConfig(), configFile.isFile());
            }
            worlds.putIfAbsent(worldName, newConfig);
            config = worlds.get(worldName);
        }

        return config;
    }

    private BukkitWorldConfiguration getUnmanagedWorldConfig() {
        BukkitWorldConfiguration config = unmanagedWorldConfig;
        if (config == null) {
            synchronized (this) {
                config = unmanagedWorldConfig;
                if (config == null) {
                    config = new BukkitWorldConfiguration(plugin, "__unmanaged__", getConfig(), false);
                    config.disableForUnmanagedWorld();
                    unmanagedWorldConfig = config;
                }
            }
        }
        return config;
    }

    public void updateCommandBookGodMode() {
        try {
            if (plugin.getServer().getPluginManager().isPluginEnabled("CommandBook")) {
                Class.forName("com.sk89q.commandbook.GodComponent");
                hasCommandBookGodMode = true;
                return;
            }
        } catch (ClassNotFoundException ignore) {}
        hasCommandBookGodMode = false;
    }

    public boolean hasCommandBookGodMode() {
        return hasCommandBookGodMode;
    }
}
