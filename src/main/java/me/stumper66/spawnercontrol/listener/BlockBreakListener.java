package me.stumper66.spawnercontrol.listener;

import me.stumper66.spawnercontrol.SpawnerControl;
import me.stumper66.spawnercontrol.UpdateOperation;
import org.bukkit.Material;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.NotNull;

public class BlockBreakListener implements Listener {
    public BlockBreakListener(@NotNull final SpawnerControl main){
        this.main = main;
    }
    private final SpawnerControl main;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onBlockBreakEvent(final @NotNull BlockBreakEvent event){
        if (event.getBlock().getType() != Material.SPAWNER) return;

        if (!main.spawnerOptions.allowedWorlds.isEnabledInList(event.getPlayer().getWorld().getName())) return;

        final CreatureSpawner cs = (CreatureSpawner) event.getBlock().getState();
        main.spawnerProcessor.updateSpawner(cs, UpdateOperation.REMOVE);
    }
}
