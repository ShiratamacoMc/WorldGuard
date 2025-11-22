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

package com.sk89q.worldguard.bukkit.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class to handle scheduler operations with Folia compatibility.
 * Automatically detects whether the server is running Folia or traditional Bukkit/Spigot/Paper
 * and uses the appropriate scheduler API.
 */
public class SchedulerUtil {

    private static final Logger logger = Logger.getLogger(SchedulerUtil.class.getCanonicalName());
    private static final boolean IS_FOLIA;
    private static Method getGlobalRegionSchedulerMethod;
    private static Method getRegionSchedulerMethod;
    private static Method getEntitySchedulerMethod;
    private static Method globalRunMethod;
    private static Method regionRunMethod;
    private static Method entityRunMethod;
    private static Method entityRunDelayedMethod;
    private static Method globalRunTimerMethod;

    static {
        boolean folia = false;
        try {
            // Try to load Folia's RegionScheduler class
            Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
            
            // Load necessary classes and methods
            Class<?> serverClass = Class.forName("org.bukkit.Server");
            Class<?> globalRegionSchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
            Class<?> regionSchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
            Class<?> entitySchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.EntityScheduler");
            
            // Get scheduler getter methods
            getGlobalRegionSchedulerMethod = serverClass.getMethod("getGlobalRegionScheduler");
            getRegionSchedulerMethod = serverClass.getMethod("getRegionScheduler");
            
            // Get Entity.getScheduler() method
            Class<?> entityClass = Class.forName("org.bukkit.entity.Entity");
            getEntitySchedulerMethod = entityClass.getMethod("getScheduler");
            
            // Get run methods
            globalRunMethod = globalRegionSchedulerClass.getMethod("run", Plugin.class, Runnable.class);
            regionRunMethod = regionSchedulerClass.getMethod("run", Plugin.class, Location.class, Runnable.class);
            entityRunMethod = entitySchedulerClass.getMethod("run", Plugin.class, Runnable.class, Runnable.class);
            entityRunDelayedMethod = entitySchedulerClass.getMethod("runDelayed", Plugin.class, Runnable.class, Runnable.class, long.class);
            globalRunTimerMethod = globalRegionSchedulerClass.getMethod("runAtFixedRate", Plugin.class, Runnable.class, long.class, long.class);
            
            // 只有当所有反射方法都成功获取后才设置为 true
            folia = true;
            logger.info("检测到 Folia 服务器，启用 Folia 调度器支持");
            
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            // Folia not detected or incompatible, will use traditional Bukkit scheduler
            folia = false;
            logger.info("使用传统 Bukkit 调度器");
        }
        IS_FOLIA = folia;
    }

    /**
     * Check if the server is running Folia.
     *
     * @return true if Folia is detected
     */
    public static boolean isFolia() {
        return IS_FOLIA;
    }

    /**
     * Run a task on the global region (Folia) or main thread (Bukkit).
     * This should be used for tasks that don't have a specific location or entity context.
     *
     * @param plugin the plugin
     * @param task   the task to run
     */
    public static void runTask(Plugin plugin, Runnable task) {
        if (IS_FOLIA) {
            try {
                Object globalScheduler = getGlobalRegionSchedulerMethod.invoke(Bukkit.getServer());
                globalRunMethod.invoke(globalScheduler, plugin, task);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to run global task on Folia", e);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Run a task on the region that owns the location (Folia) or main thread (Bukkit).
     *
     * @param plugin   the plugin
     * @param location the location
     * @param task     the task to run
     */
    public static void runTask(Plugin plugin, Location location, Runnable task) {
        if (IS_FOLIA) {
            try {
                Object regionScheduler = getRegionSchedulerMethod.invoke(Bukkit.getServer());
                regionRunMethod.invoke(regionScheduler, plugin, location, task);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to run region task on Folia", e);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Run a task on the entity's scheduler (Folia) or main thread (Bukkit).
     *
     * @param plugin the plugin
     * @param entity the entity
     * @param task   the task to run
     */
    public static void runTask(Plugin plugin, Entity entity, Runnable task) {
        if (IS_FOLIA) {
            try {
                Object entityScheduler = getEntitySchedulerMethod.invoke(entity);
                entityRunMethod.invoke(entityScheduler, plugin, task, null);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to run entity task on Folia", e);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Run a delayed task on the entity's scheduler (Folia) or main thread (Bukkit).
     * The delay must be at least 1 tick.
     *
     * @param plugin the plugin
     * @param entity the entity
     * @param task   the task to run
     * @param delay  the delay in ticks (must be >= 1)
     */
    public static void runTaskLater(Plugin plugin, Entity entity, Runnable task, long delay) {
        if (delay < 1) {
            throw new IllegalArgumentException("Delay must be at least 1 tick, got: " + delay);
        }
        
        if (IS_FOLIA) {
            try {
                Object entityScheduler = getEntitySchedulerMethod.invoke(entity);
                entityRunDelayedMethod.invoke(entityScheduler, plugin, task, null, delay);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to run delayed entity task on Folia", e);
            }
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delay);
        }
    }

    /**
     * Run a delayed task on the region that owns the location (Folia) or main thread (Bukkit).
     * The delay must be at least 1 tick.
     *
     * @param plugin   the plugin
     * @param location the location
     * @param task     the task to run
     * @param delay    the delay in ticks (must be >= 1)
     */
    public static void runTaskLater(Plugin plugin, Location location, Runnable task, long delay) {
        if (delay < 1) {
            throw new IllegalArgumentException("Delay must be at least 1 tick, got: " + delay);
        }
        
        if (IS_FOLIA) {
            try {
                Object regionScheduler = getRegionSchedulerMethod.invoke(Bukkit.getServer());
                Method runDelayedMethod = regionScheduler.getClass().getMethod("runDelayed", Plugin.class, Location.class, Runnable.class, long.class);
                runDelayedMethod.invoke(regionScheduler, plugin, location, task, delay);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to run delayed region task on Folia", e);
            }
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delay);
        }
    }

    /**
     * Run a repeating task on the global region (Folia) or main thread (Bukkit).
     * Both delay and period must be at least 1 tick.
     *
     * @param plugin the plugin
     * @param task   the task to run
     * @param delay  the initial delay in ticks (must be >= 1)
     * @param period the period in ticks (must be >= 1)
     */
    public static void runTaskTimer(Plugin plugin, Runnable task, long delay, long period) {
        if (delay < 1) {
            throw new IllegalArgumentException("Delay must be at least 1 tick, got: " + delay);
        }
        if (period < 1) {
            throw new IllegalArgumentException("Period must be at least 1 tick, got: " + period);
        }
        
        if (IS_FOLIA) {
            try {
                Object globalScheduler = getGlobalRegionSchedulerMethod.invoke(Bukkit.getServer());
                globalRunTimerMethod.invoke(globalScheduler, plugin, task, delay, period);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to run repeating task on Folia", e);
            }
        } else {
            Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
        }
    }

    /**
     * Cancel all tasks owned by the plugin.
     * In Folia, tasks are automatically cancelled when the plugin is disabled,
     * so this method only does something on traditional Bukkit servers.
     *
     * @param plugin the plugin
     */
    public static void cancelTasks(Plugin plugin) {
        if (!IS_FOLIA) {
            // On Folia, tasks are automatically cancelled on plugin disable
            // On traditional Bukkit, we need to manually cancel them
            Bukkit.getScheduler().cancelTasks(plugin);
        }
    }
}

