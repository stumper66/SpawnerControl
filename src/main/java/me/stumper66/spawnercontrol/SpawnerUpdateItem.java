package me.stumper66.spawnercontrol;

import org.bukkit.block.CreatureSpawner;
import org.jetbrains.annotations.NotNull;

public class SpawnerUpdateItem implements SpawnerUpdateInterface {
    public SpawnerUpdateItem(final @NotNull CreatureSpawner cs, final @NotNull UpdateOperation operation){
        this.cs = cs;
        this.operation = operation;
    }

    public final @NotNull CreatureSpawner cs;
    private final @NotNull UpdateOperation operation;

    public @NotNull UpdateOperation getOperation(){
        return this.operation;
    }
}
