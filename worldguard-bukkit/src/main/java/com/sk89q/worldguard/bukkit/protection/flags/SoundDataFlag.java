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

package com.sk89q.worldguard.bukkit.protection.flags;

import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.FlagContext;
import com.sk89q.worldguard.protection.flags.InvalidFlagFormat;

import org.bukkit.SoundCategory;

import java.util.Locale;

public class SoundDataFlag extends Flag<SoundData> {
    
    public SoundDataFlag(String name) {
        super(name);
    }

    @Override
    public Object marshal(SoundData o) {
        return o.sound() + " " + o.interval() + " " + o.source() + " " + o.volume() + " " + o.pitch();
    }

    @Override
    public SoundData parseInput(FlagContext context) throws InvalidFlagFormat {
        String[] splitd = context.getUserInput().trim().split(" ");
        if (splitd.length >= 2 && splitd.length <= 5) {
            return this.getSoundData(splitd);
        } else {
            throw new InvalidFlagFormat("Please use format: <sound name> <interval in ticks> [source] [volume] [pitch]");
        }
    }

    @Override
    public SoundData unmarshal(Object o) {
        String[] splitd = o.toString().split(" ");

        return this.getSoundData(splitd);
    }

    private SoundData getSoundData(String[] splitd) {
        return new SoundData(
                splitd[0],
                Integer.parseInt(splitd[1]),
                splitd.length >= 3 ? SoundCategory.valueOf(splitd[2].toUpperCase(Locale.ROOT)) : SoundCategory.MASTER,
                splitd.length >= 4 ? Float.parseFloat(splitd[3]) : Float.MAX_VALUE,
                splitd.length >= 5 ? Float.parseFloat(splitd[4]) : 1
        );
    }
}
