package me.stumper66.spawnercontrol;

import org.bukkit.entity.EntityType;

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
}
