package me.stumper66.spawnercontrol;

import me.lokka30.microlib.messaging.MessageUtils;
import me.lokka30.microlib.messaging.MicroLogger;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;

@SuppressWarnings("unused")
public class Utils {
    @NotNull
    public static final MicroLogger logger = new MicroLogger("&b&lSpawnerControl:&7 ");

    public static double round(final double value) {
        return Math.round(value * 100) / 100.00;
    }

    public static double round(final double value, final int digits) {
        final double scale = Math.pow(10, digits);
        return Math.round(value * scale) / scale;
    }

    public static String showSpawnerLocation(final @NotNull CreatureSpawner cs){
        return MessageUtils.colorizeAll(String.format("&8[&b%s &7@ &b%s&7, &b%s&7, &b%s&7 in '&b%s&7'&8]&7",
                WordUtils.capitalizeFully(cs.getSpawnedType().toString()),
                cs.getLocation().getBlockX(),
                cs.getLocation().getBlockY(),
                cs.getLocation().getBlockZ(),
                cs.getLocation().getWorld().getName()
        ));
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isInteger(@Nullable final String str) {
        if (str == null || str.isEmpty()) return false;

        try {
            Integer.parseInt(str);
            return true;
        } catch (final NumberFormatException ex) {
            return false;
        }
    }

    @NotNull
    public static Collection<Chunk> getChunksAroundPlayer(final @NotNull Player player) {
        // https://www.spigotmc.org/threads/get-chunks-around-players-chunk.73189/
        final int[] offset = { -1, 0, 1 };

        final World world = player.getWorld();
        final int baseX = player.getLocation().getChunk().getX();
        final int baseZ = player.getLocation().getChunk().getZ();

        final Collection<Chunk> chunksAroundPlayer = new HashSet<>();
        for (final int x : offset) {
            for (final int z : offset) {
                final Chunk chunk = world.getChunkAt(baseX + x, baseZ + z);
                chunksAroundPlayer.add(chunk);
            }
        }

        return chunksAroundPlayer;
    }
}
