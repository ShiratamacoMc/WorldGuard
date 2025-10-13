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

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

/**
 * Utility class to check if a world is in the whitelist and handle events accordingly.
 */
public class WorldWhitelistChecker {

    private final WorldGuardPlugin plugin;

    public WorldWhitelistChecker(WorldGuardPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Check if a world is whitelisted.
     *
     * @param world The world to check
     * @return true if the world is whitelisted or whitelist is disabled
     */
    public boolean isWorldWhitelisted(World world) {
        if (world == null) {
            return true;
        }
        return WorldGuard.getInstance().getPlatform().getGlobalStateManager()
                .isWorldWhitelisted(world.getName());
    }

    /**
     * Check if a world is whitelisted and cancel the event if not.
     * Also sends a message to the player if provided.
     *
     * @param world The world to check
     * @param event The event to cancel if world is not whitelisted
     * @param player The player to send message to (can be null)
     * @return true if the world is whitelisted, false otherwise
     */
    public boolean checkAndCancel(World world, Cancellable event, Player player) {
        if (!isWorldWhitelisted(world)) {
            event.setCancelled(true);
            if (player != null) {
                sendNotWhitelistedMessage(player);
            }
            return false;
        }
        return true;
    }

    /**
     * Send the "world not whitelisted" message to a player.
     *
     * @param player The player to send the message to
     */
    public void sendNotWhitelistedMessage(Player player) {
        String message = plugin.getMessageManager().getMessage("protection.world-not-whitelisted");
        plugin.getMiniMessageHelper().sendMessage(player, message);
    }

    /**
     * Check if WorldGuard should handle this world.
     *
     * @param worldName The world name to check
     * @return true if WorldGuard should handle this world
     */
    public boolean shouldHandleWorld(String worldName) {
        return WorldGuard.getInstance().getPlatform().getGlobalStateManager()
                .isWorldWhitelisted(worldName);
    }
}

