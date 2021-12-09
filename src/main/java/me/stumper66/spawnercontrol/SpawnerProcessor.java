package me.stumper66.spawnercontrol;

import me.lokka30.microlib.other.VersionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Entity;
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
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SpawnerProcessor {

    public SpawnerProcessor(final SpawnerControl main) {
        this.main = main;
        this.allSpawners = new HashMap<>();
        this.spawnerTracking = new HashMap<>();
        this.chunkMappings = new HashMap<>();
        this.worldMappings = new HashMap<>();
        this.spawnerUpdateQueue = new LinkedList<>();
        this.options = new SpawnerOptions();
        this.lastWGCheckTicks = -1;
        this.hasWorldGuard = SpawnerControl.isWorldGuardInstalled();
    }

    private final SpawnerControl main;
    private @NotNull SpawnerOptions options;
    private final @NotNull Map<Location, CreatureSpawner> allSpawners;
    private final @NotNull Map<Location, SpawnerInfo> spawnerTracking;
    private final @NotNull Map<Long, Set<Location>> chunkMappings;
    private final @NotNull Map<String, Set<Location>> worldMappings;
    private final static int ticksPerCall = 40;
    private int lastWGCheckTicks;
    private int activeSpawnersCount;
    private final boolean hasWorldGuard;
    private boolean recheckCriteria;
    private final @NotNull Queue<SpawnerUpdateInterface> spawnerUpdateQueue;
    private final static Object queueLock = new Object();
    private final static Object chunkLock = new Object();

    public void processSpawners() {
        if (!main.isEnabled) return;
        if (main.spawnerOptions != null)
            this.options = main.spawnerOptions;

        if (recheckCriteria) recheckSpawnerCriteria();
        checkUpdateQueue();
        if (Bukkit.getOnlinePlayers().size() == 0) return;

        if (this.spawnerTracking.isEmpty()) {
            //Utils.logger.info("no tracked spawners to process");
            return;
        }

        if (hasWorldGuard && lastWGCheckTicks == -1 || lastWGCheckTicks >= 160){
            // update roughly every 8 seconds
            this.lastWGCheckTicks = 0;
            WorldGuardManager.updateWorlguardOptionsForTrackedSpawners(main, this.spawnerTracking);
        }
        else if (hasWorldGuard)
            this.lastWGCheckTicks += ticksPerCall;

        final Set<Location> spawnersToCheck = getSpawnersWithinPlayerActivationRange();
        //Utils.logger.info("spawners to check: " + spawnersToCheck.size());
        for (final Location location : spawnersToCheck) {
            final SpawnerInfo info = spawnerTracking.get(location);
            if (info == null) continue;

            final CreatureSpawner cs = info.cs;

            if (shouldSpawnerSpawnNow(cs)) {
                final SpawnerOptions opts = info.options != null ?
                        info.options : options;
                spawnEntities(info, opts);

                spawnerTracking.get(cs.getLocation()).resetTimeLeft(options);
            }
        }
    }

    @NotNull
    private Set<Location> getSpawnersWithinPlayerActivationRange(){
        final Set<Location> spawners = new HashSet<>();

        for (final Player player : Bukkit.getOnlinePlayers()){
            if (!worldMappings.containsKey(player.getWorld().getName())) continue;

            for (final Location location : worldMappings.get(player.getWorld().getName())){
                if (spawners.contains(location)) continue;
                final SpawnerInfo info = spawnerTracking.get(location);
                if (info == null || !info.isChunkLoaded || info.options == null)
                    continue;

                final long distance = (long) Math.ceil(info.cs.getLocation().distanceSquared(player.getLocation()));
                if (distance <= info.options.playerRequiredRange)
                    spawners.add(location);
            }

            // don't check the rest of the players if all active spawners have at least one player within range
            if (spawners.size() == this.activeSpawnersCount) break;
        }

        return spawners;
    }

    private void recheckSpawnerCriteria(){
        this.recheckCriteria = false;

        for (final CreatureSpawner cs : this.allSpawners.values()){
            if (main.spawnerOptions.allowedWorlds.isEnabledInList(cs.getWorld().getName()))
                trackSpawnerIfApplicable(cs, false);
        }
    }

    private void checkUpdateQueue(){
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
                default: // update and add
                    processSpawnerOrChunkAdd((SpawnerUpdateItem) itemInterface);
                    break;
            }
        }
    }

    private void removeSpawner(final @NotNull SpawnerUpdateItem item){
        this.spawnerTracking.remove(item.cs.getLocation());
        if (this.chunkMappings.containsKey(item.cs.getLocation().getChunk().getChunkKey()))
            this.chunkMappings.get(item.cs.getLocation().getChunk().getChunkKey()).remove(item.cs.getLocation());
        this.allSpawners.remove(item.cs.getLocation());
    }

    private void processChunk(final @NotNull SpawnerChunkUpdate spawnerChunkUpdate){
        Set<Location> locations;

        synchronized (chunkLock) {
            if (!this.chunkMappings.containsKey(spawnerChunkUpdate.chunkId)) return;
            locations = this.chunkMappings.get(spawnerChunkUpdate.chunkId);
        }

        if (locations == null) return;

        for (final Location location : locations){
            if (spawnerChunkUpdate.getOperation() == UpdateOperation.CHUNK_REFRESH){
                final CreatureSpawner cs = this.allSpawners.get(location);
                if (cs == null) continue;

                trackSpawnerIfApplicable(cs, false);
            }
            else {
                // chunk unloaded
                if (!this.spawnerTracking.containsKey(location)) continue;

                this.spawnerTracking.get(location).isChunkLoaded = false;
                this.activeSpawnersCount--;
            }
        }
    }

    private void processSpawnerOrChunkAdd(final @NotNull SpawnerUpdateItem item){
        this.allSpawners.put(item.cs.getLocation(), item.cs);
        final boolean isAdd = item.getOperation() == UpdateOperation.ADD;

        trackSpawnerIfApplicable(item.cs, isAdd);
    }

    private void trackSpawnerIfApplicable(final @NotNull CreatureSpawner cs, final boolean isAdd){
        SpawnerInfo info = spawnerTracking.get(cs.getLocation());
        if (info == null)
            info = new SpawnerInfo(cs, options);
        else
            info.isChunkLoaded = true;

        if (hasWorldGuard)
            WorldGuardManager.updateWorlguardOptionsForSpawner(main, info);

        if (info.options == null)
            info.options = this.options;

        if (info.options.allowedEntityTypes.isEnabledInList(cs.getSpawnedType())) {
            //Utils.logger.info("now tracking spawner: " + Utils.showSpawnerLocation(cs));

            this.spawnerTracking.put(cs.getLocation(), info);

            if (isAdd) {
                final Set<Location> spawnersInChunk = this.chunkMappings.computeIfAbsent(cs.getLocation().getChunk().getChunkKey(), k -> new HashSet<>());
                spawnersInChunk.add(cs.getLocation());

                final Set<Location> spawnersInWorld = this.worldMappings.computeIfAbsent(cs.getLocation().getWorld().getName(), k -> new HashSet<>());
                spawnersInWorld.add(cs.getLocation());
            }

            this.activeSpawnersCount++;
        }
        else if (this.spawnerTracking.containsKey(cs.getLocation())) {
//            if (!isAdd)
//                Utils.logger.info("no longer tracking spawner: " + Utils.showSpawnerLocation(cs));
            this.spawnerTracking.remove(cs.getLocation());
            if (this.chunkMappings.containsKey(cs.getLocation().getChunk().getChunkKey()))
                this.chunkMappings.get(cs.getLocation().getChunk().getChunkKey()).remove(cs.getLocation());
            if (this.worldMappings.containsKey(cs.getLocation().getWorld().getName()))
                this.worldMappings.get(cs.getLocation().getWorld().getName()).remove(cs.getLocation());
            this.activeSpawnersCount--;
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
        this.lastWGCheckTicks = -1;
    }

    @NotNull
    public Collection<SpawnerInfo> getMonitoredSpawners(){
        return this.spawnerTracking.values();
    }

    private boolean shouldSpawnerSpawnNow(final @NotNull CreatureSpawner cs) {
        SpawnerInfo info;

        if (!this.spawnerTracking.containsKey(cs.getLocation())){
            info = new SpawnerInfo(cs, options);
            this.spawnerTracking.put(cs.getLocation(), info);
        }
        else {
            info = this.spawnerTracking.get(cs.getLocation());
            info.delayTimeLeft -= ticksPerCall;
        }

        return info.delayTimeLeft <= 0;
    }

    private void makeParticles(final @NotNull Location location) {
        if (location.getWorld() == null) return;

        location.getWorld().spawnParticle(Particle.SMOKE_NORMAL, location, 5, null);
        location.getWorld().spawnParticle(Particle.FLAME, location, 5, null);
    }

    private void spawnEntities(final @NotNull SpawnerInfo info, final @NotNull SpawnerOptions options) {
        int similarEntityCount = 0;

        final Future<Collection<Entity>> futureEntities = getNearbyEntity_NonAsync(info.cs.getLocation(), options);
        Collection<Entity> nearbyEntities;
        try {
            nearbyEntities = futureEntities.get(100L, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException | ConcurrentModificationException | ExecutionException | TimeoutException e){
            e.printStackTrace();
            return;
        }

        for (final Entity entity : nearbyEntities){
            if (entity.getType() == info.cs.getSpawnedType())
                similarEntityCount++;
        }

        if (similarEntityCount >= options.maxNearbyEntities) {
            //Utils.logger.info("too many similar entities - " + similarEntityCount + ", " + Utils.showSpawnerLocation(info.cs));
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
        final CreatureSpawner cs = info.cs;
        if (info.options == null) info.options = this.options;

        for (int i = 0; i < info.options.spawnCount; i++) {
            final Location spawnLocation = getSpawnLocation(cs.getLocation());
            if (spawnLocation == null) {
                //Utils.logger.info("spawn location was null: " + Utils.showSpawnerLocation(cs));
                continue;
            }

//            Utils.logger.info(String.format("spawning %s at %s, %s, %s" ,
//                    cs.getSpawnedType().name(), spawnLocation.getBlockX(), spawnLocation.getBlockY(), spawnLocation.getBlockZ()));

            // if you're running spigot then they will spawn in with default spawn reason
            // if running paper they will spawn in with spawner spawn reason
            final Entity entity = VersionUtils.isRunningPaper() ?
                    cs.getWorld().spawnEntity(spawnLocation, cs.getSpawnedType(), CreatureSpawnEvent.SpawnReason.SPAWNER) :
                    SpigotCompat.spawnEntity(spawnLocation, cs.getSpawnedType());

            Bukkit.getPluginManager().callEvent(new SpawnerSpawnEvent(entity, cs));

            makeParticles(entity.getLocation());

            if (entity instanceof Slime && (info.options.slimeSizeMin != null || info.options.slimeSizeMax != null)) {
                final Slime slime = (Slime) entity;
                int useMin = info.options.slimeSizeMin != null ? info.options.slimeSizeMin : 1;
                int useMax = info.options.slimeSizeMax != null ? info.options.slimeSizeMax : 4;
                if (useMin > useMax) useMin = useMax;
                final int useSize = useMin == useMax ?
                        useMin :
                        ThreadLocalRandom.current().nextInt(useMin, useMax + 1);

                //Utils.logger.info("setting slime size to " + useSize);
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
    private Location getSpawnLocation(final @NotNull Location spawnerLocation) {
        if (spawnerLocation.getWorld() == null) return null;

        final World world = spawnerLocation.getWorld();
        final int x1 = spawnerLocation.getBlockX();
        final int y1 = spawnerLocation.getBlockY();
        final int z1 = spawnerLocation.getBlockZ();
        final List<Block> blockCandidates = new LinkedList<>();

        for (int i = 0; i < 50; i++) {
            int i0 = (int) (x1 + ThreadLocalRandom.current().nextDouble() - ThreadLocalRandom.current().nextDouble() * (double) options.spawnRange + 0.5D);
            int i1 = y1 + ThreadLocalRandom.current().nextInt(options.spawnRange) - 1;
            int i2 = (int) (z1 + (ThreadLocalRandom.current().nextDouble()) - ThreadLocalRandom.current().nextDouble() * (double) options.spawnRange + 0.5D);
            final Block block = world.getBlockAt(i0, i1, i2);
            if (    options.allowAirSpawning && block.getType().isAir() ||
                    block.getType().isSolid() && !blockCandidates.contains(block)) {
                blockCandidates.add(block);
            }

            if (blockCandidates.size() >= 6) break;
        }

        if (blockCandidates.isEmpty())
            return null;

        Collections.shuffle(blockCandidates);

        // return first block from the candiates that has 2 air spaces above it
        for (final Block block : blockCandidates){
            if (world.getBlockAt(block.getX(), block.getY() + 1, block.getZ()).getType().isAir() &&
                    world.getBlockAt(block.getX(), block.getY() + 2, block.getZ()).getType().isAir()){
                int addX = 1;
                int addZ = 0;
                for (int x = 0; x < 4; x++){
                    if (x == 1) addX = -1;
                    else if (x == 2) {
                        addZ = 1;
                        addX = 0;
                    }
                    else if (x == 3) addZ = -1;

                    final Block blockCheck = world.getBlockAt(block.getX() + addX, block.getY() + 1, block.getZ() + addZ);
                    if (blockCheck.getType().isAir() &&
                            world.getBlockAt(block.getX() + addX, block.getY() + 2, block.getZ() + addZ).getType().isAir()){
                        // found an adjacent space. shift the spawn point over by 0.5 to prevent spawning in the wall
                        final double newX = (double) addX * 0.5;
                        final double newZ = (double) addZ * 0.5;

                        return block.getLocation().add(newX, 1.0, newZ);
                    }
                }
            }
        }

        return null;
    }

    public boolean hasAlreadyProcessedChunk(final long chunkId){
        synchronized (chunkLock){
            return this.chunkMappings.containsKey(chunkId);
        }
    }

    public int getActiveSpawnersCount(){
        return this.activeSpawnersCount;
    }
}
