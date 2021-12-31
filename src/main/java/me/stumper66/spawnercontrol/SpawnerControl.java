package me.stumper66.spawnercontrol;

import me.stumper66.spawnercontrol.command.CommandProcessor;
import me.stumper66.spawnercontrol.listener.BlockBreakListener;
import me.stumper66.spawnercontrol.listener.BlockPlaceListener;
import me.stumper66.spawnercontrol.listener.ChunkLoadListener;
import me.stumper66.spawnercontrol.listener.PlayerInteractEventListener;
import me.stumper66.spawnercontrol.processing.SpawnerProcessor;
import me.stumper66.spawnercontrol.processing.UpdateProcessor;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;

public class SpawnerControl extends JavaPlugin {
    public YamlConfiguration settings;
    public boolean isEnabled;
    public SpawnerProcessor spawnerProcessor;
    public UpdateProcessor updateProcessor;
    public Map<String, SpawnerOptions> wgRegionOptions;
    public Map<String, SpawnerOptions> namedSpawnerOptions;
    public SpawnerOptions spawnerOptions;
    public DebugInfo debugInfo;
    private BukkitRunnable mainTask;

    @Override
    public void onEnable() {
        spawnerProcessor = new SpawnerProcessor(this);
        updateProcessor = new UpdateProcessor(spawnerProcessor);
        debugInfo = new DebugInfo();

        registerCommands();
        loadConfig(false);
        registerListeners();

        startRunnables();

        Utils.logger.info("Done loading");
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
        final PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new BlockPlaceListener(this), this);
        pm.registerEvents(new BlockBreakListener(this), this);
        pm.registerEvents(new ChunkLoadListener(this), this);
        pm.registerEvents(new PlayerInteractEventListener(this), this);
    }

    private void startRunnables() {
        this.mainTask = new BukkitRunnable() {
            @Override
            public void run() {
                spawnerProcessor.processSpawners();
            }
        };

        this.mainTask.runTaskTimerAsynchronously(this, 100, 40);
    }

    public void loadConfig(boolean isReload) {
        this.settings = FileLoader.loadConfig(this);
        this.isEnabled = settings.getBoolean("enable-spawner-control", true);

        FileLoader.parseConfigFile(this, settings);

        if (isReload)
            updateProcessor.configReloaded();
    }

    public static boolean isWorldGuardInstalled() {
        return Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
    }
}
