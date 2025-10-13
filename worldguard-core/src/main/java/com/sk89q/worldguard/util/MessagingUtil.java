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

package com.sk89q.worldguard.util;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.commands.CommandUtils;

/**
 * Utility class for messaging.
 * 
 * Note: For MiniMessage support, use the MiniMessageHelper class
 * from the bukkit module instead.
 */
public final class MessagingUtil {

    private MessagingUtil() {
    }

    /**
     * Sends a string message to a player's chat.
     * Supports legacy color codes and macro replacement.
     * 
     * @param player The player to send the message to
     * @param message The message to send
     */
    public static void sendStringToChat(LocalPlayer player, String message) {
        String effective = CommandUtils.replaceColorMacros(message);
        effective = WorldGuard.getInstance().getPlatform().getMatcher().replaceMacros(player, effective);
        for (String mess : effective.replaceAll("\\\\n", "\n").split("\\n")) {
            player.printRaw(mess);
        }
    }

    /**
     * Sends a string message as a title to a player.
     * Supports legacy color codes and macro replacement.
     * 
     * @param player The player to send the title to
     * @param message The message to send (can contain \n for subtitle)
     */
    public static void sendStringToTitle(LocalPlayer player, String message) {
        String[] parts = message.replaceAll("\\\\n", "\n").split("\\n", 2);
        String title = CommandUtils.replaceColorMacros(parts[0]);
        title = WorldGuard.getInstance().getPlatform().getMatcher().replaceMacros(player, title);
        if (parts.length > 1) {
            String subtitle = CommandUtils.replaceColorMacros(parts[1]);
            subtitle = WorldGuard.getInstance().getPlatform().getMatcher().replaceMacros(player, subtitle);
            player.sendTitle(title, subtitle);
        } else {
            player.sendTitle(title, null);
        }
    }

}
