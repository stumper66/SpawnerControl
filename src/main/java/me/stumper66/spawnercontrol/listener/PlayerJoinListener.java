package me.stumper66.spawnercontrol.listener;

import me.stumper66.spawnercontrol.SpawnerControl;
import me.stumper66.spawnercontrol.Utils;
import me.stumper66.spawnercontrol.processing.UpdateOperation;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class PlayerJoinListener implements Listener {
    public PlayerJoinListener(final @NotNull SpawnerControl main){
        this.main = main;
    }

    private final SpawnerControl main;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onPlayerJoin(final @NotNull PlayerJoinEvent event){
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                runScanNear(event.getPlayer());
            }
        };

        runnable.runTaskLater(main, 40L);
    }

    private void runScanNear(final @NotNull Player player){
        if (!player.isOnline()) return;
        final Location location = player.getLocation();
        if (location.getWorld() == null) return;

        int count = 0;

        for (final Chunk chunk : Utils.getChunksAroundPlayer(player)){
            for (final BlockState state : chunk.getTileEntities()) {
                if (!(state instanceof CreatureSpawner)) continue;

                final CreatureSpawner cs = (CreatureSpawner) state;
                main.spawnerProcessor.updateSpawner(cs, UpdateOperation.ADD);
                count++;
            }
        }

        if (main.debugInfo.debugIsEnabled) {
            Utils.logger.info(String.format("Player %s join, discovered %s spawner(s)", player.getName(), count));
        }
    }
}
