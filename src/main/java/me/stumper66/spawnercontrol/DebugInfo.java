package me.stumper66.spawnercontrol;

import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class DebugInfo {
    public DebugInfo(){
        this.enabledDebugTypes = new HashSet<>();
        this.enabledEntityTypes = new HashSet<>();
        this.enabledSpawnerNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        this.enabledRegionNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        this.allEntityTypesEnabled = true;
    }

    public boolean debugIsEnabled;
    public final Set<DebugTypes> enabledDebugTypes;
    public final Set<EntityType> enabledEntityTypes;
    public final Set<String> enabledSpawnerNames;
    public final Set<String> enabledRegionNames;
    public boolean allEntityTypesEnabled;

    public boolean doesSpawnerMeetDebugCriteria(final @NotNull DebugTypes debugType){
        if (!this.debugIsEnabled) return false;

        return this.enabledDebugTypes.isEmpty() || this.enabledDebugTypes.contains(debugType);
    }

    public boolean doesSpawnerMeetDebugCriteria(final @NotNull SpawnerControl sc, final @NotNull DebugTypes debugType, final @NotNull SpawnerInfo info){
        if (!this.debugIsEnabled) return false;

        if (!this.enabledDebugTypes.isEmpty() && !this.enabledDebugTypes.contains(debugType)) return false;

        if (!this.allEntityTypesEnabled || !this.enabledEntityTypes.contains(info.getCs().getSpawnedType())) return false;

        String spawnerName = info.getSpawnerCustomName(sc);
        if (spawnerName == null) spawnerName = "(none)";
        if (!this.enabledSpawnerNames.isEmpty() && !this.enabledSpawnerNames.contains(spawnerName)) return false;

        String regionName = info.matchedWGRegion;
        if (regionName == null) regionName = "(none)";
        return this.enabledRegionNames.isEmpty() || this.enabledRegionNames.contains(regionName);
    }
}
