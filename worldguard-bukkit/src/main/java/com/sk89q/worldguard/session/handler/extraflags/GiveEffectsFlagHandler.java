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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.session.handler.FlagValueChangeHandler;
import com.sk89q.worldguard.session.handler.Handler;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.session.MoveType;
import com.sk89q.worldguard.session.Session;


import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.bukkit.protection.flags.PotionEffectDetails;
import com.sk89q.worldguard.bukkit.util.WorldGuardExtraFlagsUtils;

public class GiveEffectsFlagHandler extends FlagValueChangeHandler<Set<PotionEffect>>
{
	public static final Factory FACTORY()
	{
		return new Factory();
	}
	
    public static class Factory extends Handler.Factory<GiveEffectsFlagHandler>
    {
		@Override
        public GiveEffectsFlagHandler create(Session session)
        {
            return new GiveEffectsFlagHandler(session);
        }
    }

	private Map<PotionEffectType, PotionEffectDetails> removedEffects;
    private Set<PotionEffectType> givenEffects; private boolean supressRemovePotionPacket;
    
	protected GiveEffectsFlagHandler(Session session)
	{
		super(session, com.sk89q.worldguard.bukkit.protection.flags.BukkitExtraFlags.GIVE_EFFECTS);
		
		this.removedEffects = new HashMap<>();
		this.givenEffects = new HashSet<>();
	}

	@Override
	protected void onInitialValue(LocalPlayer player, ApplicableRegionSet set, Set<PotionEffect> value)
	{
		this.handleValue(player, player.getWorld(), value);
	}

	@Override
	protected boolean onSetValue(LocalPlayer player, Location from, Location to, ApplicableRegionSet toSet, Set<PotionEffect> currentValue, Set<PotionEffect> lastValue, MoveType moveType)
	{
		this.handleValue(player, (World) to.getExtent(), currentValue);
		return true;
	}

	@Override
	protected boolean onAbsentValue(LocalPlayer player, Location from, Location to, ApplicableRegionSet toSet, Set<PotionEffect> lastValue, MoveType moveType)
	{
		this.handleValue(player, (World) to.getExtent(), null);
		return true;
	}
	
	@Override
	public void tick(LocalPlayer player, ApplicableRegionSet set)
	{
		this.handleValue(player, player.getWorld(), set.queryValue(player, com.sk89q.worldguard.bukkit.protection.flags.BukkitExtraFlags.GIVE_EFFECTS));
	}
	
	private void handleValue(LocalPlayer player, World world, Set<PotionEffect> value)
	{
		Player bukkitPlayer = ((BukkitPlayer) player).getPlayer();

		if (!this.getSession().getManager().hasBypass(player, world) && value != null)
		{
			try
			{
				for (PotionEffect effect : value)
				{
					PotionEffect effect_ = null;
					for(PotionEffect activeEffect : bukkitPlayer.getActivePotionEffects())
					{
						if (activeEffect.getType().equals(effect.getType()))
						{
							effect_ = activeEffect;
							break;
						}
					}
					
					this.supressRemovePotionPacket = effect_ != null && effect_.getAmplifier() == effect.getAmplifier();
	
					if (this.givenEffects.add(effect.getType()) && effect_ != null)
					{
						this.removedEffects.put(effect_.getType(), new PotionEffectDetails(System.nanoTime() + (long)(effect_.getDuration() / 20D * TimeUnit.SECONDS.toNanos(1L)), effect_.getAmplifier(), effect_.isAmbient(), effect_.hasParticles()));

						bukkitPlayer.removePotionEffect(effect_.getType());
					}

					bukkitPlayer.addPotionEffect(effect);
				}
			}
			finally
			{
				this.supressRemovePotionPacket = false;
			}
		}
		
		Iterator<PotionEffectType> effectTypes = this.givenEffects.iterator();
		while (effectTypes.hasNext())
		{
			PotionEffectType type = effectTypes.next();
			
			if (value != null && value.size() > 0)
			{
				boolean skip = false;
				for (PotionEffect effect : value)
				{
					if (effect.getType().equals(type))
					{
						skip = true;
						break;
					}
				}
				
				if (skip)
				{
					continue;
				}
			}

			bukkitPlayer.removePotionEffect(type);
			
			effectTypes.remove();
		}
		
		Iterator<Entry<PotionEffectType, PotionEffectDetails>> potionEffects_ = this.removedEffects.entrySet().iterator();
		while (potionEffects_.hasNext())
		{
			Entry<PotionEffectType, PotionEffectDetails> effect = potionEffects_.next();
			if (!this.givenEffects.contains(effect.getKey()))
			{
				PotionEffectDetails removedEffect = effect.getValue();
				if (removedEffect != null)
				{
					int timeLeft = removedEffect.getTimeLeftInTicks();
					
					if (timeLeft > 0)
					{
						bukkitPlayer.addPotionEffect(new PotionEffect(effect.getKey(), timeLeft, removedEffect.getAmplifier(), removedEffect.isAmbient(), removedEffect.isParticles()));
					}
				}
				
				potionEffects_.remove();
			}
		}
	}
	
	public void drinkMilk(Player bukkitPlayer)
	{
		this.removedEffects.clear();

		LocalPlayer player = WorldGuardPlugin.inst().wrapPlayer(bukkitPlayer);

		this.handleValue(player, player.getWorld(), WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery().getApplicableRegions(player.getLocation()).queryValue(player, com.sk89q.worldguard.bukkit.protection.flags.BukkitExtraFlags.GIVE_EFFECTS));
	}
	
	public void drinkPotion(Player bukkitPlayer, Collection<PotionEffect> effects)
	{
		for(PotionEffect effect : effects)
		{
			this.removedEffects.put(effect.getType(), new PotionEffectDetails(System.nanoTime() + (long)(effect.getDuration() / 20D * TimeUnit.SECONDS.toNanos(1L)), effect.getAmplifier(), effect.isAmbient(), effect.hasParticles()));
		}

		LocalPlayer player = WorldGuardPlugin.inst().wrapPlayer(bukkitPlayer);
		
		this.handleValue(player, player.getWorld(), WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery().getApplicableRegions(player.getLocation()).queryValue(player, com.sk89q.worldguard.bukkit.protection.flags.BukkitExtraFlags.GIVE_EFFECTS));
	}
}

