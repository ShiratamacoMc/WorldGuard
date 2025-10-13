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

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Utility class for sending internationalized messages with MiniMessage support.
 */
public final class MessageUtil {
    
    private MessageUtil() {
    }
    
    /**
     * Sends an internationalized message to a CommandSender.
     * 
     * @param plugin The WorldGuard plugin instance
     * @param sender The command sender
     * @param key The message key
     * @param placeholders Optional placeholder key-value pairs
     */
    public static void sendMessage(WorldGuardPlugin plugin, CommandSender sender, String key, Object... placeholders) {
        String message = plugin.getMessageManager().getMessage(key, placeholders);
        plugin.getMiniMessageHelper().sendMessage(sender, message);
    }
    
    /**
     * Sends an internationalized message with a prefix to a CommandSender.
     * 
     * @param plugin The WorldGuard plugin instance
     * @param sender The command sender
     * @param key The message key
     * @param placeholders Optional placeholder key-value pairs
     */
    public static void sendPrefixedMessage(WorldGuardPlugin plugin, CommandSender sender, String key, Object... placeholders) {
        String prefix = plugin.getMessageManager().getMessage("general.prefix");
        String message = plugin.getMessageManager().getMessage(key, placeholders);
        plugin.getMiniMessageHelper().sendMessage(sender, prefix + message);
    }
    
    /**
     * Sends an internationalized title to a Player.
     * 
     * @param plugin The WorldGuard plugin instance
     * @param player The player
     * @param titleKey The title message key
     * @param subtitleKey The subtitle message key (can be null)
     * @param placeholders Optional placeholder key-value pairs
     */
    public static void sendTitle(WorldGuardPlugin plugin, Player player, String titleKey, String subtitleKey, Object... placeholders) {
        String title = plugin.getMessageManager().getMessage(titleKey, placeholders);
        String subtitle = subtitleKey != null ? plugin.getMessageManager().getMessage(subtitleKey, placeholders) : null;
        plugin.getMiniMessageHelper().sendTitle(player, title, subtitle);
    }
    
    /**
     * Sends an internationalized action bar to a Player.
     * 
     * @param plugin The WorldGuard plugin instance
     * @param player The player
     * @param key The message key
     * @param placeholders Optional placeholder key-value pairs
     */
    public static void sendActionBar(WorldGuardPlugin plugin, Player player, String key, Object... placeholders) {
        String message = plugin.getMessageManager().getMessage(key, placeholders);
        plugin.getMiniMessageHelper().sendActionBar(player, message);
    }
    
    /**
     * Broadcasts an internationalized message to all players.
     * 
     * @param plugin The WorldGuard plugin instance
     * @param key The message key
     * @param placeholders Optional placeholder key-value pairs
     */
    public static void broadcast(WorldGuardPlugin plugin, String key, Object... placeholders) {
        String message = plugin.getMessageManager().getMessage(key, placeholders);
        plugin.getMiniMessageHelper().broadcast(message);
    }
    
    /**
     * Broadcasts an internationalized message to players with a specific permission.
     * 
     * @param plugin The WorldGuard plugin instance
     * @param key The message key
     * @param permission The permission node
     * @param placeholders Optional placeholder key-value pairs
     */
    public static void broadcastWithPermission(WorldGuardPlugin plugin, String key, String permission, Object... placeholders) {
        String message = plugin.getMessageManager().getMessage(key, placeholders);
        plugin.getMiniMessageHelper().broadcastWithPermission(message, permission);
    }
}

