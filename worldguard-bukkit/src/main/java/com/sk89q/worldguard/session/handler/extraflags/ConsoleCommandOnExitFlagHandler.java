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

package com.sk89q.worldguard.session.handler.extraflags;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import com.google.common.collect.Lists;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.session.handler.Handler;
import org.bukkit.Bukkit;

import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.session.MoveType;
import com.sk89q.worldguard.session.Session;

import com.sk89q.worldguard.protection.flags.Flags;

public class ConsoleCommandOnExitFlagHandler extends Handler
{
	public static final Factory FACTORY()
	{
		return new Factory();
	}
	
    public static class Factory extends Handler.Factory<ConsoleCommandOnExitFlagHandler>
    {
		@Override
        public ConsoleCommandOnExitFlagHandler create(Session session)
        {
            return new ConsoleCommandOnExitFlagHandler(session);
        }
    }
    
	private Collection<Set<String>> lastCommands;
	    
	protected ConsoleCommandOnExitFlagHandler(Session session)
	{
		super(session);
		
		this.lastCommands = new ArrayList<>();
	}

    @Override
	public void initialize(LocalPlayer player, Location current, ApplicableRegionSet set)
    {
    	this.lastCommands = set.queryAllValues(player, Flags.CONSOLE_COMMAND_ON_EXIT);
    }
    	
	@Override
	public boolean onCrossBoundary(LocalPlayer player, Location from, Location to, ApplicableRegionSet toSet, Set<ProtectedRegion> entered, Set<ProtectedRegion> exited, MoveType moveType)
	{
		Collection<Set<String>> commands = Lists.newArrayList(toSet.queryAllValues(player, Flags.CONSOLE_COMMAND_ON_EXIT));
		
		if (!commands.isEmpty())
		{
			for (ProtectedRegion region : toSet)
			{
                Set<String> commands_ = region.getFlag(Flags.CONSOLE_COMMAND_ON_EXIT);
                if (commands_ != null)
                {
                	commands.add(commands_);
                }
            }
		}

		Collection<Set<String>> lastCommands = this.lastCommands;

		this.lastCommands = commands;

		if (!this.getSession().getManager().hasBypass(player, (World) to.getExtent()))
		{
			for (Set<String> commands_ : lastCommands)
			{
				if (!commands.contains(commands_))
				{
					for (String command : commands_)
					{
						Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command.substring(1).replace("%username%", player.getName())); //TODO: Make this better
					}

					break;
				}
			}
		}
		
		return true;
	}
}

