package me.stumper66.spawnercontrol.listener;

import me.stumper66.spawnercontrol.DebugType;
import me.stumper66.spawnercontrol.SpawnerControl;
import me.stumper66.spawnercontrol.Utils;
import me.stumper66.spawnercontrol.processing.UpdateOperation;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.CreatureSpawner;
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

        if (main.spawnerProcessor.hasAlreadyProcessedChunk(event.getChunk().getChunkKey())){
            main.spawnerProcessor.updateChunk(event.getChunk().getChunkKey(), UpdateOperation.CHUNK_REFRESH);
            return;
        }

        final World world = event.getWorld();
        final int yMin = world.getMinHeight();
        final int yMax = world.getMaxHeight();
        final int cx = event.getChunk().getX() << 4;
        final int cz = event.getChunk().getZ() << 4;

        for (int x = cx; x < cx + 16; x++) {
            for (int z = cz; z < cz + 16; z++) {
                for (int y = yMin; y <= yMax; y++) {
                    if (world.getBlockAt(x, y, z).getType() != Material.SPAWNER) continue;

                    final CreatureSpawner cs = (CreatureSpawner) world.getBlockAt(x, y, z).getState();
                    if (main.debugInfo.doesSpawnerMeetDebugCriteria(DebugType.CHUNK_LOAD))
                        Utils.logger.info("ChunkLoadEvent: found spawner " + Utils.showSpawnerLocation(cs));

                    main.spawnerProcessor.updateSpawner(cs, UpdateOperation.ADD);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onChunkUnload(final @NotNull ChunkUnloadEvent event){
        if (!main.spawnerOptions.allowedWorlds.isEnabledInList(event.getWorld().getName())) return;

        main.spawnerProcessor.updateChunk(event.getChunk().getChunkKey(), UpdateOperation.CHUNK_UNLOADED);
    }
}
