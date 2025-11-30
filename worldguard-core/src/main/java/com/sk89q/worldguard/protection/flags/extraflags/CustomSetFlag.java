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

package com.sk89q.worldguard.protection.flags.extraflags;

import java.util.Set;

import com.google.common.collect.Sets;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.FlagContext;
import com.sk89q.worldguard.protection.flags.InvalidFlagFormat;
import com.sk89q.worldguard.protection.flags.SetFlag;

public class CustomSetFlag<T> extends SetFlag<T> {
    
    public CustomSetFlag(String name, Flag<T> subFlag) {
        super(name, subFlag);
    }
    
    @Override
    public Set<T> parseInput(FlagContext context) throws InvalidFlagFormat {
        String input = context.getUserInput();
        if (input.isEmpty()) {
            return Sets.newHashSet();
        } else {
            Set<T> items = Sets.newLinkedHashSet();

            for (String str : input.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1)) {
                if (str.startsWith("\"") && str.endsWith("\"")) {
                    str = str.substring(1, str.length() - 1);
                }

                FlagContext copy = context.copyWith(null, str, null);
                items.add(this.getType().parseInput(copy));
            }

            return items;
        }
    }

    @Override
    public Set<T> unmarshal(Object o) {
        if (!(o instanceof Iterable<?> iterable)) {
            return null;
        }

        Set<T> items = Sets.newLinkedHashSet();

        iterable.forEach(i -> {
            final T value = this.getType().unmarshal(i);
            if (value != null) {
                items.add(value);
            }
        });

        return items;
    }
}
