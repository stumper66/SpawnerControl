package me.stumper66.spawnercontrol;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

public class SpigotCompat {
    public static @NotNull Entity spawnEntity(final @NotNull Location location, final EntityType entityType) {
        return location.getWorld().spawnEntity(location, entityType);
    }
}
