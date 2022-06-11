package me.stumper66.spawnercontrol;

import me.lokka30.microlib.messaging.MessageUtils;
import me.lokka30.microlib.other.VersionUtils;
import me.stumper66.spawnercontrol.command.CommandProcessor;
import me.stumper66.spawnercontrol.listener.BlockBreakListener;
import me.stumper66.spawnercontrol.listener.BlockPlaceListener;
import me.stumper66.spawnercontrol.listener.ChunkLoadListener;
import me.stumper66.spawnercontrol.listener.PlayerInteractEventListener;
import me.stumper66.spawnercontrol.listener.PlayerJoinListener;
import me.stumper66.spawnercontrol.listener.SpawnerSpawnListener;
import me.stumper66.spawnercontrol.processing.SpawnerProcessor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;

public class SpawnerControl extends JavaPlugin {

    public YamlConfiguration settings;
    public boolean isEnabled;
    public boolean supportsVariableMinHeight;
    public SpawnerProcessor spawnerProcessor;
    public Map<String, SpawnerOptions> wgRegionOptions;
    public Map<String, SpawnerOptions> namedSpawnerOptions;
    public SpawnerOptions spawnerOptions;
    public DebugInfo debugInfo;
    private BukkitRunnable mainTask;

    @Override
    public void onEnable() {
        spawnerProcessor = new SpawnerProcessor(this);
        debugInfo = new DebugInfo();
        this.supportsVariableMinHeight = VersionUtils.isOneSixteen();

        registerCommands();
        loadConfig(false);
        registerListeners();

        startRunnables();

        Utils.logger.info("Loading complete.");
    }

    @Override
    public void onDisable() {
        if (this.mainTask != null && !mainTask.isCancelled())
            this.mainTask.cancel();
    }

    private void registerCommands() {
        CommandProcessor cmd = new CommandProcessor(this);
        final PluginCommand scCmd = getCommand("spawnercontrol");
        if (scCmd == null)
            Utils.logger.error("Command &b/spawnercontrol&7 is unavailable, is it not registered in plugin.yml?");
        else
            scCmd.setExecutor(cmd);
    }

    private void registerListeners() {
        Arrays.asList(
                new BlockBreakListener(this),
                new BlockPlaceListener(this),
                new ChunkLoadListener(this),
                new PlayerInteractEventListener(this),
                new SpawnerSpawnListener(this),
                new PlayerJoinListener((this))
        ).forEach(listener -> Bukkit.getPluginManager().registerEvents(listener, this));
    }

    private void startRunnables() {
        this.mainTask = new BukkitRunnable() {
            @Override
            public void run() {
                spawnerProcessor.startProcessing();
            }
        };

        this.mainTask.runTaskTimerAsynchronously(this, 100, 40);
    }

    public void loadConfig(boolean isReload) {
        this.settings = FileLoader.loadConfig(this);
        this.isEnabled = settings.getBoolean("enable-spawner-control", true);

        FileLoader.parseConfigFile(this, settings);

        if (isReload)
            spawnerProcessor.configReloaded();
    }

    public static boolean isWorldGuardInstalled() {
        return Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
    }

    public void sendPrefixedMessage(final @NotNull CommandSender sender, final String msg) {
        sender.sendMessage(MessageUtils.colorizeAll("&b&lSpawnerControl: &7" + msg));
    }
}
