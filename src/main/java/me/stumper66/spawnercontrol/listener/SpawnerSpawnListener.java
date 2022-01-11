package me.stumper66.spawnercontrol.listener;

import me.stumper66.spawnercontrol.SpawnerControl;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.jetbrains.annotations.NotNull;

public class SpawnerSpawnListener implements Listener {
    public SpawnerSpawnListener(final @NotNull SpawnerControl main){
        this.main = main;
    }

    private final SpawnerControl main;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    void onSpawnerEvent(final @NotNull SpawnerSpawnEvent event){
        // this will cancel natural spawners from an active spawner
        // we only want it spawning mobs created from this plugin
        if (event.getEntity().getUniqueId() != main.spawnerProcessor.currentSpawningEntityId &&
            main.spawnerProcessor.isSpawnerActive(event.getSpawner())){
            event.setCancelled(true);
        }
    }
}
