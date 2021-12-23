package me.stumper66.spawnercontrol.processing;

import org.bukkit.block.CreatureSpawner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SpawnerUpdateItem implements SpawnerUpdateInterface {
    public SpawnerUpdateItem(final @NotNull CreatureSpawner cs, final @NotNull UpdateOperation operation){
        this.cs = cs;
        this.basicLocation = new BasicLocation(cs.getLocation());
        this.operation = operation;
    }

    public final @NotNull CreatureSpawner cs;
    public final @NotNull BasicLocation basicLocation;
    public @Nullable String oldName;
    public @Nullable String newName;
    private final @NotNull UpdateOperation operation;

    public @NotNull UpdateOperation getOperation(){
        return this.operation;
    }
}
