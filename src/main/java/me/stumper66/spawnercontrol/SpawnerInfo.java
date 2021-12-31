package me.stumper66.spawnercontrol;

import me.stumper66.spawnercontrol.processing.BasicLocation;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;

public class SpawnerInfo {
    public SpawnerInfo(final @NotNull CreatureSpawner cs, final @NotNull SpawnerOptions options){
        this.cs = cs;
        this.basicLocation = new BasicLocation(cs.getLocation());
        this.isChunkLoaded = true;
        this.options = options;
        resetTimeLeft(options);
    }

    public int delayTimeLeft;
    public @Nullable SpawnerOptions options;
    public @Nullable String matchedWGRegion;
    public boolean isChunkLoaded;
    private @NotNull CreatureSpawner cs;
    private @NotNull BasicLocation basicLocation;
    private String spawnerCustomName;
    private boolean hasCheckedForCustomName;

    @NotNull
    public CreatureSpawner getCs(){
        return this.cs;
    }

    public void setCs(final @NotNull CreatureSpawner cs){
        this.cs = cs;
        this.basicLocation = new BasicLocation(cs.getLocation());
    }

    @NotNull
    public BasicLocation getBasicLocation(){
        return this.basicLocation;
    }

    public void resetTimeLeft(final @NotNull SpawnerOptions options){
        this.delayTimeLeft = options.delay;
        this.delayTimeLeft += options.maxSpawnDelay <= options.minSpawnDelay ?
                options.minSpawnDelay :
                options.maxSpawnDelay + ThreadLocalRandom.current().nextInt(Math.max(options.maxSpawnDelay - options.minSpawnDelay, 1));
    }

    @Nullable
    public String getSpawnerCustomName(final @NotNull SpawnerControl spawnerControl){
        if (!hasCheckedForCustomName){
            hasCheckedForCustomName = true;
            if (cs.getPersistentDataContainer().has(spawnerControl.spawnerProcessor.spawnerCustomNameKey, PersistentDataType.STRING))
                this.spawnerCustomName = cs.getPersistentDataContainer().get(spawnerControl.spawnerProcessor.spawnerCustomNameKey, PersistentDataType.STRING);
        }

        return this.spawnerCustomName;
    }

    public void setSpawnerCustomName(final @Nullable String customName, final @NotNull SpawnerControl spawnerControl){
        this.spawnerCustomName = customName;
        this.hasCheckedForCustomName = true;

        if (customName == null || customName.isEmpty()) {
            if (cs.getPersistentDataContainer().has(spawnerControl.spawnerProcessor.spawnerCustomNameKey, PersistentDataType.STRING))
                cs.getPersistentDataContainer().remove(spawnerControl.spawnerProcessor.spawnerCustomNameKey);

            Utils.logger.info("setSpawnerCustomName name was nulll or empty, " + Utils.showSpawnerLocation(cs));
        }
        else {
            Utils.logger.info("setting name to " + customName + ", " + Utils.showSpawnerLocation(cs));
            cs.getPersistentDataContainer().set(spawnerControl.spawnerProcessor.spawnerCustomNameKey, PersistentDataType.STRING, customName);
        }

        cs.update();
    }

    public void clearCache(){
        this.spawnerCustomName = null;
        this.hasCheckedForCustomName = false;
    }

    public String toString() {
        return "Time left: " + delayTimeLeft;
    }
}
