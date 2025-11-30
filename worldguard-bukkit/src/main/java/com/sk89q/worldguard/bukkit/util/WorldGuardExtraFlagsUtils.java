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

import java.util.HashSet;
import java.util.Set;

import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class WorldGuardExtraFlagsUtils {
    
    public static final String PREVENT_TELEPORT_LOOP_META = "WG: TLP";
    
    @SuppressWarnings("unchecked")
    public static boolean hasNoTeleportLoop(Plugin plugin, Player player, Object location) {
        MetadataValue result = player.getMetadata(PREVENT_TELEPORT_LOOP_META).stream()
                .filter((p) -> p.getOwningPlugin().equals(plugin))
                .findFirst()
                .orElse(null);
        
        if (result == null) {
            result = new FixedMetadataValue(plugin, new HashSet<>());
            
            player.setMetadata(PREVENT_TELEPORT_LOOP_META, result);
            
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.removeMetadata(PREVENT_TELEPORT_LOOP_META, plugin);
                }
            }.runTask(plugin);
        }
        
        Set<Object> set = (Set<Object>)result.value();
        if (set.add(location)) {
            return true;
        }
        
        return false;
    }
}
