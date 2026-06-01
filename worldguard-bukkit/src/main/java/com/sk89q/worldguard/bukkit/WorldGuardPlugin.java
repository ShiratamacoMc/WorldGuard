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

package com.sk89q.worldguard.bukkit;

import com.google.common.collect.ImmutableList;
import com.sk89q.bukkit.util.CommandsManagerRegistration;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissionsException;
import com.sk89q.minecraft.util.commands.CommandUsageException;
import com.sk89q.minecraft.util.commands.CommandsManager;
import com.sk89q.minecraft.util.commands.MissingNestedCommandException;
import com.sk89q.minecraft.util.commands.SimpleInjector;
import com.sk89q.minecraft.util.commands.WrappedCommandException;
import com.sk89q.wepif.PermissionsResolverManager;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitCommandSender;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.util.concurrency.LazyReference;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.blacklist.Blacklist;
import com.sk89q.worldguard.config.WorldConfiguration;
import com.sk89q.worldguard.bukkit.event.player.ProcessPlayerEvent;
import com.sk89q.worldguard.bukkit.listener.BlacklistListener;
import com.sk89q.worldguard.bukkit.listener.BlockedPotionsListener;
import com.sk89q.worldguard.bukkit.listener.BuildPermissionListener;
import com.sk89q.worldguard.bukkit.listener.ChestProtectionListener;
import com.sk89q.worldguard.bukkit.listener.DebuggingListener;
import com.sk89q.worldguard.bukkit.listener.EventAbstractionListener;
import com.sk89q.worldguard.bukkit.listener.InvincibilityListener;
import com.sk89q.worldguard.bukkit.listener.PlayerModesListener;
import com.sk89q.worldguard.bukkit.listener.PlayerMoveListener;
import com.sk89q.worldguard.bukkit.listener.RegionFlagsListener;
import com.sk89q.worldguard.bukkit.listener.RegionProtectionListener;
import com.sk89q.worldguard.bukkit.listener.WorldGuardBlockListener;
import com.sk89q.worldguard.bukkit.listener.WorldGuardCommandBookListener;
import com.sk89q.worldguard.bukkit.listener.WorldGuardEntityListener;
import com.sk89q.worldguard.bukkit.listener.WorldGuardHangingListener;
import com.sk89q.worldguard.bukkit.listener.WorldGuardPlayerListener;
import com.sk89q.worldguard.bukkit.listener.WorldGuardServerListener;
import com.sk89q.worldguard.bukkit.listener.WorldGuardVehicleListener;
import com.sk89q.worldguard.bukkit.listener.WorldGuardWeatherListener;
import com.sk89q.worldguard.bukkit.listener.WorldGuardWorldListener;
import com.sk89q.worldguard.bukkit.listener.WorldRulesListener;
import com.sk89q.worldguard.bukkit.session.BukkitSessionManager;
import com.sk89q.worldguard.bukkit.util.ClassSourceValidator;
import com.sk89q.worldguard.bukkit.util.Entities;
import com.sk89q.worldguard.bukkit.util.Events;
import com.sk89q.worldguard.bukkit.i18n.I18nConfig;
import com.sk89q.worldguard.bukkit.i18n.MessageManager;
import com.sk89q.worldguard.bukkit.i18n.MiniMessageHelper;
import com.sk89q.worldguard.commands.GeneralCommands;
import com.sk89q.worldguard.commands.ProtectionCommands;
import com.sk89q.worldguard.commands.ToggleCommands;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.registry.SimpleFlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.storage.RegionDriver;
import com.sk89q.worldguard.protection.managers.storage.file.DirectoryYamlDriver;
import com.sk89q.worldguard.protection.managers.storage.sql.SQLDriver;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.util.logging.RecordMessagePrefixer;
import io.papermc.lib.PaperLib;
import io.papermc.paper.ServerBuildInfo;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The main class for WorldGuard as a Bukkit plugin.
 */
public class WorldGuardPlugin extends JavaPlugin {

    private static final org.apache.logging.log4j.Logger LOGGER = LogManagerCompat.getLogger();
    private static WorldGuardPlugin inst;
    private static BukkitWorldGuardPlatform platform;
    @SuppressWarnings("deprecation")
    private final CommandsManager<Actor> commands;
    private PlayerMoveListener playerMoveListener;
    private I18nConfig i18nConfig;
    private MessageManager messageManager;
    private MiniMessageHelper miniMessageHelper;
    private com.sk89q.worldguard.bukkit.util.WorldWhitelistChecker worldWhitelistChecker;

    private static final int BSTATS_PLUGIN_ID = 3283;

    /**
     * Construct objects. Actual loading occurs when the plugin is enabled, so
     * this merely instantiates the objects.
     */
    @SuppressWarnings("deprecation")
    public WorldGuardPlugin() {
        inst = this;
        commands = new CommandsManager<Actor>() {
            @Override
            public boolean hasPermission(Actor player, String perm) {
                return player.hasPermission(perm);
            }
        };
    }

    /**
     * Get the current instance of WorldGuard
     * @return WorldGuardPlugin instance
     */
    public static WorldGuardPlugin inst() {
        return inst;
    }

    /**
     * Called on plugin enable.
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onEnable() {
        // Catch bad things being done by naughty plugins that include WorldGuard's classes
        ClassSourceValidator verifier = new ClassSourceValidator(this);
        verifier.reportMismatches(ImmutableList.of(WorldGuard.class, ProtectedRegion.class, Flag.class));

        configureLogger();

        getDataFolder().mkdirs(); // Need to create the plugins/WorldGuard folder

        // Check for WorldGuardExtraFlags plugin
        checkForExtraFlagsPlugin();

        // Initialize i18n and MiniMessage support
        i18nConfig = new I18nConfig(this);
        i18nConfig.load();
        
        messageManager = new MessageManager(this);
        messageManager.loadLanguages();
        messageManager.setLanguage(i18nConfig.getLanguage());
        
        miniMessageHelper = new MiniMessageHelper(this);
        
        getLogger().info("Internationalization loaded: " + i18nConfig.getLanguage());

        // Initialize world whitelist checker
        worldWhitelistChecker = new com.sk89q.worldguard.bukkit.util.WorldWhitelistChecker(this);

        PermissionsResolverManager.initialize(this);

        WorldGuard.getInstance().setPlatform(platform = new BukkitWorldGuardPlatform()); // Initialise WorldGuard
        
        // Register Bukkit-specific extra flags BEFORE setup() loads region data,
        // otherwise give-effects/blocked-effects/play-sounds get registered as
        // UnknownFlag during unmarshal and cause a FlagConflictException here.
        registerBukkitExtraFlags();
        
        WorldGuard.getInstance().setup();
        
        BukkitSessionManager sessionManager = (BukkitSessionManager) platform.getSessionManager();

        // Register ExtraFlags handlers
        registerExtraFlagsHandlers(sessionManager);

        // Set the proper command injector
        @SuppressWarnings("deprecation")
        com.sk89q.minecraft.util.commands.SimpleInjector injector = new com.sk89q.minecraft.util.commands.SimpleInjector(WorldGuard.getInstance());
        commands.setInjector(injector);

        // Register command classes
        @SuppressWarnings("deprecation")
        final CommandsManagerRegistration reg = new CommandsManagerRegistration(this, commands);
        reg.register(ToggleCommands.class);
        reg.register(ProtectionCommands.class);

        if (!platform.getGlobalStateManager().hasCommandBookGodMode()) {
            reg.register(GeneralCommands.class);
        }

        if (this.isFolia()) {
            getServer().getGlobalRegionScheduler().runAtFixedRate(this, new Consumer() {
                @Override
                public void accept(Object ignored) {
                    sessionManager.run();
                }
            }, BukkitSessionManager.RUN_DELAY, BukkitSessionManager.RUN_DELAY);
        } else {
            getServer().getScheduler().scheduleSyncRepeatingTask(this, sessionManager, BukkitSessionManager.RUN_DELAY, BukkitSessionManager.RUN_DELAY);
        }

        // Register events
        getServer().getPluginManager().registerEvents(sessionManager, this);
        (new WorldGuardPlayerListener(this)).registerEvents();
        (new WorldGuardBlockListener(this)).registerEvents();
        (new WorldGuardEntityListener(this)).registerEvents();
        (new WorldGuardWeatherListener(this)).registerEvents();
        (new WorldGuardVehicleListener(this)).registerEvents();
        (new WorldGuardServerListener(this)).registerEvents();
        (new WorldGuardHangingListener(this)).registerEvents();

        // Modules
        (playerMoveListener = new PlayerMoveListener(this)).registerEvents();
        (new BlacklistListener(this)).registerEvents();
        (new ChestProtectionListener(this)).registerEvents();
        (new RegionProtectionListener(this)).registerEvents();
        (new RegionFlagsListener(this)).registerEvents();
        (new WorldRulesListener(this)).registerEvents();
        (new BlockedPotionsListener(this)).registerEvents();
        (new EventAbstractionListener(this)).registerEvents();
        (new PlayerModesListener(this)).registerEvents();
        (new BuildPermissionListener(this)).registerEvents();
        (new InvincibilityListener(this)).registerEvents();
        if ("true".equalsIgnoreCase(System.getProperty("worldguard.debug.listener"))) {
            (new DebuggingListener(this, WorldGuard.logger)).registerEvents();
        }

        platform.getGlobalStateManager().updateCommandBookGodMode();

        if (getServer().getPluginManager().isPluginEnabled("CommandBook")) {
            getServer().getPluginManager().registerEvents(new WorldGuardCommandBookListener(this), this);
        }

        // handle worlds separately to initialize already loaded worlds
        WorldGuardWorldListener worldListener = (new WorldGuardWorldListener(this));
        for (World world : getServer().getWorlds()) {
            worldListener.initWorld(world);
        }
        worldListener.registerEvents();

        if (this.isFolia()) {
            for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                player.getScheduler().run(this, new Consumer() {
                    @Override
                    public void accept(Object ignored) {
                        ProcessPlayerEvent event = new ProcessPlayerEvent(player);
                        Events.fire(event);
                    }
                }, null);
            }
        } else {
            Bukkit.getScheduler().runTask(this, () -> {
                for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                    ProcessPlayerEvent event = new ProcessPlayerEvent(player);
                    Events.fire(event);
                }
            });
        }

        ((SimpleFlagRegistry) WorldGuard.getInstance().getFlagRegistry()).setInitialized(true);

        // Enable metrics
        final Metrics metrics = new Metrics(this, BSTATS_PLUGIN_ID); // bStats plugin id
        if (platform.getGlobalStateManager().extraStats) {
            setupCustomCharts(metrics);
        }
    }

    private void setupCustomCharts(Metrics metrics) {
        metrics.addCustomChart(new SingleLineChart("region_count", () ->
                platform.getRegionContainer().getLoaded().stream().mapToInt(RegionManager::size).sum()));
        metrics.addCustomChart(new SimplePie("region_driver", () -> {
            RegionDriver driver = platform.getGlobalStateManager().selectedRegionStoreDriver;
            return driver instanceof DirectoryYamlDriver ? "yaml" : driver instanceof SQLDriver ? "sql" : "unknown";
        }));
        metrics.addCustomChart(new DrilldownPie("blacklist", () -> {
            int empty = 0;
            Map<String, Integer> blacklistMap = new HashMap<>();
            Map<String, Integer> whitelistMap = new HashMap<>();
            for (BukkitWorldConfiguration worldConfig : platform.getGlobalStateManager().getWorldConfigs()) {
                Blacklist blacklist = worldConfig.getBlacklist();
                if (blacklist != null && !blacklist.isEmpty()) {
                    Map<String, Integer> target = blacklist.isWhitelist() ? whitelistMap : blacklistMap;
                    int floor = ((blacklist.getItemCount() - 1) / 10) * 10;
                    String range = floor >= 100 ? "101+" : (floor + 1) + " - " + (floor + 10);
                    target.merge(range, 1, Integer::sum);
                } else {
                    empty++;
                }
            }
            Map<String, Map<String, Integer>> blacklistCounts = new HashMap<>();
            Map<String, Integer> emptyMap = new HashMap<>();
            emptyMap.put("empty", empty);
            blacklistCounts.put("empty", emptyMap);
            blacklistCounts.put("blacklist", blacklistMap);
            blacklistCounts.put("whitelist", whitelistMap);
            return blacklistCounts;
        }));
        metrics.addCustomChart(new SimplePie("chest_protection", () ->
                "" + platform.getGlobalStateManager().getWorldConfigs().stream().anyMatch(cfg -> cfg.signChestProtection)));
        metrics.addCustomChart(new SimplePie("build_permissions", () ->
                "" + platform.getGlobalStateManager().getWorldConfigs().stream().anyMatch(cfg -> cfg.buildPermissions)));

        metrics.addCustomChart(new SimplePie("custom_flags", () ->
                "" + (WorldGuard.getInstance().getFlagRegistry().size() > Flags.INBUILT_FLAGS.size())));
        metrics.addCustomChart(new SimplePie("custom_handlers", () ->
                "" + (WorldGuard.getInstance().getPlatform().getSessionManager().customHandlersRegistered())));
    }

    @Override
    public void onDisable() {
        WorldGuard.getInstance().disable();
        if (this.isFolia()) {
            this.getServer().getGlobalRegionScheduler().cancelTasks(this);
            this.getServer().getAsyncScheduler().cancelTasks(this);
        } else {
            this.getServer().getScheduler().cancelTasks(this);
        }
        
        // Close MiniMessage helper
        if (miniMessageHelper != null) {
            miniMessageHelper.close();
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        try {
            Actor actor = wrapCommandSender(sender);
            try {
                commands.execute(cmd.getName(), args, actor, actor);
            } catch (Throwable t) {
                Throwable next = t;
                do {
                    try {
                        WorldGuard.getInstance().getExceptionConverter().convert(next);
                    } catch (org.enginehub.piston.exception.CommandException pce) {
                        if (pce.getCause() instanceof com.sk89q.minecraft.util.commands.CommandException) {
                            throw ((com.sk89q.minecraft.util.commands.CommandException) pce.getCause());
                        }
                    }
                    next = next.getCause();
                } while (next != null);

                throw t;
            }
        } catch (com.sk89q.minecraft.util.commands.CommandPermissionsException e) {
            miniMessageHelper.sendMessage(sender, messageManager.getMessage("general.no-permission"));
        } catch (com.sk89q.minecraft.util.commands.MissingNestedCommandException e) {
            String message = messageManager.getMessage("error.invalid-args") + " " + e.getUsage();
            miniMessageHelper.sendMessage(sender, message);
        } catch (com.sk89q.minecraft.util.commands.CommandUsageException e) {
            miniMessageHelper.sendMessage(sender, "<red>" + e.getMessage() + "</red>");
            miniMessageHelper.sendMessage(sender, "<red>" + e.getUsage() + "</red>");
        } catch (com.sk89q.minecraft.util.commands.WrappedCommandException e) {
            miniMessageHelper.sendMessage(sender, messageManager.getMessage("general.error", 
                "error", e.getCause().getMessage()));
        } catch (com.sk89q.minecraft.util.commands.CommandException e) {
            miniMessageHelper.sendMessage(sender, "<red>" + e.getMessage() + "</red>");
        }

        return true;
    }

    /**
     * Check whether a player is in a group.
     * This calls the corresponding method in PermissionsResolverManager
     *
     * @param player The player to check
     * @param group The group
     * @return whether {@code player} is in {@code group}
     */
    public boolean inGroup(OfflinePlayer player, String group) {
        try {
            return PermissionsResolverManager.getInstance().inGroup(player, group);
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    /**
     * Get the groups of a player.
     * This calls the corresponding method in PermissionsResolverManager.
     * @param player The player to check
     * @return The names of each group the playe is in.
     */
    public String[] getGroups(OfflinePlayer player) {
        try {
            return PermissionsResolverManager.getInstance().getGroups(player);
        } catch (Throwable t) {
            t.printStackTrace();
            return new String[0];
        }
    }

    /**
     * Checks permissions.
     *
     * @param sender The sender to check the permission on.
     * @param perm The permission to check the permission on.
     * @return whether {@code sender} has {@code perm}
     */
    public boolean hasPermission(CommandSender sender, String perm) {
        if (sender.isOp()) {
            if (sender instanceof Player) {
                WorldConfiguration config = platform.getGlobalStateManager().get(BukkitAdapter.adapt(((Player) sender).getWorld()));
                if (config != null && config.opPermissions) {
                    return true;
                }
            } else {
                return true;
            }
        }

        // Invoke the permissions resolver
        if (sender instanceof Player) {
            Player player = (Player) sender;
            return PermissionsResolverManager.getInstance().hasPermission(player.getWorld().getName(), player, perm);
        }

        return false;
    }

    /**
     * Checks permissions and throws an exception if permission is not met.
     *
     * @param sender The sender to check the permission on.
     * @param perm The permission to check the permission on.
     * @throws com.sk89q.minecraft.util.commands.CommandPermissionsException if {@code sender} doesn't have {@code perm}
     */
    @SuppressWarnings("deprecation")
    public void checkPermission(CommandSender sender, String perm)
            throws com.sk89q.minecraft.util.commands.CommandPermissionsException {
        if (!hasPermission(sender, perm)) {
            throw new com.sk89q.minecraft.util.commands.CommandPermissionsException();
        }
    }

    /**
     * Gets a copy of the WorldEdit plugin.
     *
     * @return The WorldEditPlugin instance
     * @throws com.sk89q.minecraft.util.commands.CommandException If there is no WorldEditPlugin available
     */
    @SuppressWarnings("deprecation")
    public WorldEditPlugin getWorldEdit() throws com.sk89q.minecraft.util.commands.CommandException {
        Plugin worldEdit = getServer().getPluginManager().getPlugin("WorldEdit");
        if (worldEdit == null) {
            throw new com.sk89q.minecraft.util.commands.CommandException("WorldEdit does not appear to be installed.");
        } else if (!worldEdit.isEnabled()) {
            throw new com.sk89q.minecraft.util.commands.CommandException("WorldEdit does not appear to be enabled.");
        }

        if (worldEdit instanceof WorldEditPlugin) {
            return (WorldEditPlugin) worldEdit;
        } else {
            throw new com.sk89q.minecraft.util.commands.CommandException("WorldEdit detection failed (report error).");
        }
    }

    /**
     * Wrap a player as a LocalPlayer.
     *
     * @param player The player to wrap
     * @return The wrapped player
     */
    public LocalPlayer wrapPlayer(Player player) {
        return new BukkitPlayer(this, player);
    }

    /**
     * Wrap a player as a LocalPlayer.
     *
     * @param player The player to wrap
     * @param silenced True to silence messages
     * @return The wrapped player
     */
    public LocalPlayer wrapPlayer(Player player, boolean silenced) {
        return new BukkitPlayer(this, player, silenced);
    }

    @SuppressWarnings("deprecation")
    public Actor wrapCommandSender(CommandSender sender) {
        if (sender instanceof Player player) {
            if (Entities.isNPC(player)) return null;
            return wrapPlayer(player);
        }

        try {
            return new BukkitCommandSender(getWorldEdit(), sender);
        } catch (CommandException e) {
            e.printStackTrace();
        }
        return null;
    }

    public CommandSender unwrapActor(Actor sender) {
        if (sender instanceof BukkitPlayer) {
            return ((BukkitPlayer) sender).getPlayer();
        } else if (sender instanceof BukkitCommandSender) {
            return Bukkit.getConsoleSender(); // TODO Fix
        } else {
            throw new IllegalArgumentException("Unknown actor type. Please report");
        }
    }

    /**
     * Wrap a player as a LocalPlayer.
     *
     * <p>This implementation is incomplete -- permissions cannot be checked.</p>
     *
     * @param player The player to wrap
     * @return The wrapped player
     */
    public LocalPlayer wrapOfflinePlayer(OfflinePlayer player) {
        return new BukkitOfflinePlayer(this, player);
    }

    /**
     * Internal method. Do not use as API.
     */
    public BukkitConfigurationManager getConfigManager() {
        return platform.getGlobalStateManager();
    }

    /**
     * Return a protection query helper object that can be used by another
     * plugin to test whether WorldGuard permits an action at a particular
     * place.
     *
     * @return an instance
     */
    public ProtectionQuery createProtectionQuery() {
        return new ProtectionQuery();
    }

    /**
     * Configure WorldGuard's loggers.
     */
    private void configureLogger() {
        RecordMessagePrefixer.register(Logger.getLogger("com.sk89q.worldguard"), "[WorldGuard] ");
    }

    /**
     * Create a default configuration file from the .jar.
     *
     * @param actual The destination file
     * @param defaultName The name of the file inside the jar's defaults folder
     */
    public void createDefaultConfiguration(File actual, String defaultName) {

        // Make parent directories
        File parent = actual.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }

        if (actual.exists()) {
            return;
        }

        try (InputStream stream = getResource("defaults/" + defaultName)){
            if (stream == null) throw new FileNotFoundException();
            copyDefaultConfig(stream, actual, defaultName);
        } catch (IOException e) {
            getLogger().severe("Unable to read default configuration: " + defaultName);
        }

    }

    private void copyDefaultConfig(InputStream input, File actual, String name) {
        try (FileOutputStream output = new FileOutputStream(actual)) {
            byte[] buf = new byte[8192];
            int length;
            while ((length = input.read(buf)) > 0) {
                output.write(buf, 0, length);
            }
            getLogger().info("Default configuration file written: " + name);
        } catch (IOException e) {
            getLogger().log(Level.WARNING, "Failed to write default config file", e);
        }
    }

    public PlayerMoveListener getPlayerMoveListener() {
        return playerMoveListener;
    }

    /**
     * Gets the i18n configuration.
     *
     * @return The i18n configuration
     */
    public I18nConfig getI18nConfig() {
        return i18nConfig;
    }

    /**
     * Gets the message manager for internationalization.
     *
     * @return The message manager
     */
    public MessageManager getMessageManager() {
        return messageManager;
    }

    /**
     * Gets the MiniMessage helper.
     *
     * @return The MiniMessage helper
     */
    public MiniMessageHelper getMiniMessageHelper() {
        return miniMessageHelper;
    }

    /**
     * Gets the world whitelist checker.
     *
     * @return The world whitelist checker
     */
    public com.sk89q.worldguard.bukkit.util.WorldWhitelistChecker getWorldWhitelistChecker() {
        return worldWhitelistChecker;
    }

    /**
     * Check for WorldGuardExtraFlags plugin and warn if detected.
     */
    private void checkForExtraFlagsPlugin() {
        Plugin extraFlagsPlugin = getServer().getPluginManager().getPlugin("WorldGuardExtraFlags");
        if (extraFlagsPlugin != null) {
            getLogger().warning("========================================");
            getLogger().warning("检测到 WorldGuardExtraFlags 插件!");
            getLogger().warning("WorldGuard 已内置所有 ExtraFlags 功能");
            getLogger().warning("不需要安装 WorldGuardExtraFlags 插件");
            getLogger().warning("请删除 WorldGuardExtraFlags.jar 文件");
            getLogger().warning("========================================");
            getLogger().warning("Detected WorldGuardExtraFlags plugin!");
            getLogger().warning("WorldGuard now has all ExtraFlags features built-in");
            getLogger().warning("WorldGuardExtraFlags plugin is NOT needed");
            getLogger().warning("Please remove WorldGuardExtraFlags.jar file");
            getLogger().warning("========================================");
            
            // Disable WorldGuardExtraFlags plugin
            try {
                getServer().getPluginManager().disablePlugin(extraFlagsPlugin);
                getLogger().info("已自动禁用 WorldGuardExtraFlags 插件 / Automatically disabled WorldGuardExtraFlags plugin");
            } catch (Exception e) {
                getLogger().warning("无法自动禁用 WorldGuardExtraFlags 插件,请手动删除 / Failed to disable WorldGuardExtraFlags, please remove manually");
            }
        }
    }

    /**
     * Register Bukkit-specific extra flags that require Bukkit API.
     */
    private void registerBukkitExtraFlags() {
        try {
            com.sk89q.worldguard.protection.flags.registry.FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
            registry.register(com.sk89q.worldguard.bukkit.protection.flags.BukkitExtraFlags.BLOCKED_EFFECTS);
            registry.register(com.sk89q.worldguard.bukkit.protection.flags.BukkitExtraFlags.GIVE_EFFECTS);
            registry.register(com.sk89q.worldguard.bukkit.protection.flags.BukkitExtraFlags.PLAY_SOUNDS);
            getLogger().info("Registered Bukkit-specific extra flags");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to register Bukkit-specific extra flags", e);
        }
    }

    /**
     * Register ExtraFlags session handlers.
     */
    private void registerExtraFlagsHandlers(BukkitSessionManager sessionManager) {
        try {
            // Speed handlers
            sessionManager.registerHandler(com.sk89q.worldguard.session.handler.extraflags.WalkSpeedFlagHandler.FACTORY(), null);
            sessionManager.registerHandler(com.sk89q.worldguard.session.handler.extraflags.FlySpeedFlagHandler.FACTORY(), null);
            
            // State handlers
            sessionManager.registerHandler(com.sk89q.worldguard.session.handler.extraflags.FlyFlagHandler.FACTORY(), null);
            sessionManager.registerHandler(com.sk89q.worldguard.session.handler.extraflags.GlideFlagHandler.FACTORY(), null);
            sessionManager.registerHandler(com.sk89q.worldguard.session.handler.extraflags.GodmodeFlagHandler.FACTORY(), null);
            
            // Teleport handlers
            sessionManager.registerHandler(com.sk89q.worldguard.session.handler.extraflags.TeleportOnEntryFlagHandler.FACTORY(this), null);
            sessionManager.registerHandler(com.sk89q.worldguard.session.handler.extraflags.TeleportOnExitFlagHandler.FACTORY(this), null);
            
            // Command handlers
            sessionManager.registerHandler(com.sk89q.worldguard.session.handler.extraflags.CommandOnEntryFlagHandler.FACTORY(), null);
            sessionManager.registerHandler(com.sk89q.worldguard.session.handler.extraflags.CommandOnExitFlagHandler.FACTORY(), null);
            sessionManager.registerHandler(com.sk89q.worldguard.session.handler.extraflags.ConsoleCommandOnEntryFlagHandler.FACTORY(), null);
            sessionManager.registerHandler(com.sk89q.worldguard.session.handler.extraflags.ConsoleCommandOnExitFlagHandler.FACTORY(), null);
            
            // Effect handlers
            sessionManager.registerHandler(com.sk89q.worldguard.session.handler.extraflags.BlockedEffectsFlagHandler.FACTORY(), null);
            sessionManager.registerHandler(com.sk89q.worldguard.session.handler.extraflags.GiveEffectsFlagHandler.FACTORY(), null);
            sessionManager.registerHandler(com.sk89q.worldguard.session.handler.extraflags.PlaySoundsFlagHandler.FACTORY(this), null);
            
            getLogger().info("Registered ExtraFlags session handlers");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to register ExtraFlags session handlers", e);
        }
    }

    private final LazyReference<Boolean> folia = LazyReference.from(() -> {
        try {
            // Folia is Paper-based, so this is a good first check.
            if (PaperLib.isPaper()) {
                return ServerBuildInfo.buildInfo().isBrandCompatible(net.kyori.adventure.key.Key.key("papermc", "folia"));
            }
        } catch (Throwable t) {
            // Ignore, this likely means an outdated version.
            LOGGER.warn("Failed to check if server is running Folia", t);
        }

        return false;
    });

    public boolean isFolia() {
        return folia.getValue();
    }

}
