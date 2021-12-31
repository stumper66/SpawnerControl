package me.stumper66.spawnercontrol.processing;

import me.lokka30.microlib.other.VersionUtils;
import me.stumper66.spawnercontrol.SpawnerControl;
import me.stumper66.spawnercontrol.SpawnerInfo;
import me.stumper66.spawnercontrol.SpawnerOptions;
import me.stumper66.spawnercontrol.SpigotCompat;
import me.stumper66.spawnercontrol.Utils;
import me.stumper66.spawnercontrol.WorldGuardManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
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
        this.activeSpawners = new HashMap<>();
        this.options = new SpawnerOptions();
        this.lastWGCheckTicks = -1;
        this.hasWorldGuard = SpawnerControl.isWorldGuardInstalled();
        this.spawnerCustomNameKey = new NamespacedKey(main, "spawnerCustomNameKey");
        this.updateProcessor = new UpdateProcessor(this);
    }

    final SpawnerControl main;
    final @NotNull Map<BasicLocation, SpawnerInfo> activeSpawners;
    final boolean hasWorldGuard;
    @NotNull SpawnerOptions options;
    int lastWGCheckTicks;
    private final UpdateProcessor updateProcessor;
    private final static int ticksPerCall = 40;
    public final NamespacedKey spawnerCustomNameKey;

    public void processSpawners() {
        if (!main.isEnabled) return;
        if (main.spawnerOptions != null)
            this.options = main.spawnerOptions;

        updateProcessor.processUpdates();
        if (Bukkit.getOnlinePlayers().size() == 0) return;

        if (this.activeSpawners.isEmpty()) {
            //Utils.logger.info("no tracked spawners to process");
            return;
        }

        if (hasWorldGuard && lastWGCheckTicks == -1 || lastWGCheckTicks >= 160){
            // updates roughly every 8 seconds
            this.lastWGCheckTicks = 0;
            WorldGuardManager.updateWorlguardOptionsForTrackedSpawners(main, this.activeSpawners);
        }
        else if (hasWorldGuard)
            this.lastWGCheckTicks += ticksPerCall;

        final Set<BasicLocation> spawnersToCheck = getSpawnersWithinPlayerActivationRange();
        Utils.logger.info("spawners to check: " + spawnersToCheck.size());
        for (final BasicLocation location : spawnersToCheck) {
            final SpawnerInfo info = activeSpawners.get(location);
            if (info == null) continue;

            if (shouldSpawnerSpawnNow(info)) {
                Utils.logger.info("should spawn now");
                final SpawnerOptions opts = info.options != null ?
                        info.options : options;
                spawnEntities(info, opts);

                info.resetTimeLeft(options);
            }
        }
    }

    @NotNull
    private Set<BasicLocation> getSpawnersWithinPlayerActivationRange(){
        final Set<BasicLocation> spawners = new HashSet<>();

        for (final Player player : Bukkit.getOnlinePlayers()){
            if (!updateProcessor.worldMappings.containsKey(player.getWorld().getName()))
                continue;

            for (final BasicLocation basicLocation : updateProcessor.worldMappings.get(player.getWorld().getName())){
                if (spawners.contains(basicLocation))
                    continue;

                final SpawnerInfo info = activeSpawners.get(basicLocation);
                if (info == null || !info.isChunkLoaded || info.options == null)
                    continue;

                final long distance = (long) Math.ceil(info.getCs().getLocation().distanceSquared(player.getLocation()));
                if (distance <= info.options.playerRequiredRange)
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

    private boolean shouldSpawnerSpawnNow(final @NotNull SpawnerInfo info) {
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

        for (int i = 0; i < info.options.spawnCount; i++) {
            final Location spawnLocation = getSpawnLocation(cs.getLocation());
            if (spawnLocation == null) {
                Utils.logger.info("spawn location was null: " + Utils.showSpawnerLocation(cs));
                continue;
            }

            Utils.logger.info(String.format("spawning %s at %s, %s, %s" ,
                    cs.getSpawnedType().name(), spawnLocation.getBlockX(), spawnLocation.getBlockY(), spawnLocation.getBlockZ()));

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

    public int getActiveSpawnersCount(){
        return this.activeSpawners.size();
    }
}
