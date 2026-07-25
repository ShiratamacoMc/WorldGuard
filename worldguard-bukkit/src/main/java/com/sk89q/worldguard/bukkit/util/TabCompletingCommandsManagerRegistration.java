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

import com.sk89q.bukkit.util.CommandInfo;
import com.sk89q.bukkit.util.CommandInspector;
import com.sk89q.bukkit.util.CommandsManagerRegistration;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.CommandsManager;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Registers legacy CommandsManager commands with Bukkit tab completion enabled.
 */
@SuppressWarnings("deprecation")
public final class TabCompletingCommandsManagerRegistration extends CommandsManagerRegistration {

    private final CommandInspector inspector;

    public TabCompletingCommandsManagerRegistration(
            Plugin plugin, CommandsManager<?> commands, CommandInspector inspector) {
        super(plugin, commands);
        this.inspector = inspector;
    }

    @Override
    public boolean registerAll(List<Command> registered) {
        List<CommandInfo> toRegister = new ArrayList<>();
        for (Command command : registered) {
            List<String> permissions = null;
            Method commandMethod = commands.getMethods().get(null).get(command.aliases()[0]);
            Map<String, Method> childMethods = commands.getMethods().get(commandMethod);

            if (commandMethod != null && commandMethod.isAnnotationPresent(CommandPermissions.class)) {
                permissions = Arrays.asList(commandMethod.getAnnotation(CommandPermissions.class).value());
            } else if (commandMethod != null && childMethods != null && !childMethods.isEmpty()) {
                permissions = new ArrayList<>();
                for (Method child : childMethods.values()) {
                    if (child.isAnnotationPresent(CommandPermissions.class)) {
                        permissions.addAll(Arrays.asList(child.getAnnotation(CommandPermissions.class).value()));
                    }
                }
            }

            toRegister.add(new CommandInfo(command.usage(), command.desc(), command.aliases(), inspector,
                    permissions == null ? null : permissions.toArray(new String[0])));
        }
        return register(toRegister);
    }
}
