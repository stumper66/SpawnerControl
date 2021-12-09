package me.stumper66.spawnercontrol;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class WorldGuardManager {
    @NotNull
    public static List<String> getWorldGuardRegionsForLocation(@NotNull final Location location) {
        final List<String> wg_Regions = new LinkedList<>();

        if (location.getWorld() == null) return wg_Regions;
        final com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(location.getWorld());

        final BlockVector3 position = BlockVector3.at(
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );

        final RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        final RegionManager regions = container.get(world);
        if (regions == null) return wg_Regions;

        final ApplicableRegionSet set = regions.getApplicableRegions(position);
        for (final ProtectedRegion region : set)
            wg_Regions.add(region.getId());

        return wg_Regions;
    }

    public static void updateWorlguardOptionsForTrackedSpawners(final @NotNull SpawnerControl main, final @NotNull Map<Location, SpawnerInfo> spawnerTracking){
        final boolean hasDefinedRegions = main.wgRegionOptions != null && !main.wgRegionOptions.isEmpty();

        for (final Location location : spawnerTracking.keySet()){
            final SpawnerInfo sInfo = spawnerTracking.get(location);

            if (!hasDefinedRegions){
                sInfo.options = main.spawnerOptions;
                continue;
            }

            updateWorlguardOptionsForSpawner(main, sInfo);
        }
    }

    public static void updateWorlguardOptionsForSpawner(final @NotNull SpawnerControl main, final @NotNull SpawnerInfo spawnerInfo){
        spawnerInfo.matchedWGRegion = null;

        for (final String foundRegion : getWorldGuardRegionsForLocation(spawnerInfo.cs.getLocation())){
            if (!main.wgRegionOptions.containsKey(foundRegion)) continue;

            spawnerInfo.options = main.wgRegionOptions.get(foundRegion);
            spawnerInfo.matchedWGRegion = foundRegion;
            return;
        }
    }
}
