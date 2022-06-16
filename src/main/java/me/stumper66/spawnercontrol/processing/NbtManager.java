package me.stumper66.spawnercontrol.processing;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.stumper66.spawnercontrol.DebugInfo;
import me.stumper66.spawnercontrol.DebugType;
import me.stumper66.spawnercontrol.Utils;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class NbtManager {
    public static void applyNBT_Data_Mob(@NotNull final LivingEntity livingEntity, @NotNull final String nbtString, final @NotNull DebugInfo debugInfo) {
        String jsonBefore = null;
        String jsonAfter = null;

        try {
            final de.tr7zw.nbtapi.NBTEntity nbtent = new de.tr7zw.nbtapi.NBTEntity(livingEntity);
            jsonBefore = nbtent.toString();
            nbtent.mergeCompound(new de.tr7zw.nbtapi.NBTContainer(nbtString));
            jsonAfter = nbtent.toString();
        } catch (final Exception e) {
            Utils.logger.warning("Error applying NBT data: " + e.getMessage());
            return;
        }

        if (debugInfo.doesSpawnerMeetDebugCriteria(DebugType.SPAWN_ATTEMPT_SUCCESS)){
            final String whatChanged = showChangedJson(jsonBefore, jsonAfter);
            if (whatChanged == null)
                Utils.logger.info(livingEntity.getType() + ": no nbt changes");
            else
                Utils.logger.info(livingEntity.getType() + ": nbt changes: " + whatChanged);
        }
    }

    private static @Nullable String showChangedJson(final String jsonBefore, final String jsonAfter) {
        final Map<String, String> objectsBefore = new TreeMap<>();
        final Map<String, String> objectsAfter = new TreeMap<>();
        final JsonObject jsonObjectBefore = JsonParser.parseString(jsonBefore).getAsJsonObject();
        final JsonObject jsonObjectAfter = JsonParser.parseString(jsonAfter).getAsJsonObject();

        try {
            for (final String key : jsonObjectBefore.keySet()) {
                objectsBefore.put(key, jsonObjectBefore.get(key).toString());
            }
            for (final String key : jsonObjectAfter.keySet()) {
                objectsAfter.put(key, jsonObjectAfter.get(key).toString());
            }
        } catch (final Exception e) {
            e.printStackTrace();
            return null;
        }

        final List<String> objectsUpdated = new LinkedList<>();
        final List<String> objectsAdded = new LinkedList<>();
        final List<String> objectsRemoved = new LinkedList<>();

        for (final String key : jsonObjectAfter.keySet()) {
            final String value = jsonObjectAfter.get(key).toString();

            if (objectsBefore.containsKey(key) && objectsAfter.containsKey(key) && !objectsBefore.get(key).equals(value)) {
                objectsUpdated.add(key + ":" + value);
            } else if (!objectsBefore.containsKey(key) && objectsAfter.containsKey(key)) {
                objectsAdded.add(key + ":" + value);
            } else if (objectsBefore.containsKey(key) && !objectsAfter.containsKey(key)) {
                objectsRemoved.add(key + ":" + value);
            }
        }

        final StringBuilder sb = new StringBuilder();
        if (!objectsAdded.isEmpty()){
            sb.append("added: ");
            sb.append(String.join(",", objectsAdded));
        }

        if (!objectsUpdated.isEmpty()){
            if (sb.length() > 0) sb.append(System.lineSeparator());
            sb.append("updated: ");
            sb.append(String.join(",", objectsUpdated));
        }

        if (!objectsRemoved.isEmpty()){
            if (sb.length() > 0) sb.append(System.lineSeparator());
            sb.append("removed: ");
            sb.append(String.join(",", objectsRemoved));
        }

        if (sb.length() > 0)
            return sb.toString();
        else
            return null;
    }
}
