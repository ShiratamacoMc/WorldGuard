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

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.BukkitWorldConfiguration;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.bukkit.event.DelegateEvent;
import com.sk89q.worldguard.bukkit.event.block.BreakBlockEvent;
import com.sk89q.worldguard.bukkit.event.block.PlaceBlockEvent;
import com.sk89q.worldguard.bukkit.event.block.UseBlockEvent;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.SignChangeEvent;

/**
 * Handle events that need to be processed by the chest protection.
 */
public class ChestProtectionListener extends AbstractListener {

    private final ListenerMessageHelper messageHelper;

    /**
     * Construct the listener.
     *
     * @param plugin an instance of WorldGuardPlugin
     */
    public ChestProtectionListener(WorldGuardPlugin plugin) {
        super(plugin);
        this.messageHelper = new ListenerMessageHelper(plugin);
    }

    private void sendMessage(DelegateEvent event, Player player, String messageKey) {
        if (!event.isSilent()) {
            messageHelper.sendMessage(player, messageKey);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlaceBlock(final PlaceBlockEvent event) {
        final Player player = event.getCause().getFirstPlayer();

        if (player != null) {
            final BukkitWorldConfiguration wcfg = getWorldConfig(event.getWorld());

            // Early guard
            if (wcfg == null || !wcfg.signChestProtection) {
                return;
            }

            event.filter(target -> {
                if (wcfg.getChestProtection().isChest(BukkitAdapter.asBlockType(event.getEffectiveMaterial())) && wcfg.isChestProtected(BukkitAdapter.adapt(target.getBlock().getLocation()),
                        WorldGuardPlugin.inst().wrapPlayer(player))) {
                    sendMessage(event, player, "chest.protected-spot");
                    return false;
                }

                return true;
            }, true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreakBlock(final BreakBlockEvent event) {
        final Player player = event.getCause().getFirstPlayer();

        final BukkitWorldConfiguration wcfg = getWorldConfig(event.getWorld());

        // Early guard
        if (wcfg == null || !wcfg.signChestProtection) {
            return;
        }

        if (player != null) {
            final LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
            event.filter(target -> {
                if (wcfg.isChestProtected(BukkitAdapter.adapt(target.getBlock().getLocation()), localPlayer)) {
                    sendMessage(event, player, "chest.protected-chest");
                    return false;
                }

                return true;
            }, true);
        } else {
            event.filter(target -> !wcfg.isChestProtected(BukkitAdapter.adapt(target.getBlock().getLocation())));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onUseBlock(final UseBlockEvent event) {
        final Player player = event.getCause().getFirstPlayer();

        final BukkitWorldConfiguration wcfg = getWorldConfig(event.getWorld());

        // Early guard
        if (wcfg == null || !wcfg.signChestProtection) {
            return;
        }

        if (player != null) {
            final LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
            event.filter(target -> {
                if (wcfg.isChestProtected(BukkitAdapter.adapt(target.getBlock().getLocation()), localPlayer)) {
                    sendMessage(event, player, "chest.protected-chest");
                    return false;
                }

                return true;
            }, true);
        } else {
            event.filter(target -> !wcfg.isChestProtected(BukkitAdapter.adapt(target.getBlock().getLocation())));
        }
    }

    @EventHandler(ignoreCancelled = true)
    @SuppressWarnings("deprecation")
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        final BukkitWorldConfiguration wcfg = getWorldConfig(event.getBlock().getWorld());

        if (wcfg != null && wcfg.signChestProtection) {
            if ("[Lock]".equalsIgnoreCase(event.getLine(0))) {
                if (wcfg.isChestProtectedPlacement(BukkitAdapter.adapt(event.getBlock().getLocation()), WorldGuardPlugin.inst().wrapPlayer(player))) {
                    messageHelper.sendMessage(player, "chest.adjacent-not-owned");
                    event.getBlock().breakNaturally();
                    event.setCancelled(true);
                    return;
                }

                if (!Tag.STANDING_SIGNS.isTagged(event.getBlock().getType())) {
                    messageHelper.sendMessage(player, "chest.sign-must-be-post");

                    event.getBlock().breakNaturally();
                    event.setCancelled(true);
                    return;
                }

                if (!player.getName().equalsIgnoreCase(event.getLine(1))) {
                    messageHelper.sendMessage(player, "chest.owner-must-be-you");

                    event.getBlock().breakNaturally();
                    event.setCancelled(true);
                    return;
                }

                Material below = event.getBlock().getRelative(0, -1, 0).getType();

                if (below == Material.TNT || below == Material.SAND
                        || below == Material.GRAVEL || Tag.STANDING_SIGNS.isTagged(below)) {
                    messageHelper.sendMessage(player, "chest.unsafe-block");

                    event.getBlock().breakNaturally();
                    event.setCancelled(true);
                    return;
                }

                event.setLine(0, "[Lock]");
                messageHelper.sendMessage(player, "chest.protected-by-you");
            }
        } else if (wcfg != null && !wcfg.disableSignChestProtectionCheck) {
            if ("[Lock]".equalsIgnoreCase(event.getLine(0))) {
                messageHelper.sendMessage(player, "chest.protection-disabled");

                event.getBlock().breakNaturally();
                event.setCancelled(true);
            }
        }
    }

}
