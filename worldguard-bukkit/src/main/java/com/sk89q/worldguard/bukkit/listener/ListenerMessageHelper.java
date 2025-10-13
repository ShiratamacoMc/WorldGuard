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

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import org.bukkit.entity.Player;

/**
 * Helper class for sending internationalized messages from listeners.
 */
public class ListenerMessageHelper {
    
    private final WorldGuardPlugin plugin;
    
    public ListenerMessageHelper(WorldGuardPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Sends an internationalized message to a player.
     * 
     * @param player The player to send the message to
     * @param key The message key
     * @param placeholders Optional placeholder key-value pairs
     */
    public void sendMessage(Player player, String key, Object... placeholders) {
        String message = plugin.getMessageManager().getMessage(key, placeholders);
        plugin.getMiniMessageHelper().sendMessage(player, message);
    }
    
    /**
     * Sends an internationalized message to a player if MiniMessage is enabled.
     * Falls back to legacy chat colors if disabled.
     * 
     * @param player The player to send the message to
     * @param key The message key
     * @param placeholders Optional placeholder key-value pairs
     */
    public void sendMessageSafe(Player player, String key, Object... placeholders) {
        try {
            sendMessage(player, key, placeholders);
        } catch (Exception e) {
            // Fallback to simple message if MiniMessage parsing fails
            String message = plugin.getMessageManager().getMessage(key, placeholders);
            player.sendMessage(message);
        }
    }
}

