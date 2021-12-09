package me.stumper66.spawnercontrol;

import org.jetbrains.annotations.NotNull;

public class SpawnerChunkUpdate implements SpawnerUpdateInterface {
    public SpawnerChunkUpdate(final long chunkId, final @NotNull UpdateOperation operation){
        this.chunkId = chunkId;
        this.operation = operation;
    }

    public long chunkId;
    private final UpdateOperation operation;

    public @NotNull UpdateOperation getOperation() {
        return operation;
    }
}
