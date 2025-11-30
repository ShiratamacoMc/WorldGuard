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

import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DoubleFlag;
import com.sk89q.worldguard.session.MoveType;
import com.sk89q.worldguard.session.Session;
import com.sk89q.worldguard.session.handler.FlagValueChangeHandler;
import org.bukkit.entity.Player;

public abstract class AbstractSpeedFlagHandler extends FlagValueChangeHandler<Double>
{
	private Float originalSpeed;
	
	protected AbstractSpeedFlagHandler(Session session, DoubleFlag flag)
	{
		super(session, flag);
	}
	
	protected abstract float getSpeed(Player player);
	protected abstract void setSpeed(Player player, float speed);

	@Override
	protected void onInitialValue(LocalPlayer player, ApplicableRegionSet set, Double value)
	{
		this.handleValue(player, player.getWorld(), value);
	}

	@Override
	protected boolean onSetValue(LocalPlayer player, Location from, Location to, ApplicableRegionSet toSet, Double currentValue, Double lastValue, MoveType moveType)
	{
		this.handleValue(player, (World) to.getExtent(), currentValue);
		return true;
	}

	@Override
	protected boolean onAbsentValue(LocalPlayer player, Location from, Location to, ApplicableRegionSet toSet, Double lastValue, MoveType moveType)
	{
		this.handleValue(player, (World) to.getExtent(), null);
		return true;
	}

	private void handleValue(LocalPlayer player, World world, Double speed)
	{
		Player bukkitPlayer = ((BukkitPlayer) player).getPlayer();

		if (!this.getSession().getManager().hasBypass(player, world) && speed != null)
		{
			if (speed > 1.0)
			{
				speed = 1.0;
			}
			else if (speed < -1.0)
			{
				speed = -1.0;
			}
			
			if (this.getSpeed(bukkitPlayer) != speed.floatValue())
			{
				if (this.originalSpeed == null)
				{
					this.originalSpeed = this.getSpeed(bukkitPlayer);
				}
				
				this.setSpeed(bukkitPlayer, speed.floatValue());
			}
		}
		else
		{
			if (this.originalSpeed != null)
			{
				this.setSpeed(bukkitPlayer, this.originalSpeed);
				
				this.originalSpeed = null;
			}
		}
	}
}

