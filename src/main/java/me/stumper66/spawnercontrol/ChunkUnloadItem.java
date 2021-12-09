package me.stumper66.spawnercontrol;

import org.bukkit.Chunk;
import org.jetbrains.annotations.NotNull;

public class ChunkUnloadItem implements SpawnerUpdateInterface {
    public ChunkUnloadItem(final @NotNull Chunk chunk, final @NotNull UpdateOperation operation){
        this.chunk = chunk;
        this.operation = operation;
    }

    private final @NotNull UpdateOperation operation;
    public final @NotNull Chunk chunk;

    public @NotNull UpdateOperation getOperation() {
        return this.operation;
    }
}
