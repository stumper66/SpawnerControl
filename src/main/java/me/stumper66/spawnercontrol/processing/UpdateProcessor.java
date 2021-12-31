package me.stumper66.spawnercontrol.processing;

import me.stumper66.spawnercontrol.SpawnerControl;
import me.stumper66.spawnercontrol.SpawnerInfo;
import me.stumper66.spawnercontrol.Utils;
import me.stumper66.spawnercontrol.WorldGuardManager;
import org.bukkit.block.CreatureSpawner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class UpdateProcessor {
    public UpdateProcessor(final @NotNull SpawnerProcessor sp){
        this.sp = sp;
        this.main = sp.main;
        this.spawnerUpdateQueue = new LinkedList<>();
        this.allSpawners = new HashMap<>();
        this.chunkMappings = new HashMap<>();
        this.worldMappings = new HashMap<>();
    }

    private final SpawnerControl main;
    private final @NotNull Map<BasicLocation, CreatureSpawner> allSpawners;
    private final @NotNull Map<Long, Set<BasicLocation>> chunkMappings;
    final @NotNull Map<String, Set<BasicLocation>> worldMappings;
    private final SpawnerProcessor sp;
    private final @NotNull Queue<SpawnerUpdateInterface> spawnerUpdateQueue;
    private boolean recheckCriteria;
    private final static Object chunkLock = new Object();
    private final static Object queueLock = new Object();

    void processUpdates() {
        if (recheckCriteria) recheckSpawnerCriteria();

        while (true){
            SpawnerUpdateInterface itemInterface;

            synchronized (queueLock) {
                if (this.spawnerUpdateQueue.isEmpty()) return;
                itemInterface = this.spawnerUpdateQueue.remove();
            }

            if (itemInterface == null) continue;

            switch (itemInterface.getOperation()){
                case REMOVE:
                    removeSpawner((SpawnerUpdateItem) itemInterface);
                    break;
                case CHUNK_UNLOADED:
                case CHUNK_REFRESH:
                    processChunk((SpawnerChunkUpdate) itemInterface);
                    break;
                case CUSTOM_NAME_CHANGE:
                    processSpawnerRename((SpawnerUpdateItem) itemInterface);
                    break;
                default: // update and add
                    processSpawnerOrChunkAdd((SpawnerUpdateItem) itemInterface);
                    break;
            }
        }
    }


    private void processSpawnerRename(final @NotNull SpawnerUpdateItem item){
        final SpawnerInfo info = sp.activeSpawners.get(item.basicLocation);
        if (info != null){
            info.clearCache();
            info.setCs(item.cs);
            Utils.logger.info("reevaluating tracked spawner");
        }
        else
            Utils.logger.info("evaluating UNtracked spawner");

        evaluateTrackingCriteriaForSpawner(item.cs, item.basicLocation);
    }

    private void removeSpawner(final @NotNull SpawnerUpdateItem item){
        sp.activeSpawners.remove(item.basicLocation);
        if (this.chunkMappings.containsKey(item.cs.getLocation().getChunk().getChunkKey()))
            this.chunkMappings.get(item.cs.getLocation().getChunk().getChunkKey()).remove(item.basicLocation);
        if (this.worldMappings.containsKey(item.cs.getLocation().getWorld().getName()))
            this.worldMappings.get(item.cs.getLocation().getWorld().getName()).remove(item.basicLocation);
        this.allSpawners.remove(item.basicLocation);
    }

    private void processChunk(final @NotNull SpawnerChunkUpdate spawnerChunkUpdate){
        Set<BasicLocation> locations;

        synchronized (chunkLock) {
            if (!this.chunkMappings.containsKey(spawnerChunkUpdate.chunkId)) return;
            locations = this.chunkMappings.get(spawnerChunkUpdate.chunkId);
        }

        if (locations == null) return;

        for (final BasicLocation location : locations){
            if (spawnerChunkUpdate.getOperation() == UpdateOperation.CHUNK_REFRESH){
                final CreatureSpawner cs = this.allSpawners.get(location);
                if (cs == null) continue;

                evaluateTrackingCriteriaForSpawner(cs, location);
            }
            else {
                // chunk unloaded
                if (!sp.activeSpawners.containsKey(location)) continue;

                sp.activeSpawners.get(location).isChunkLoaded = false;
            }
        }
    }

    private void processSpawnerOrChunkAdd(final @NotNull SpawnerUpdateItem item){
        this.allSpawners.put(item.basicLocation, item.cs);

        evaluateTrackingCriteriaForSpawner(item.cs, item.basicLocation);
    }

    private void recheckSpawnerCriteria(){
        this.recheckCriteria = false;

        for (final BasicLocation basicLocation : this.allSpawners.keySet()){
            final CreatureSpawner cs = this.allSpawners.get(basicLocation);
            if (main.spawnerOptions.allowedWorlds.isEnabledInList(cs.getWorld().getName()))
                evaluateTrackingCriteriaForSpawner(cs, basicLocation);
        }
    }

    private void evaluateTrackingCriteriaForSpawner(final @NotNull CreatureSpawner cs, final @NotNull BasicLocation basicLocation){
        SpawnerInfo info = sp.activeSpawners.get(basicLocation);
        if (info == null)
            info = new SpawnerInfo(cs, sp.options);

        info.isChunkLoaded = cs.getLocation().isChunkLoaded();

        if (main.namedSpawnerOptions != null) {
            final String spawnerCustomName = info.getSpawnerCustomName(main);
            if (spawnerCustomName != null) {
                Utils.logger.info(Utils.showSpawnerLocation(cs) + ", customName: " + spawnerCustomName + ", result: " +
                        (main.namedSpawnerOptions.containsKey(spawnerCustomName)));
            }
            if (spawnerCustomName != null && main.namedSpawnerOptions.containsKey(spawnerCustomName))
                info.options = main.namedSpawnerOptions.get(spawnerCustomName);
            else
                info.options = main.spawnerOptions;
        }

        if (sp.hasWorldGuard)
            WorldGuardManager.updateWorlguardOptionsForSpawner(main, info);

        if (info.options == null)
            info.options = sp.options;

        final Set<BasicLocation> spawnersInChunk = this.chunkMappings.computeIfAbsent(cs.getLocation().getChunk().getChunkKey(), k -> new HashSet<>());
        spawnersInChunk.add(info.getBasicLocation());

        final Set<BasicLocation> spawnersInWorld = this.worldMappings.computeIfAbsent(cs.getLocation().getWorld().getName(), k -> new HashSet<>());
        spawnersInWorld.add(info.getBasicLocation());

        final String name = info.getSpawnerCustomName(main);
        if (name != null && name.startsWith("test")){
            final boolean test = info.options.allowedEntityTypes.isEnabledInList(cs.getSpawnedType());
            Utils.logger.info("name: " + name + ", is allowed: " + test + ", " + info.options);
        }

        if (info.options.allowedEntityTypes.isEnabledInList(cs.getSpawnedType())) {

            if (info.isChunkLoaded && !sp.activeSpawners.containsKey(info.getBasicLocation())) {
                //Utils.logger.info("now tracking spawner: " + Utils.showSpawnerLocation(cs));
                sp.activeSpawners.put(info.getBasicLocation(), info);
            }
            else if (!info.isChunkLoaded){
                sp.activeSpawners.remove(info.getBasicLocation());
            }

        }
        else if (sp.activeSpawners.containsKey(info.getBasicLocation())) {
            Utils.logger.info("no longer tracking spawner: " + Utils.showSpawnerLocation(cs));
            sp.activeSpawners.remove(info.getBasicLocation());
        }
        else if (name != null && name.startsWith("test")){
            Utils.logger.info("test 10");
        }
    }

    public void spawnerGotRenamed(final @NotNull CreatureSpawner cs, final @Nullable String oldName, final @Nullable String newName){
        final SpawnerUpdateItem update = new SpawnerUpdateItem(cs, UpdateOperation.CUSTOM_NAME_CHANGE);
        update.oldName = oldName;
        update.newName = newName;

        synchronized (queueLock){
            this.spawnerUpdateQueue.add(update);
        }
    }

    public void updateSpawner(final @NotNull CreatureSpawner cs, final @NotNull UpdateOperation operation){
        synchronized (queueLock) {
            this.spawnerUpdateQueue.add(new SpawnerUpdateItem(cs, operation));
        }
    }

    public void updateChunk(final long chunkId, final @NotNull UpdateOperation operation){
        synchronized (queueLock){
            this.spawnerUpdateQueue.add(new SpawnerChunkUpdate(chunkId, operation));
        }
    }

    public void configReloaded(){
        this.recheckCriteria = true;
        sp.lastWGCheckTicks = -1;
    }

    public boolean hasAlreadyProcessedChunk(final long chunkId){
        synchronized (chunkLock){
            return this.chunkMappings.containsKey(chunkId);
        }
    }

    public int getAllKnownSpawnersCount(){
        return this.allSpawners.size();
    }
}
