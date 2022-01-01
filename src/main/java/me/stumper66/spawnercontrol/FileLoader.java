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

    @NotNull
    static YamlConfiguration loadConfig(final @NotNull SpawnerControl main){
        final File file = new File(main.getDataFolder(), "config.yml");

        if (!file.exists())
            main.saveResource(file.getName(), false);

        final YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        cfg.options().copyDefaults(true);

        return cfg;
    }

    static void parseConfigFile(final @NotNull SpawnerControl main, final @NotNull YamlConfiguration settings){
        final SpawnerOptions defaults = new SpawnerOptions();

        final SpawnerOptions spawnerOptions = parseSpawnerOptions(settings, defaults);
        main.wgRegionOptions = parseConfigRegions(settings.get("worldguard-regions"), defaults, true);
        main.namedSpawnerOptions = parseConfigRegions(settings.get("named-spawners"), defaults, false);
        main.spawnerOptions = spawnerOptions;
    }

    @NotNull
    private static SpawnerOptions parseSpawnerOptions(final ConfigurationSection cs, final SpawnerOptions defaults){
        final SpawnerOptions spawnerOptions = new SpawnerOptions();
        final CachedModalList<String> parsedWorldList = buildCachedModalListOfString(cs);
        if (parsedWorldList != null)
            spawnerOptions.allowedWorlds = parsedWorldList;

        final CachedModalList<EntityType> allowedEntities = buildCachedModalListOfEntityType(cs);
        if (allowedEntities != null) spawnerOptions.allowedEntityTypes = allowedEntities;
        spawnerOptions.maxNearbyEntities = cs.getInt("max-nearby-entities", defaults.maxNearbyEntities);
        spawnerOptions.spawnRange = cs.getInt("spawn-range", defaults.spawnRange);
        spawnerOptions.spawnCount = cs.getInt("spawn-count", defaults.spawnCount);
        spawnerOptions.minSpawnDelay = cs.getInt("min-spawn-delay", defaults.minSpawnDelay);
        spawnerOptions.maxSpawnDelay = cs.getInt("max-spawn-delay", defaults.maxSpawnDelay);
        spawnerOptions.playerRequiredRange = cs.getInt("player-required-range", defaults.playerRequiredRange);
        spawnerOptions.delay = cs.getInt("spawner-delay", defaults.delay);
        spawnerOptions.allowAirSpawning = cs.getBoolean("allow-air-spawning");
        final String numberRange = cs.getString("allowed-light-levels");
        if (numberRange != null) setAmountRangeFromString(numberRange, spawnerOptions);

        if (cs.getInt("slime-size-min", 0) > 0)
            spawnerOptions.slimeSizeMin = cs.getInt("slime-size-min");
        if (cs.getInt("slime-size-max", 0) > 0)
            spawnerOptions.slimeSizeMax = cs.getInt("slime-size-max");

        return spawnerOptions;
    }

    private static void setAmountRangeFromString(final String numberOrNumberRange, final @NotNull SpawnerOptions spawnerOptions){
        if (numberOrNumberRange == null || numberOrNumberRange.isEmpty()) return;

        if (!numberOrNumberRange.contains("-")){
            if (!Utils.isInteger(numberOrNumberRange)) {
                Utils.logger.warning("Invalid number: " + numberOrNumberRange);
                return;
            }

            spawnerOptions.allowedLightLevel_Min = Integer.parseInt(numberOrNumberRange);
            spawnerOptions.allowedLightLevel_Max = spawnerOptions.allowedLightLevel_Min;
            return;
        }

        final String[] nums = numberOrNumberRange.split("-");
        if (nums.length != 2) {
            Utils.logger.warning("Invalid number range: " + numberOrNumberRange);
            return;
        }

        if (!Utils.isInteger(nums[0].trim()) || !Utils.isInteger(nums[1].trim())) {
            Utils.logger.warning("Invalid number range: " + numberOrNumberRange);
            return;
        }
        spawnerOptions.allowedLightLevel_Min = Integer.parseInt(nums[0].trim());
        spawnerOptions.allowedLightLevel_Max = Integer.parseInt(nums[1].trim());
    }

    @Nullable
    private static Map<String, SpawnerOptions> parseConfigRegions(final @Nullable Object configRegion, final @NotNull SpawnerOptions defaults, final boolean isWG){
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

    @Nullable
    private static CachedModalList<String> buildCachedModalListOfString(final ConfigurationSection cs){
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

    @Nullable
    private static CachedModalList<EntityType> buildCachedModalListOfEntityType(final ConfigurationSection csParent) {
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

    @Nullable
    private static ConfigurationSection objTo_CS(final Object object){
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
