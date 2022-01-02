package me.stumper66.spawnercontrol;

import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

public class SpawnerOptions {
    public SpawnerOptions(){
        this.allowedWorlds = new CachedModalList<>();
        this.allowedEntityTypes = new CachedModalList<>();
        // these are the defaults
        this.playerRequiredRange = 6.0;
        this.minSpawnDelay = 200;
        this.maxSpawnDelay = 800;
        this.spawnCount_Min = 1;
        this.spawnCount_Max = 2;
        this.spawnRange = 6;
        this.maxNearbyEntities = 4;
        this.delay = 20;
        this.allowedLightLevel_Min = 0;
        this.allowedLightLevel_Max = 15;
        this.allowedSkyLightLevel_Min = 0;
        this.allowedSkyLightLevel_Max = 15;
        this.allowedBlockLightLevel_Min = 0;
        this.allowedBlockLightLevel_Max = 15;
    }

    public int minSpawnDelay;
    public int maxSpawnDelay;
    public int maxNearbyEntities;
    public int spawnCount_Min;
    public int spawnCount_Max;
    public int spawnRange;
    public int delay;
    public int allowedLightLevel_Min;
    public int allowedLightLevel_Max;
    public int allowedSkyLightLevel_Min;
    public int allowedSkyLightLevel_Max;
    public int allowedBlockLightLevel_Min;
    public int allowedBlockLightLevel_Max;
    public int immediateSpawnResetPeriod;
    public Integer slimeSizeMin;
    public Integer slimeSizeMax;
    public double playerRequiredRange;
    public boolean allowAirSpawning;
    public boolean doImmediateSpawn;
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
