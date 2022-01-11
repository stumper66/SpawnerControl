package me.stumper66.spawnercontrol.processing;

import org.bukkit.ChunkSnapshot;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public class ChunkEnumeratorItem implements SpawnerUpdateInterface {
    public ChunkEnumeratorItem(final @NotNull ChunkSnapshot chunkSnapshot, final @NotNull World world){
        this.operation = UpdateOperation.CHUNK_ENUMERATION;
        this.chunkSnapshot = chunkSnapshot;
        this.world = world;
    }

    private final @NotNull UpdateOperation operation;
    public final @NotNull World world;
    public final @NotNull ChunkSnapshot chunkSnapshot;

    public @NotNull UpdateOperation getOperation(){
        return this.operation;
    }
}
