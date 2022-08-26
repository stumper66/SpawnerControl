package me.stumper66.spawnercontrol;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

public class FileLoader {

    @NotNull static YamlConfiguration loadConfig(final @NotNull SpawnerControl main){
        final File file = new File(main.getDataFolder(), "config.yml");

        if (!file.exists())
            main.saveResource(file.getName(), false);

        final YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        cfg.options().copyDefaults(true);

        return cfg;
    }

    static void parseConfigFile(final @NotNull SpawnerControl main, final @NotNull YamlConfiguration settings){
        final SpawnerOptions defaults = new SpawnerOptions();
        main.spawnerProcessor.hasSpawnerGroupIds = false;

        final SpawnerOptions spawnerOptions = parseSpawnerOptions(settings, defaults);
        spawnerOptions.isDefaultOptions = true;
        if (defaults.spawnGroupId != null && defaults.spawnGroupId.length() > 0)
            main.spawnerProcessor.hasSpawnerGroupIds = true;
        if (spawnerOptions.spawnGroupId != null && spawnerOptions.spawnGroupId.length() > 0)
            main.spawnerProcessor.hasSpawnerGroupIds = true;

        main.wgRegionOptions = parseConfigRegions(settings.get("worldguard-regions"), spawnerOptions, true);
        main.namedSpawnerOptions = parseConfigRegions(settings.get("named-spawners"), spawnerOptions, false);
        main.spawnerOptions = spawnerOptions;
    }

    private @NotNull static SpawnerOptions parseSpawnerOptions(final @NotNull ConfigurationSection cs, final @NotNull SpawnerOptions defaults){
        final SpawnerOptions spawnerOptions = new SpawnerOptions();
        final CachedModalList<String> parsedWorldList = buildCachedModalListOfString(cs);
        if (parsedWorldList != null)
            spawnerOptions.allowedWorlds = parsedWorldList;

        final CachedModalList<EntityType> allowedEntities = buildCachedModalListOfEntityType(cs);
        if (allowedEntities != null) spawnerOptions.allowedEntityTypes = allowedEntities;
        spawnerOptions.maxNearbyEntities = cs.getInt("max-nearby-entities", defaults.maxNearbyEntities);
        spawnerOptions.spawnRange = cs.getInt("spawn-range", defaults.spawnRange);
        spawnerOptions.minSpawnDelay = cs.getInt("min-spawn-delay", defaults.minSpawnDelay);
        spawnerOptions.maxSpawnDelay = cs.getInt("max-spawn-delay", defaults.maxSpawnDelay);
        spawnerOptions.setPlayerRequiredRange(cs.getDouble("player-required-range", defaults.getPlayerRequiredRange()));
        spawnerOptions.delay = cs.getInt("spawner-delay", defaults.delay);
        spawnerOptions.allowAirSpawning = cs.getBoolean("allow-air-spawning");
        spawnerOptions.doImmediateSpawn = cs.getBoolean("immediate-spawn");
        spawnerOptions.immediateSpawnResetPeriod = cs.getInt("immediate-spawn-reset-period");
        spawnerOptions.commandToRun = cs.getString("command-to-run");
        if (spawnerOptions.commandToRun != null && spawnerOptions.commandToRun.length() == 0)
            spawnerOptions.commandToRun = null;
        spawnerOptions.doMobSpawn = cs.getBoolean("also-spawn-mob", defaults.doMobSpawn);
        spawnerOptions.doSpawnerParticles = cs.getBoolean("create-particles-on-spawner", defaults.doSpawnerParticles);
        spawnerOptions.nbtData = cs.getString("nbt-data", defaults.nbtData);
        spawnerOptions.spawnGroupId = cs.getString("spawn-group-id", defaults.spawnGroupId);

        Integer[] numberRange = getAmountRangeFromString(cs.getString("spawn-count"));
        if (numberRange != null) {
            spawnerOptions.spawnCount_Min = numberRange[0];
            spawnerOptions.spawnCount_Max = numberRange[1];
        }

        numberRange = getAmountRangeFromString(cs.getString("allowed-light-levels"));
        if (numberRange != null) {
            spawnerOptions.allowedLightLevel_Min = numberRange[0];
            spawnerOptions.allowedLightLevel_Max = numberRange[1];
        }

        numberRange = getAmountRangeFromString(cs.getString("allowed-skylight-levels"));
        if (numberRange != null) {
            spawnerOptions.allowedSkyLightLevel_Min = numberRange[0];
            spawnerOptions.allowedSkyLightLevel_Max = numberRange[1];
        }

        numberRange = getAmountRangeFromString(cs.getString("allowed-block-light-levels"));
        if (numberRange != null) {
            spawnerOptions.allowedBlockLightLevel_Min = numberRange[0];
            spawnerOptions.allowedBlockLightLevel_Max = numberRange[1];
        }

        if (cs.getInt("slime-size-min", 0) > 0)
            spawnerOptions.slimeSizeMin = cs.getInt("slime-size-min");
        if (cs.getInt("slime-size-max", 0) > 0)
            spawnerOptions.slimeSizeMax = cs.getInt("slime-size-max");

        return spawnerOptions;
    }

    private @Nullable static Integer[] getAmountRangeFromString(final String numberOrNumberRange){
        if (numberOrNumberRange == null || numberOrNumberRange.isEmpty()) return null;

        if (!numberOrNumberRange.contains("-")){
            if (!Utils.isInteger(numberOrNumberRange)) {
                Utils.logger.warning("Invalid number: " + numberOrNumberRange);
                return null;
            }

            return new Integer[]{ Integer.parseInt(numberOrNumberRange), Integer.parseInt(numberOrNumberRange) };
        }

        final String[] nums = numberOrNumberRange.split("-");
        if (nums.length != 2) {
            Utils.logger.warning("Invalid number range: " + numberOrNumberRange);
            return null;
        }

        if (!Utils.isInteger(nums[0].trim()) || !Utils.isInteger(nums[1].trim())) {
            Utils.logger.warning("Invalid number range: " + numberOrNumberRange);
            return null;
        }

        return new Integer[]{ Integer.parseInt(nums[0].trim()), Integer.parseInt(nums[1].trim()) };
    }

    private @Nullable static Map<String, SpawnerOptions> parseConfigRegions(final @Nullable Object configRegion, final @NotNull SpawnerOptions defaults, final boolean isWG){
        if (configRegion == null) return null;

        final Map<String, SpawnerOptions> regionOptions = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        //noinspection unchecked
        for (final LinkedHashMap<String, Object> hashMap : (List<LinkedHashMap<String, Object>>) (configRegion)) {
            final ConfigurationSection cs = objTo_CS(hashMap);
            if (cs == null) return null;

            String keyName = null;
            for (final String hashKey : hashMap.keySet()){
                keyName = hashKey;
                break;
            }

            if (keyName == null) continue;
            final SpawnerOptions opts = parseSpawnerOptions(cs, defaults);
            if (!isWG)
                opts.customNameMatch = keyName;

            regionOptions.put(keyName, opts);
        }

        return regionOptions;
    }

    private @Nullable static CachedModalList<String> buildCachedModalListOfString(final ConfigurationSection cs){
        if (cs == null) return null;

        final CachedModalList<String> cachedModalList = new CachedModalList<>(new TreeSet<>(String.CASE_INSENSITIVE_ORDER), new TreeSet<>(String.CASE_INSENSITIVE_ORDER));
        final Object simpleStringOrArray = cs.get("allowed-worlds");
        ConfigurationSection cs2 = null;
        List<String> useList = null;

        if (simpleStringOrArray instanceof ArrayList)
            //noinspection unchecked
            useList = new LinkedList<>((ArrayList<String>) simpleStringOrArray);
        else if (simpleStringOrArray instanceof String)
            useList = List.of((String) simpleStringOrArray);

        if (useList == null)
            cs2 = objTo_CS(cs.get("allowed-worlds"));

        if (cs2 != null)
            useList = cs2.getStringList("allowed-list");

        if (cs2 == null)
            return null;

        for (final String item : useList) {
            if (item.trim().isEmpty()) continue;
            if ("*".equals(item.trim())){
                cachedModalList.allowAll = true;
                continue;
            }
            cachedModalList.allowedList.add(item);
        }

        for (final String item : cs2.getStringList("excluded-list")) {
            if (item.trim().isEmpty()) continue;
            if ("*".equals(item.trim())){
                cachedModalList.excludeAll = true;
                continue;
            }
            cachedModalList.excludedList.add(item);
        }

        if (cachedModalList.isEmpty() && !cachedModalList.allowAll && !cachedModalList.excludeAll)
            return null;

        return cachedModalList;
    }

    private @Nullable static CachedModalList<EntityType> buildCachedModalListOfEntityType(final ConfigurationSection csParent) {
        if (csParent == null) return null;
        final ConfigurationSection cs = objTo_CS(csParent.get("allowed-entity-types"));
        if (cs == null){
            Utils.logger.info("cs was null");
            return null;
        }

        final CachedModalList<EntityType> cachedModalList = new CachedModalList<>();
        final Object simpleStringOrArray = cs.get("allowed-entity-types");
        ConfigurationSection cs2 = null;
        List<String> useList = null;

        if (simpleStringOrArray instanceof ArrayList)
            //noinspection unchecked
            useList = new LinkedList<>((ArrayList<String>) simpleStringOrArray);
        else if (simpleStringOrArray instanceof String)
            useList = List.of((String) simpleStringOrArray);

        if (useList == null)
            cs2 = cs;

        if (cs2 != null)
            useList = cs2.getStringList("allowed-list");

        for (final String item : useList){
            if (item.trim().isEmpty()) continue;
            if ("*".equals(item.trim())){
                cachedModalList.allowAll = true;
                continue;
            }
            try {
                final EntityType type = EntityType.valueOf(item.trim().toUpperCase());
                cachedModalList.allowedList.add(type);
            } catch (final IllegalArgumentException ignored) {
                Utils.logger.warning("Invalid entity type: " + item);
            }
        }
        if (cs2 == null) return cachedModalList;

        for (final String item : cs2.getStringList("excluded-list")) {
            if (item.trim().isEmpty()) continue;
            if ("*".equals(item.trim())){
                cachedModalList.excludeAll = true;
                continue;
            }
            try {
                final EntityType type = EntityType.valueOf(item.trim().toUpperCase());
                cachedModalList.excludedList.add(type);
            } catch (final IllegalArgumentException ignored) {
                Utils.logger.warning("Invalid entity type: " + item);
            }
        }

        if (cachedModalList.isEmpty() && !cachedModalList.allowAll && !cachedModalList.excludeAll)
            return null;

        return cachedModalList;
    }

    private @Nullable static ConfigurationSection objTo_CS(final Object object){
        if (object == null) return null;

        if (object instanceof ConfigurationSection) {
            return (ConfigurationSection) object;
        } else if (object instanceof Map) {
            final MemoryConfiguration result = new MemoryConfiguration();
            //noinspection unchecked
            result.addDefaults((Map<String, Object>) object);
            return result.getDefaultSection();
        } else {
            Utils.logger.warning("couldn't parse Config of type: " + object.getClass().getSimpleName() + ", value: " + object);
            return null;
        }
    }
}
