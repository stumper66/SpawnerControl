package me.stumper66.spawnercontrol;

import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

public class SpawnerOptions {
    public SpawnerOptions(){
        this.allowedWorlds = new CachedModalList<>();
        this.allowedEntityTypes = new CachedModalList<>();
        // these are the defaults
        this.playerRequiredRange = 50;
        this.minSpawnDelay = 200;
        this.maxSpawnDelay = 800;
        this.spawnCount = 1;
        this.spawnRange = 6;
        this.maxNearbyEntities = 4;
        this.delay = 20;
    }

    public int playerRequiredRange;
    public int minSpawnDelay;
    public int maxSpawnDelay;
    public int maxNearbyEntities;
    public int spawnCount;
    public int spawnRange;
    public int delay;
    public Integer slimeSizeMin;
    public Integer slimeSizeMax;
    public boolean allowAirSpawning;
    public String customNameMatch;
    public @NotNull CachedModalList<String> allowedWorlds;
    public @NotNull CachedModalList<EntityType> allowedEntityTypes;

    public String toString(){
        final StringBuilder sb = new StringBuilder();

        try {
            for (final Field f : this.getClass().getDeclaredFields()) {
                if (f.get(this) == null) continue;
                if (sb.length() > 0) sb.append(", ");
                sb.append(f.getName());
                sb.append(": ");
                sb.append(f.get(this));
            }
        } catch (IllegalAccessException e){
            e.printStackTrace();
            return super.toString();
        }

        return sb.toString();
    }
}
