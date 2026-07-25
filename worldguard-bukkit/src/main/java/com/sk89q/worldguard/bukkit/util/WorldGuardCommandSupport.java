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

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Provides localized command help and context-aware tab completion.
 */
public final class WorldGuardCommandSupport {

    private static final int ENTRIES_PER_PAGE = 6;
    private static final List<HelpEntry> WORLDGUARD_ENTRIES = Arrays.asList(
            entry("help", "help.help", null), entry("version", "help.version", null),
            entry("reload", "help.reload", "worldguard.reload"), entry("report", "help.report", "worldguard.report"),
            entry("running", "help.running", "worldguard.running"),
            entry("stopfire [world]", "help.stopfire", "worldguard.fire-toggle.stop"),
            entry("allowfire [world]", "help.allowfire", "worldguard.fire-toggle.stop"),
            entry("halt-activity [confirm]", "help.halt-activity", "worldguard.halt-activity")
    );
    private static final List<HelpEntry> REGION_ENTRIES = Arrays.asList(
            entry("help", "help.help", null), entry("define <id>", "help.region-define", "worldguard.region.define"),
            entry("redefine <id>", "help.region-redefine", "worldguard.region.redefine"),
            entry("claim <id>", "help.region-claim", "worldguard.region.claim"),
            entry("info [id]", "help.region-info", "worldguard.region.info"),
            entry("list [world] [page]", "help.region-list", "worldguard.region.list"),
            entry("flag <id> <flag> [value]", "help.region-flag", "worldguard.region.flag"),
            entry("flags [id]", "help.region-flags", "worldguard.region.flag"),
            entry("priority <id> <priority>", "help.region-priority", "worldguard.region.setpriority"),
            entry("setparent <id> [parent]", "help.region-parent", "worldguard.region.setparent"),
            entry("remove <id>", "help.region-remove", "worldguard.region.remove"),
            entry("bypass [player]", "help.region-bypass", "worldguard.region.bypass"),
            entry("teleport <id>", "help.region-teleport", "worldguard.region.teleport")
    );
    private static final List<Completion> WORLDGUARD_COMPLETIONS = Arrays.asList(
            completion("help", null), completion("version", null), completion("reload", "worldguard.reload"),
            completion("report", "worldguard.report"), completion("running", "worldguard.running"),
            completion("queue", "worldguard.running"), completion("profile", "worldguard.profile"),
            completion("stopprofile", "worldguard.profile"), completion("flushstates", "worldguard.flushstates"),
            completion("clearstates", "worldguard.flushstates"), completion("debug", "worldguard.debug")
    );
    private static final List<Completion> REGION_COMPLETIONS = Arrays.asList(
            completion("help", null), completion("define", "worldguard.region.define"),
            completion("redefine", "worldguard.region.redefine"), completion("claim", "worldguard.region.claim"),
            completion("info", "worldguard.region.info"), completion("list", "worldguard.region.list"),
            completion("flag", "worldguard.region.flag"), completion("flags", "worldguard.region.flag"),
            completion("priority", "worldguard.region.setpriority"), completion("setparent", "worldguard.region.setparent"),
            completion("remove", "worldguard.region.remove"), completion("load", "worldguard.region.load"),
            completion("save", "worldguard.region.save"), completion("teleport", "worldguard.region.teleport"),
            completion("bypass", "worldguard.region.bypass")
    );

    private final WorldGuardPlugin plugin;

    public WorldGuardCommandSupport(WorldGuardPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean handleHelp(CommandSender sender, String label, String[] args) {
        if (!isHelpLabel(label)) {
            return false;
        }
        if (args.length == 0 || (args.length <= 2 && isHelpArgument(args[0]))) {
            int page = args.length == 2 ? parsePage(args[1]) : 1;
            sendHelp(sender, label, page);
            return true;
        }
        return false;
    }

    public void sendHelp(CommandSender sender, String label, int requestedPage) {
        List<HelpEntry> entries = visibleEntries(sender, isRegionLabel(label) ? REGION_ENTRIES : WORLDGUARD_ENTRIES);
        if (entries.isEmpty()) {
            plugin.getMiniMessageHelper().sendMessage(sender, plugin.getMessageManager().getMessage("help.no-entries"));
            return;
        }

        int pageCount = Math.max(1, (entries.size() + ENTRIES_PER_PAGE - 1) / ENTRIES_PER_PAGE);
        int page = Math.max(1, Math.min(requestedPage, pageCount));
        int from = (page - 1) * ENTRIES_PER_PAGE;
        int to = Math.min(from + ENTRIES_PER_PAGE, entries.size());

        String title = plugin.getMessageManager().getMessage(isRegionLabel(label) ? "help.title-region" : "help.title-worldguard");
        Component output = plugin.getMiniMessageHelper().parse(plugin.getMessageManager().getMessage(
                "help.header", "title", title, "page", page, "pages", pageCount));
        for (HelpEntry entry : entries.subList(from, to)) {
            output = output.append(Component.newline()).append(createEntry(label, entry));
        }
        output = output.append(Component.newline()).append(createNavigation(label, page, pageCount));
        plugin.getMiniMessageHelper().sendComponent(sender, output);
    }

    public List<String> complete(CommandSender sender, String label, String[] args) {
        if (isWorldGuardLabel(label)) {
            if (args.length == 1) {
                return completeEntries(sender, WORLDGUARD_COMPLETIONS, args[0]);
            }
            if (args.length == 2 && isHelpArgument(args[0])) {
                return filter(helpPages(sender, WORLDGUARD_ENTRIES), args[1]);
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("debug")) {
                return filter(Arrays.asList("testbreak", "testplace", "testinteract", "testdamage"), args[1]);
            }
            return List.of();
        }

        if (isRegionLabel(label)) {
            return completeRegion(sender, args);
        }

        String normalized = label.toLowerCase(Locale.ROOT);
        if ((normalized.equals("stopfire") || normalized.equals("allowfire")) && args.length == 1) {
            return filter(whitelistedWorldNames(), args[0]);
        }
        if (Arrays.asList("god", "ungod", "heal", "slay", "locate").contains(normalized) && args.length == 1) {
            return filter(onlinePlayerNames(), args[0]);
        }
        return List.of();
    }

    private List<String> completeRegion(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return completeEntries(sender, REGION_COMPLETIONS, args[0]);
        }
        if (args.length == 2 && isHelpArgument(args[0])) {
            return filter(helpPages(sender, REGION_ENTRIES), args[1]);
        }
        if (args.length == 2 && expectsRegionId(args[0])) {
            return filter(regionIds(sender), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("flag")) {
            return filter(flagNames(), args[2]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("setparent")) {
            return filter(regionIds(sender), args[2]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("list")) {
            return filter(whitelistedWorldNames(), args[1]);
        }
        return List.of();
    }

    private Component createEntry(String label, HelpEntry entry) {
        String suggestion = "/" + label + " " + entry.command;
        Component command = Component.text(suggestion, NamedTextColor.AQUA)
                .clickEvent(ClickEvent.suggestCommand(suggestion))
                .hoverEvent(HoverEvent.showText(plugin.getMiniMessageHelper().parse(
                        plugin.getMessageManager().getMessage(entry.descriptionKey))));
        return command.append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                .append(plugin.getMiniMessageHelper().parse(plugin.getMessageManager().getMessage(entry.descriptionKey)));
    }

    private Component createNavigation(String label, int page, int pageCount) {
        String rootCommand = isRegionLabel(label) ? "region" : "worldguard";
        Component navigation = Component.empty();
        if (page > 1) {
            navigation = navigation.append(navigationButton("help.previous", "/" + rootCommand + " help " + (page - 1)));
        }
        navigation = navigation.append(Component.text(" ", NamedTextColor.GRAY))
                .append(plugin.getMiniMessageHelper().parse(plugin.getMessageManager().getMessage(
                        "help.page", "page", page, "pages", pageCount)));
        if (page < pageCount) {
            navigation = navigation.append(Component.text(" ", NamedTextColor.GRAY))
                    .append(navigationButton("help.next", "/" + rootCommand + " help " + (page + 1)));
        }
        return navigation;
    }

    private Component navigationButton(String key, String command) {
        Component tooltip = plugin.getMiniMessageHelper().parse(
                plugin.getMessageManager().getMessage("help.click-to-open"));
        return makeInteractive(plugin.getMiniMessageHelper().parse(plugin.getMessageManager().getMessage(key)),
                ClickEvent.runCommand(command), tooltip);
    }

    private Component makeInteractive(Component component, ClickEvent clickEvent, Component hoverText) {
        // Child text inherits the interaction from the MiniMessage component root.
        return component.clickEvent(clickEvent)
                .hoverEvent(HoverEvent.showText(hoverText));
    }

    private List<HelpEntry> visibleEntries(CommandSender sender, List<HelpEntry> entries) {
        return entries.stream().filter(entry -> entry.permission == null || plugin.hasPermission(sender, entry.permission))
                .collect(Collectors.toList());
    }

    private List<String> helpPages(CommandSender sender, List<HelpEntry> entries) {
        int pageCount = Math.max(1, (visibleEntries(sender, entries).size() + ENTRIES_PER_PAGE - 1) / ENTRIES_PER_PAGE);
        List<String> pages = new ArrayList<>();
        for (int page = 1; page <= pageCount; page++) {
            pages.add(String.valueOf(page));
        }
        return pages;
    }

    private List<String> completeEntries(CommandSender sender, List<Completion> completions, String prefix) {
        return filter(completions.stream()
                .filter(entry -> entry.permission == null || plugin.hasPermission(sender, entry.permission))
                .map(entry -> entry.value)
                .collect(Collectors.toList()), prefix);
    }

    private List<String> whitelistedWorldNames() {
        return Bukkit.getWorlds().stream()
                .filter(world -> plugin.getConfigManager().isWorldWhitelisted(world.getName()))
                .map(world -> world.getName())
                .collect(Collectors.toList());
    }

    private List<String> onlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
    }

    private List<String> regionIds(CommandSender sender) {
        if (!(sender instanceof Player)) {
            return List.of();
        }
        Player player = (Player) sender;
        if (!plugin.getConfigManager().isWorldWhitelisted(player.getWorld().getName())) {
            return List.of();
        }
        RegionManager manager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(player.getWorld()));
        return manager == null ? List.of() : new ArrayList<>(manager.getRegions().keySet());
    }

    private List<String> flagNames() {
        return WorldGuard.getInstance().getFlagRegistry().getAll().stream()
                .map(Flag::getName).collect(Collectors.toList());
    }

    private List<String> filter(Collection<String> candidates, String prefix) {
        String normalizedPrefix = prefix.toLowerCase(Locale.ROOT);
        return candidates.stream().filter(candidate -> candidate.toLowerCase(Locale.ROOT).startsWith(normalizedPrefix))
                .sorted(Comparator.naturalOrder()).collect(Collectors.toList());
    }

    private boolean expectsRegionId(String command) {
        String normalized = command.toLowerCase(Locale.ROOT);
        return Arrays.asList("redefine", "info", "i", "flag", "f", "flags", "priority", "pri",
                "setpriority", "setparent", "parent", "par", "remove", "delete", "del", "rem",
                "teleport", "tp").contains(normalized);
    }

    private boolean isHelpLabel(String label) {
        return isWorldGuardLabel(label) || isRegionLabel(label);
    }

    public boolean isHelpCommandLabel(String label) {
        return isHelpLabel(label);
    }

    private boolean isHelpArgument(String argument) {
        return argument.equalsIgnoreCase("help") || argument.equals("?");
    }

    private boolean isWorldGuardLabel(String label) {
        String normalized = normalizeLabel(label);
        return normalized.equals("wg") || normalized.equals("worldguard");
    }

    private boolean isRegionLabel(String label) {
        String normalized = normalizeLabel(label);
        return normalized.equals("rg") || normalized.equals("region") || normalized.equals("regions");
    }

    private String normalizeLabel(String label) {
        String normalized = label.toLowerCase(Locale.ROOT);
        int namespaceSeparator = normalized.lastIndexOf(':');
        return namespaceSeparator >= 0 ? normalized.substring(namespaceSeparator + 1) : normalized;
    }

    private int parsePage(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private static HelpEntry entry(String usage, String descriptionKey, String permission) {
        return new HelpEntry(usage, usage.replaceAll("\\s*<[^>]+>", "").replaceAll("\\s*\\[[^]]+\\]", ""),
                descriptionKey, permission);
    }

    private static Completion completion(String value, String permission) {
        return new Completion(value, permission);
    }

    private static final class HelpEntry {
        private final String usage;
        private final String command;
        private final String descriptionKey;
        private final String permission;

        private HelpEntry(String usage, String command, String descriptionKey, String permission) {
            this.usage = usage;
            this.command = command;
            this.descriptionKey = descriptionKey;
            this.permission = permission;
        }
    }

    private static final class Completion {
        private final String value;
        private final String permission;

        private Completion(String value, String permission) {
            this.value = value;
            this.permission = permission;
        }
    }
}
