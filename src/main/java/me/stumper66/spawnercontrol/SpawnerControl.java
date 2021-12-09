package me.stumper66.spawnercontrol;

import me.stumper66.spawnercontrol.listener.BlockBreakListener;
import me.stumper66.spawnercontrol.listener.BlockPlaceListener;
import me.stumper66.spawnercontrol.listener.ChunkLoadListener;
import me.stumper66.spawnercontrol.listener.PlayerInteractEventListener;
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
    public Map<String, SpawnerOptions> wgRegionOptions;
    public SpawnerOptions spawnerOptions;
    private BukkitRunnable mainTask;

    @Override
    public void onEnable() {
        spawnerProcessor = new SpawnerProcessor(this);

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
        final PluginCommand scCmd = getCommand("slimespawners");
        if (scCmd == null)
            Utils.logger.error("Command &b/slimespawners&7 is unavailable, is it not registered in plugin.yml?");
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

    void loadConfig(boolean isReload) {
        this.settings = FileLoader.loadConfig(this);
        this.isEnabled = settings.getBoolean("enable-spawner-control", true);

        FileLoader.parseConfigFile(this, settings);

        if (isReload)
            spawnerProcessor.configReloaded();
    }

    static boolean isWorldGuardInstalled() {
        return Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
    }
}
