package me.stumper66.spawnercontrol.listener;

import me.stumper66.spawnercontrol.SpawnerControl;
import me.stumper66.spawnercontrol.processing.UpdateOperation;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.jetbrains.annotations.NotNull;

public class ChunkLoadListener implements Listener {
    public ChunkLoadListener(final SpawnerControl main){
        this.main = main;
    }

    private final SpawnerControl main;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onChunkLoad(final @NotNull ChunkLoadEvent event){
        if (!main.spawnerOptions.allowedWorlds.isEnabledInList(event.getWorld().getName())) return;

        if (main.spawnerProcessor.hasAlreadyProcessedChunk(event.getChunk().getChunkKey()))
            main.spawnerProcessor.updateChunk(event.getChunk().getChunkKey(), UpdateOperation.CHUNK_REFRESH);
        else
            main.spawnerProcessor.enumerateChunk(event.getChunk().getChunkSnapshot(true, false, false), event.getWorld());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onChunkUnload(final @NotNull ChunkUnloadEvent event){
        if (!main.spawnerOptions.allowedWorlds.isEnabledInList(event.getWorld().getName())) return;

        main.spawnerProcessor.updateChunk(event.getChunk().getChunkKey(), UpdateOperation.CHUNK_UNLOADED);
    }
}
