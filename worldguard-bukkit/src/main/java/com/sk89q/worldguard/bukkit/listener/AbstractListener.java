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

package com.sk89q.worldguard.bukkit.listener;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.BukkitConfigurationManager;
import com.sk89q.worldguard.bukkit.BukkitPlayer;
import com.sk89q.worldguard.bukkit.BukkitWorldConfiguration;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.bukkit.cause.Cause;
import com.sk89q.worldguard.bukkit.util.Entities;
import com.sk89q.worldguard.config.WorldConfiguration;
import com.sk89q.worldguard.domains.Association;
import com.sk89q.worldguard.protection.association.DelayedRegionOverlapAssociation;
import com.sk89q.worldguard.protection.association.Associables;
import com.sk89q.worldguard.protection.association.RegionAssociable;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import com.sk89q.worldguard.bukkit.event.DelegateEvent;
import io.papermc.lib.PaperLib;
import org.bukkit.event.Event;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

/**
 * Abstract listener to ease creation of listeners.
 */
class AbstractListener implements Listener {

    private final WorldGuardPlugin plugin;
    private final ThreadLocal<RegionQuery> queryCache = ThreadLocal.withInitial(
        () -> WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery()
    );

    /**
     * Construct the listener.
     *
     * @param plugin an instance of WorldGuardPlugin
     */
    public AbstractListener(WorldGuardPlugin plugin) {
        checkNotNull(plugin);
        this.plugin = plugin;
    }

    /**
     * Register events.
     */
    public void registerEvents() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Get the plugin.
     *
     * @return the plugin
     */
    protected static WorldGuardPlugin getPlugin() {
        return WorldGuardPlugin.inst();
    }

    /**
     * Get the global configuration.
     *
     * @return the configuration
     */
    protected static BukkitConfigurationManager getConfig() {
        return getPlugin().getConfigManager();
    }

    /**
     * Get the world configuration given a world.
     *
     * @param world The world to get the configuration for.
     * @return The configuration for {@code world}, or null if world is not whitelisted
     */
    protected static BukkitWorldConfiguration getWorldConfig(String world) {
        return getConfig().get(world);
    }

    protected static BukkitWorldConfiguration getWorldConfig(org.bukkit.World world) {
        return getWorldConfig(world.getName());
    }

    /**
     * Get the world configuration given a player.
     *
     * @param player The player to get the wold from
     * @return The {@link WorldConfiguration} for the player's world
     */
    protected static BukkitWorldConfiguration getWorldConfig(LocalPlayer player) {
        return getWorldConfig(((BukkitPlayer) player).getPlayer().getWorld());
    }

    /**
     * Return whether region support is enabled.
     *
     * @param world the world
     * @return true if region support is enabled
     */
    protected static boolean isRegionSupportEnabled(org.bukkit.World world) {
        BukkitWorldConfiguration config = getWorldConfig(world);
        return config != null && config.useRegions;
    }

    /**
     * Check if a world is in the whitelist.
     *
     * @param world the world to check
     * @return true if the world is whitelisted or whitelist is disabled
     */
    protected static boolean isWorldWhitelisted(org.bukkit.World world) {
        return getPlugin().getWorldWhitelistChecker().isWorldWhitelisted(world);
    }

    /**
     * Check if a world is whitelisted and cancel the event if not.
     * Also sends a message to the player if provided.
     *
     * @param world the world to check
     * @param event the event to cancel if world is not whitelisted
     * @param player the player to send message to (can be null)
     * @return true if the world is whitelisted, false otherwise
     */
    protected static boolean checkWorldWhitelist(org.bukkit.World world, org.bukkit.event.Cancellable event, Player player) {
        return getPlugin().getWorldWhitelistChecker().checkAndCancel(world, event, player);
    }

    /**
     * Get a cached RegionQuery instance for the current thread.
     * This reduces object allocation overhead compared to creating a new query each time.
     *
     * @return a RegionQuery instance
     */
    protected RegionQuery getRegionQuery() {
        return queryCache.get();
    }

    // Cache Paper check result to avoid repeated checks
    private static final boolean IS_PAPER = PaperLib.isPaper();

    protected RegionAssociable createRegionAssociable(Cause cause) {
        Object rootCause = cause.getRootCause();

        if (!cause.isKnown()) {
            return Associables.constant(Association.NON_MEMBER);
        } else if (rootCause instanceof Player player && !Entities.isNPC(player)) {
            return getPlugin().wrapPlayer(player);
        } else if (rootCause instanceof OfflinePlayer offlinePlayer) {
            return getPlugin().wrapOfflinePlayer(offlinePlayer);
        } else if (rootCause instanceof Entity entity) {
            BukkitWorldConfiguration config = getWorldConfig(entity.getWorld());
            if (config == null) {
                return Associables.constant(Association.NON_MEMBER);
            }
            Location loc;
            if (IS_PAPER && config.usePaperEntityOrigin) {
                loc = entity.getOrigin();
                // Origin world may be null, and thus a Location with a null world created, which cannot be adapted to a WorldEdit location
                if (loc == null || loc.getWorld() == null) {
                    loc = entity.getLocation();
                }
            } else {
                loc = entity.getLocation();
            }
            return new DelayedRegionOverlapAssociation(getRegionQuery(), BukkitAdapter.adapt(loc),
                    config.useMaxPriorityAssociation);
        } else if (rootCause instanceof Block block) {
            Location loc = block.getLocation();
            BukkitWorldConfiguration blockConfig = getWorldConfig(loc.getWorld());
            if (blockConfig == null) {
                return Associables.constant(Association.NON_MEMBER);
            }
            return new DelayedRegionOverlapAssociation(getRegionQuery(), BukkitAdapter.adapt(loc),
                    blockConfig.useMaxPriorityAssociation);
        } else {
            return Associables.constant(Association.NON_MEMBER);
        }
    }

    /**
     * Check if a region event should be processed.
     * This performs common pre-checks to avoid unnecessary region queries.
     *
     * @param event the delegate event
     * @param world the world
     * @param cause the cause
     * @param pvp whether this is a PvP event
     * @return true if the event should be processed, false otherwise
     */
    protected boolean shouldProcessRegionEvent(DelegateEvent event, org.bukkit.World world, Cause cause, boolean pvp) {
        if (event.getResult() == Event.Result.ALLOW) return false;
        
        BukkitWorldConfiguration config = getWorldConfig(world);
        if (config == null) return false;
        if (!config.useRegions) return false;
        
        // Check whitelist/bypass
        Object rootCause = cause.getRootCause();
        if (rootCause instanceof Player player) {
            if (config.fakePlayerBuildOverride && com.sk89q.worldguard.bukkit.util.InteropUtils.isFakePlayer(player)) {
                return false;
            }
            LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
            if (!pvp && WorldGuard.getInstance().getPlatform().getSessionManager().hasBypass(localPlayer, localPlayer.getWorld())) {
                return false;
            }
        }
        
        return true;
    }
}
