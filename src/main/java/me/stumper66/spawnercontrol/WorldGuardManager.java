package me.stumper66.spawnercontrol;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import me.stumper66.spawnercontrol.processing.BasicLocation;
import org.bukkit.Location;
import org.bukkit.block.CreatureSpawner;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class WorldGuardManager {
    public static @NotNull List<String> getWorldGuardRegionsForLocation(@NotNull final Location location) {
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

    public static void updateWorlguardOptionsForTrackedSpawners(final @NotNull SpawnerControl main, final @NotNull Map<BasicLocation, CreatureSpawner> allSpawners,
                                                                final @NotNull Map<BasicLocation, SpawnerInfo> activeSpawners){
        final boolean hasDefinedRegions = main.wgRegionOptions != null && !main.wgRegionOptions.isEmpty();

        for (final BasicLocation location : allSpawners.keySet()){
            final CreatureSpawner cs = allSpawners.get(location);
            if (!cs.getLocation().getChunk().isLoaded()) continue;

            SpawnerInfo sInfo = activeSpawners.get(location);

            if (!hasDefinedRegions){
                if (sInfo != null) {
                    final String customName = sInfo.getSpawnerCustomName();
                    if (customName != null && main.namedSpawnerOptions != null && main.namedSpawnerOptions.containsKey(sInfo.getSpawnerCustomName()))
                        sInfo.options = main.namedSpawnerOptions.get(customName);
                    else
                        sInfo.options = main.spawnerOptions;
                }
                continue;
            }

            if (sInfo == null)
                sInfo = new SpawnerInfo(cs, main.spawnerOptions);

            if (updateWorlguardOptionsForSpawner(main, sInfo))
                main.spawnerProcessor.reevaluateSpawner(sInfo);
        }
    }

    public static boolean updateWorlguardOptionsForSpawner(final @NotNull SpawnerControl main, final @NotNull SpawnerInfo spawnerInfo){
        final SpawnerOptions previouslyUsedOptions = spawnerInfo.options;
        final String previouslyMatchedRegion = spawnerInfo.matchedWGRegion;

        spawnerInfo.options = spawnerInfo.namedSpawnerOptions != null ?
                spawnerInfo.namedSpawnerOptions : main.spawnerOptions;
        spawnerInfo.matchedWGRegion = null;

        if (main.wgRegionOptions != null) {
            for (final String foundRegion : getWorldGuardRegionsForLocation(spawnerInfo.getCs().getLocation())) {
                if (!main.wgRegionOptions.containsKey(foundRegion)) continue;

                spawnerInfo.options = main.wgRegionOptions.get(foundRegion);
                spawnerInfo.matchedWGRegion = foundRegion;
                break;
            }
        }

        return previouslyUsedOptions != spawnerInfo.options ||
                !Objects.equals(previouslyMatchedRegion, spawnerInfo.matchedWGRegion);
    }
}
