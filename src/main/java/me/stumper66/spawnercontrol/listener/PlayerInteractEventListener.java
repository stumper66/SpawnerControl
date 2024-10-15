package me.stumper66.spawnercontrol.listener;

import me.stumper66.spawnercontrol.SpawnerControl;
import me.stumper66.spawnercontrol.Utils;
import me.stumper66.spawnercontrol.processing.UpdateOperation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerInteractEventListener implements Listener {
    public PlayerInteractEventListener (final SpawnerControl main){
        this.main = main;
    }

    private final SpawnerControl main;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onPlayerInteractEvent(final @NotNull PlayerInteractEvent event){
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getItem() == null) return;
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.SPAWNER) return;

        final Runnable runnable = () -> checkCreatureSpawnerDelayed(event.getClickedBlock().getLocation());
        Bukkit.getScheduler().runTaskLater(main, runnable, 100L);
    }

    private void checkCreatureSpawnerDelayed(final @NotNull Location location){
        final Block block = location.getWorld().getBlockAt(location);
        if (block.getType() != Material.SPAWNER){
            //Utils.logger.info("block was not a spawner");
            return;
        }

        final CreatureSpawner cs = (CreatureSpawner) block.getState();

        if (main.debugInfo.debugIsEnabled)
            Utils.logger.info("Player updated spawner: " + Utils.showSpawnerLocation(cs));
        main.spawnerProcessor.updateSpawner(cs, UpdateOperation.UPDATE);
    }
}
