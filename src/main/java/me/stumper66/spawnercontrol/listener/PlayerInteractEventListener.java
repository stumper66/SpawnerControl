package me.stumper66.spawnercontrol.listener;

import me.stumper66.spawnercontrol.SpawnerControl;
import me.stumper66.spawnercontrol.processing.UpdateOperation;
import org.bukkit.Material;
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

        final CreatureSpawner cs = (CreatureSpawner) event.getClickedBlock().getState();
        main.spawnerProcessor.updateSpawner(cs, UpdateOperation.UPDATE);
    }
}
