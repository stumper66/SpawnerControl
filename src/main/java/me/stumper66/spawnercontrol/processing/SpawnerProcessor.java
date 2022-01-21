package me.stumper66.spawnercontrol.processing;

import me.lokka30.microlib.other.VersionUtils;
import me.stumper66.spawnercontrol.DebugType;
import me.stumper66.spawnercontrol.SpawnerControl;
import me.stumper66.spawnercontrol.SpawnerInfo;
import me.stumper66.spawnercontrol.SpawnerOptions;
import me.stumper66.spawnercontrol.SpigotCompat;
import me.stumper66.spawnercontrol.Utils;
import me.stumper66.spawnercontrol.WorldGuardManager;
import org.bukkit.Bukkit;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SpawnerProcessor {

    public SpawnerProcessor(final SpawnerControl main) {
        this.main = main;
        this.activeSpawners = new HashMap<>();
        this.activeSpawnerList = new HashSet<>();
        this.options = new SpawnerOptions();
        this.lastWGCheckTicks = -1;
        this.hasWorldGuard = SpawnerControl.isWorldGuardInstalled();
        this.spawnerCustomNameKey = new NamespacedKey(main, "spawnerCustomNameKey");
        this.updateProcessor = new UpdateProcessor(this);
        this.invalidActiveSpawners = new LinkedList<>();
    }

    final SpawnerControl main;
    final @NotNull Map<BasicLocation, SpawnerInfo> activeSpawners;
    private final Set<BasicLocation> activeSpawnerList;
    final boolean hasWorldGuard;
    boolean activeSpawnersNeedsUpdating;
    @NotNull SpawnerOptions options;
    int lastWGCheckTicks;
    private final UpdateProcessor updateProcessor;
    private final List<BasicLocation> invalidActiveSpawners;
    private final static int ticksPerCall = 40;
    public final NamespacedKey spawnerCustomNameKey;
    public UUID currentSpawningEntityId;
    public final static Object lock_ActiveSpawners = new Object();
    private boolean threadIsProcessing;

    public void startProcessing() {
        if (!main.isEnabled) return;
        if (threadIsProcessing) return;

        try {
            threadIsProcessing = true;
            processSpawners();
        }
        finally {
            threadIsProcessing = false;
        }
    }

    private void processSpawners(){
        if (main.spawnerOptions != null)
            this.options = main.spawnerOptions;

        updateProcessor.processUpdates();
        if (Bukkit.getOnlinePlayers().size() == 0) return;

        if (hasWorldGuard && lastWGCheckTicks == -1 || lastWGCheckTicks >= 160){
            // updates roughly every 8 seconds
            this.lastWGCheckTicks = 0;
            final Map<BasicLocation, CreatureSpawner> allSpawnersCopy = new HashMap<>(updateProcessor.allSpawners.size());
            allSpawnersCopy.putAll(updateProcessor.allSpawners);
            WorldGuardManager.updateWorlguardOptionsForTrackedSpawners(main, allSpawnersCopy, this.activeSpawners);
        }
        else if (hasWorldGuard)
            this.lastWGCheckTicks += ticksPerCall;

        if (this.activeSpawners.isEmpty()) return;
        final Set<BasicLocation> spawnersMeetingLightCriteria = getSpawnersMeetingLightCriteria();
        if (spawnersMeetingLightCriteria.isEmpty()){
            if (main.debugInfo.doesSpawnerMeetDebugCriteria(DebugType.LIGHT_REQ_NOT_MET))
                Utils.logger.info("No spawners met light level criteria");
            return;
        }

        makeSureSpawnersStillExist();
        tickSpawnersForResetPeriod();

        if (main.debugInfo.doesSpawnerMeetDebugCriteria(DebugType.LIGHT_REQ_NOT_MET))
            Utils.logger.info("Spawners meeting light level criteria: " + spawnersMeetingLightCriteria.size());

        final Set<BasicLocation> spawnersToCheck = getSpawnersWithinPlayerActivationRange(spawnersMeetingLightCriteria);
        if (main.debugInfo.doesSpawnerMeetDebugCriteria(DebugType.ACTIVE_SPAWNERS))
            Utils.logger.info("player activated spawners count: " + spawnersToCheck.size());

        for (final BasicLocation location : spawnersToCheck) {
            final SpawnerInfo info = activeSpawners.get(location);
            if (info == null) continue;

            if (shouldSpawnerSpawnNow(info)) {
                if (main.debugInfo.doesSpawnerMeetDebugCriteria(main, DebugType.ACTIVE_SPAWNERS, info))
                    Utils.logger.info("attempting spawn from " + Utils.showSpawnerLocation(info.getCs()));
                final SpawnerOptions opts = info.options != null ?
                        info.options : options;
                spawnEntities(info, opts);

                info.resetTimeLeft(options);
            }
        }
    }

    private void makeSureSpawnersStillExist(){
        if (!invalidActiveSpawners.isEmpty()) this.activeSpawnersNeedsUpdating = true;

        for (final BasicLocation basicLocation : invalidActiveSpawners){
            activeSpawners.remove(basicLocation);
            updateProcessor.allSpawners.remove(basicLocation);
        }

        updateActiveSpawnerList();
    }

    void updateActiveSpawnerList(){
        if (!this.activeSpawnersNeedsUpdating) return;

        synchronized (lock_ActiveSpawners){
            this.activeSpawnerList.clear();
            this.activeSpawnerList.addAll(this.activeSpawners.keySet());
        }

        this.activeSpawnersNeedsUpdating = false;
    }

    @NotNull
    private Set<BasicLocation> getSpawnersMeetingLightCriteria(){
        final Set<BasicLocation> spawners = new HashSet<>();

        for (final BasicLocation basicLocation : activeSpawners.keySet()){
            final SpawnerInfo info = activeSpawners.get(basicLocation);
            if (info.options == null) info.options = new SpawnerOptions();

            final int lightLevel = info.getCs().getBlock().getLightLevel();
            if (lightLevel < info.options.allowedLightLevel_Min || lightLevel > info.options.allowedLightLevel_Max) {
                if (main.debugInfo.doesSpawnerMeetDebugCriteria(main, DebugType.LIGHT_REQ_NOT_MET, info)) {
                    Utils.logger.info(String.format(
                            "Spawner doesn't meet light criteria of %s-%s. light level: %s. %s",
                            info.options.allowedLightLevel_Min, info.options.allowedLightLevel_Max, info.getCs().getBlock().getLightLevel(), Utils.showSpawnerLocation(info.getCs())));
                }
                continue;
            }

            final int skyLightLevel = info.getCs().getBlock().getLightFromSky();
            if (skyLightLevel < info.options.allowedSkyLightLevel_Min || skyLightLevel > info.options.allowedSkyLightLevel_Max) {
                if (main.debugInfo.doesSpawnerMeetDebugCriteria(main, DebugType.LIGHT_REQ_NOT_MET, info)) {
                    Utils.logger.info(String.format(
                            "Spawner doesn't meet sky light criteria of %s-%s. block light level: %s. %s",
                            info.options.allowedSkyLightLevel_Min, info.options.allowedSkyLightLevel_Max, info.getCs().getBlock().getLightFromSky(), Utils.showSpawnerLocation(info.getCs())));
                }
                continue;
            }

            final int blockLightLevel = info.getCs().getBlock().getLightFromBlocks();
            if (blockLightLevel < info.options.allowedBlockLightLevel_Min || blockLightLevel > info.options.allowedBlockLightLevel_Max) {
                if (main.debugInfo.doesSpawnerMeetDebugCriteria(main, DebugType.LIGHT_REQ_NOT_MET, info)) {
                    Utils.logger.info(String.format(
                            "Spawner doesn't meet block light criteria of %s-%s. block light level: %s. %s",
                            info.options.allowedBlockLightLevel_Min, info.options.allowedBlockLightLevel_Max, info.getCs().getBlock().getLightFromBlocks(), Utils.showSpawnerLocation(info.getCs())));
                }
                continue;
            }

            spawners.add(basicLocation);
        }

        return spawners;
    }

    @NotNull
    private Set<BasicLocation> getSpawnersWithinPlayerActivationRange(final Set<BasicLocation> spawnersToCompare){
        final Set<BasicLocation> spawners = new HashSet<>();

        for (final Player player : Bukkit.getOnlinePlayers()){
            if (!updateProcessor.worldMappings.containsKey(player.getWorld().getName()))
                continue;

            for (final BasicLocation basicLocation : updateProcessor.worldMappings.get(player.getWorld().getName())){
                // already added this one to the list
                if (spawners.contains(basicLocation))
                    continue;

                final SpawnerInfo info = activeSpawners.get(basicLocation);
                if (info == null || !info.isChunkLoaded || info.options == null)
                    continue;

                // must have met light level criteria
                if (!spawnersToCompare.contains(basicLocation))
                    continue;

                final long distance = (long) Math.ceil(info.getCs().getLocation().distanceSquared(player.getLocation()));
                if (distance <= info.options.getEffectivePlayerRequiredRange())
                    spawners.add(basicLocation);
            }

            // don't check the rest of the players if all active spawners have at least one player within range
            if (spawners.size() == this.activeSpawners.size()) break;
        }

        return spawners;
    }


    @NotNull
    public Collection<SpawnerInfo> getMonitoredSpawners(){
        return this.activeSpawners.values();
    }

    private void tickSpawnersForResetPeriod(){
        for (final SpawnerInfo info : activeSpawners.values()){
            if (info.options != null && info.hasHadInitialSpawn && info.options.immediateSpawnResetPeriod > 0 &&
                info.immediateSpawnResetTimeLeft > 0){
                info.immediateSpawnResetTimeLeft -= ticksPerCall;
                if (info.immediateSpawnResetTimeLeft <= 0)
                    info.hasHadInitialSpawn = false;
            }
        }
    }

    private boolean shouldSpawnerSpawnNow(final @NotNull SpawnerInfo info) {
        if (info.options != null && info.options.doImmediateSpawn && !info.hasHadInitialSpawn){
            info.hasHadInitialSpawn = true;
            return true;
        }

        if (info.options != null && info.options.immediateSpawnResetPeriod > 0)
            info.immediateSpawnResetTimeLeft = info.options.immediateSpawnResetPeriod;

        info.delayTimeLeft -= ticksPerCall;

        return info.delayTimeLeft <= 0;
    }

    private void makeParticles(final @NotNull Location location) {
        if (location.getWorld() == null) return;

        location.getWorld().spawnParticle(Particle.SMOKE_NORMAL, location, 5, null);
        location.getWorld().spawnParticle(Particle.FLAME, location, 5, null);
    }

    private void spawnEntities(final @NotNull SpawnerInfo info, final @NotNull SpawnerOptions options) {
        int similarEntityCount = 0;

        final Future<Collection<Entity>> futureEntities = getNearbyEntity_NonAsync(info.getCs().getLocation(), options);
        Collection<Entity> nearbyEntities;
        try {
            nearbyEntities = futureEntities.get(100L, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException | ConcurrentModificationException | ExecutionException | TimeoutException e){
            e.printStackTrace();
            return;
        }

        for (final Entity entity : nearbyEntities){
            if (entity.getType() == info.getCs().getSpawnedType())
                similarEntityCount++;
        }

        if (similarEntityCount >= options.maxNearbyEntities) {
            if (main.debugInfo.doesSpawnerMeetDebugCriteria(main, DebugType.SPAWN_ATTEMPT_FAILED, info))
                Utils.logger.info("too many similar entities - " + similarEntityCount + ", " + Utils.showSpawnerLocation(info.getCs()));
            return;
        }

        final int currentCount = similarEntityCount;
        final BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                spawnEntity_NonAsync(info, currentCount);
            }
        };

        runnable.runTask(main);
    }

    private void spawnEntity_NonAsync(final @NotNull SpawnerInfo info, int similarEntityCount){
        final CreatureSpawner cs = info.getCs();
        if (info.options == null) info.options = this.options;

        int spawnCount = info.options.spawnCount_Min;
        if (info.options.spawnCount_Max > spawnCount)
            spawnCount = ThreadLocalRandom.current().nextInt(info.options.spawnCount_Min, info.options.spawnCount_Max + 1);

        for (int i = 0; i < spawnCount; i++) {
            if (i == 0){
                // verify the spawner still exists first
                if (info.getBasicLocation().getLocation().getBlock().getType() != Material.SPAWNER){
                    invalidActiveSpawners.add(info.getBasicLocation());
                    if (main.debugInfo.doesSpawnerMeetDebugCriteria(main, DebugType.SPAWNER_DEACTIVATION, info))
                        Utils.logger.info("Spawner doesn't exist anymore: " + Utils.showSpawnerLocation(info.getCs()));

                    return;
                }
            }

            final Location spawnLocation = getSpawnLocation(cs.getLocation(), cs.getSpawnedType());
            if (spawnLocation == null) {
                if (main.debugInfo.doesSpawnerMeetDebugCriteria(main, DebugType.SPAWN_ATTEMPT_FAILED, info))
                    Utils.logger.info("Could not find a suitable spawn location: " + Utils.showSpawnerLocation(cs));
                continue;
            }

            if (main.debugInfo.doesSpawnerMeetDebugCriteria(main, DebugType.SPAWN_ATTEMPT_SUCCESS, info)) {
                Utils.logger.info(String.format("spawning %s at %s, %s, %s",
                        cs.getSpawnedType().name(), spawnLocation.getBlockX(), spawnLocation.getBlockY(), spawnLocation.getBlockZ()));
            }

            // if you're running spigot then they will spawn in with default spawn reason
            // if running paper they will spawn in with spawner spawn reason
            final Entity entity = VersionUtils.isRunningPaper() ?
                    cs.getWorld().spawnEntity(spawnLocation, cs.getSpawnedType(), CreatureSpawnEvent.SpawnReason.SPAWNER) :
                    SpigotCompat.spawnEntity(spawnLocation, cs.getSpawnedType());

            this.currentSpawningEntityId = entity.getUniqueId();
            Bukkit.getPluginManager().callEvent(new SpawnerSpawnEvent(entity, cs));
            this.currentSpawningEntityId = null;

            makeParticles(entity.getLocation());

            if (entity instanceof Slime && (info.options.slimeSizeMin != null || info.options.slimeSizeMax != null)) {
                final Slime slime = (Slime) entity;
                int useMin = info.options.slimeSizeMin != null ? info.options.slimeSizeMin : 1;
                int useMax = info.options.slimeSizeMax != null ? info.options.slimeSizeMax : 4;
                if (useMin > useMax) useMin = useMax;
                final int useSize = useMin == useMax ?
                        useMin :
                        ThreadLocalRandom.current().nextInt(useMin, useMax + 1);

                if (main.debugInfo.doesSpawnerMeetDebugCriteria(main, DebugType.ENTITY_SPECIFIC, info))
                    Utils.logger.info("setting slime size to " + useSize + ", " + Utils.showSpawnerLocation(info.getCs()));
                slime.setSize(useSize);
            }

            similarEntityCount++;
            if (similarEntityCount >= info.options.maxNearbyEntities) break;
        }
    }

    @NotNull
    private Future<Collection<Entity>> getNearbyEntity_NonAsync(final @NotNull Location location, final @NotNull SpawnerOptions options){
        final CompletableFuture<Collection<Entity>> completableFuture = new CompletableFuture<>();

        final BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                if (!location.getChunk().isLoaded()){
                    completableFuture.complete(null);
                    return;
                }
                final Collection<Entity> result =
                        location.getWorld().getNearbyEntities(location, options.spawnRange, options.spawnRange, options.spawnRange);

                completableFuture.complete(result);
            }
        };

        runnable.runTask(main);
        return completableFuture;
    }

    @Nullable
    private Location getSpawnLocation(final @NotNull Location spawnerLocation, final @NotNull EntityType entityType) {
        if (spawnerLocation.getWorld() == null) return null;

        final World world = spawnerLocation.getWorld();
        final int x1 = spawnerLocation.getBlockX();
        final int y1 = spawnerLocation.getBlockY();
        final int z1 = spawnerLocation.getBlockZ();
        final List<Block> blockCandidates = new LinkedList<>();
        int blocksNeeded;
        if (entityType == EntityType.ENDERMAN || entityType == EntityType.RAVAGER)
            blocksNeeded = 3;
        else
            blocksNeeded = 2;

        for (int i = 0; i < 50; i++) {
            int i0 = (int) (x1 + ThreadLocalRandom.current().nextDouble() - ThreadLocalRandom.current().nextDouble() * (double) options.spawnRange + 0.5D);
            int i1 = y1 + ThreadLocalRandom.current().nextInt(options.spawnRange) - 1;
            int i2 = (int) (z1 + (ThreadLocalRandom.current().nextDouble()) - ThreadLocalRandom.current().nextDouble() * (double) options.spawnRange + 0.5D);
            final Block block = world.getBlockAt(i0, i1, i2);
            if (    options.allowAirSpawning && block.getType().isAir() ||
                    block.getType().isSolid() && !blockCandidates.contains(block)) {
                blockCandidates.add(block);
            }

            if (blockCandidates.size() >= 10) break;
        }

        if (blockCandidates.isEmpty())
            return null;

        Collections.shuffle(blockCandidates);

        // return first block from the candiates that has 2 air spaces above it
        for (final Block block : blockCandidates){
            boolean notGoodSpot = false;
            for (int i = 1; i < blocksNeeded + 1; i++){
                if (!world.getBlockAt(block.getX(), block.getY() + i, block.getZ()).getType().isAir()){
                    notGoodSpot = true;
                    break;
                }
            }

            if (notGoodSpot) continue;

            return block.getLocation().add(0.5, 1.0, 0.5);
        }

        return null;
    }

    public void configReloaded(){
        this.updateProcessor.configReloaded();
    }

    public int getActiveSpawnersCount() {
        synchronized (lock_ActiveSpawners) {
            return this.activeSpawnerList.size();
        }
    }

    public void spawnerGotRenamed(final @NotNull CreatureSpawner cs, final @Nullable String oldName, final @Nullable String newName){
        final SpawnerUpdateItem update = new SpawnerUpdateItem(cs, UpdateOperation.CUSTOM_NAME_CHANGE);
        update.oldName = oldName;
        update.newName = newName;

        updateProcessor.spawnerUpdateQueue.add(update);
    }

    public void updateSpawner(final @NotNull CreatureSpawner cs, final @NotNull UpdateOperation operation){
        updateProcessor.spawnerUpdateQueue.add(new SpawnerUpdateItem(cs, operation));
    }

    public void updateChunk(final long chunkId, final @NotNull UpdateOperation operation){
        updateProcessor.spawnerUpdateQueue.add(new SpawnerChunkUpdate(chunkId, operation));
    }

    public void enumerateChunk(final @NotNull ChunkSnapshot chunkSnapshot, final @NotNull World world){
        updateProcessor.spawnerUpdateQueue.add(new ChunkEnumeratorItem(chunkSnapshot, world));
    }

    public boolean hasAlreadyProcessedChunk(final long chunkId){
        synchronized (UpdateProcessor.chunkLock){
            return updateProcessor.chunkMappings.containsKey(chunkId);
        }
    }

    public int getAllKnownSpawnersCount(){
        return updateProcessor.allSpawners.size();
    }

    public boolean isSpawnerActive(final @NotNull CreatureSpawner cs){
        final BasicLocation basicLocation = new BasicLocation(cs.getLocation());
        synchronized (lock_ActiveSpawners){
            return this.activeSpawnerList.contains(basicLocation);
        }
    }

    public void reevaluateSpawner(final @NotNull SpawnerInfo spawnerInfo){
        updateProcessor.evaluateTrackingCriteriaForSpawner(spawnerInfo);
    }
}
