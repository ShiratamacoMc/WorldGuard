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
    @Unreported private BukkitWorldConfiguration defaultConfig;

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
        // 创建一个默认配置对象，用于未列入白名单的世界
        // 这样外部插件调用 API 不会收到 null，但不会生成配置文件
        if (defaultConfig == null) {
            defaultConfig = new BukkitWorldConfiguration(plugin, "__default__", this.getConfig());
        }
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
     * @return {@code world}'s configuration
     */
    @Override
    public BukkitWorldConfiguration get(World world) {
        String worldName = world.getName();
        return get(worldName);
    }

    public BukkitWorldConfiguration get(String worldName) {
        // 检查世界是否列入白名单
        if (!isWorldWhitelisted(worldName)) {
            // 未列入白名单的世界返回默认配置
            // 为外部插件(如 MythicMobs)提供 API 访问,防止空指针
            // 但不会创建配置文件,不会缓存,不会处理该世界
            return defaultConfig;
        }

        BukkitWorldConfiguration config = worlds.get(worldName);
        BukkitWorldConfiguration newConfig = null;

        // 只为白名单中的世界创建和缓存配置
        while (config == null) {
            if (newConfig == null) {
                newConfig = new BukkitWorldConfiguration(plugin, worldName, this.getConfig());
            }
            worlds.putIfAbsent(worldName, newConfig);
            config = worlds.get(worldName);
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
