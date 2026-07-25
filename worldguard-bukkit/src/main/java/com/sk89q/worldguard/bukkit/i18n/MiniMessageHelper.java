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

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.title.Title;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for working with MiniMessage formatting.
 */
public class MiniMessageHelper {
    
    private final Plugin plugin;
    private final BukkitAudiences audiences;
    private final MiniMessage miniMessage;
    
    public MiniMessageHelper(Plugin plugin) {
        this.plugin = plugin;
        this.audiences = BukkitAudiences.create(plugin);
        this.miniMessage = MiniMessage.miniMessage();
    }
    
    /**
     * Parses a MiniMessage string into a Component.
     * 
     * @param message The MiniMessage formatted string
     * @return The parsed Component
     */
    public Component parse(String message) {
        return miniMessage.deserialize(message);
    }
    
    /**
     * Parses a MiniMessage string with placeholders into a Component.
     * 
     * @param message The MiniMessage formatted string
     * @param placeholders Placeholder key-value pairs
     * @return The parsed Component
     */
    public Component parse(String message, Object... placeholders) {
        if (placeholders.length == 0) {
            return parse(message);
        }
        
        List<TagResolver> resolvers = new ArrayList<>();
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            String key = placeholders[i].toString();
            String value = placeholders[i + 1].toString();
            resolvers.add(Placeholder.unparsed(key, value));
        }
        
        return miniMessage.deserialize(message, TagResolver.resolver(resolvers));
    }
    
    /**
     * Sends a MiniMessage formatted message to a CommandSender.
     * 
     * @param sender The command sender
     * @param message The MiniMessage formatted string
     * @param placeholders Optional placeholder key-value pairs
     */
    public void sendMessage(CommandSender sender, String message, Object... placeholders) {
        Component component = parse(message, placeholders);
        audiences.sender(sender).sendMessage(component);
    }

    /**
     * Sends a pre-built Adventure component to a command sender.
     *
     * @param sender recipient
     * @param component component to send
     */
    public void sendComponent(CommandSender sender, Component component) {
        audiences.sender(sender).sendMessage(component);
    }
    
    /**
     * Sends a MiniMessage formatted action bar to a Player.
     * 
     * @param player The player
     * @param message The MiniMessage formatted string
     * @param placeholders Optional placeholder key-value pairs
     */
    public void sendActionBar(Player player, String message, Object... placeholders) {
        Component component = parse(message, placeholders);
        audiences.player(player).sendActionBar(component);
    }
    
    /**
     * Sends a MiniMessage formatted title to a Player.
     * 
     * @param player The player
     * @param title The title text
     * @param subtitle The subtitle text
     * @param fadeIn Fade in time in ticks
     * @param stay Stay time in ticks
     * @param fadeOut Fade out time in ticks
     * @param placeholders Optional placeholder key-value pairs
     */
    public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut, Object... placeholders) {
        Component titleComponent = parse(title, placeholders);
        Component subtitleComponent = subtitle != null ? parse(subtitle, placeholders) : Component.empty();
        
        Title.Times times = Title.Times.times(
            Duration.ofMillis(fadeIn * 50L),
            Duration.ofMillis(stay * 50L),
            Duration.ofMillis(fadeOut * 50L)
        );
        
        Title titleObj = Title.title(titleComponent, subtitleComponent, times);
        audiences.player(player).showTitle(titleObj);
    }
    
    /**
     * Sends a MiniMessage formatted title to a Player with default timings.
     * 
     * @param player The player
     * @param title The title text
     * @param subtitle The subtitle text
     * @param placeholders Optional placeholder key-value pairs
     */
    public void sendTitle(Player player, String title, String subtitle, Object... placeholders) {
        sendTitle(player, title, subtitle, 10, 70, 20, placeholders);
    }
    
    /**
     * Broadcasts a MiniMessage formatted message to all online players.
     * 
     * @param message The MiniMessage formatted string
     * @param placeholders Optional placeholder key-value pairs
     */
    public void broadcast(String message, Object... placeholders) {
        Component component = parse(message, placeholders);
        audiences.all().sendMessage(component);
    }
    
    /**
     * Broadcasts a MiniMessage formatted message to players with a specific permission.
     * 
     * @param message The MiniMessage formatted string
     * @param permission The permission node
     * @param placeholders Optional placeholder key-value pairs
     */
    public void broadcastWithPermission(String message, String permission, Object... placeholders) {
        Component component = parse(message, placeholders);
        audiences.permission(permission).sendMessage(component);
    }
    
    /**
     * Gets the BukkitAudiences instance.
     * 
     * @return The BukkitAudiences instance
     */
    public BukkitAudiences getAudiences() {
        return audiences;
    }
    
    /**
     * Closes the audiences manager. Should be called on plugin disable.
     */
    public void close() {
        if (audiences != null) {
            audiences.close();
        }
    }
}

