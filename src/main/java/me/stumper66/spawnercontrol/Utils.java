package me.stumper66.spawnercontrol;

import me.lokka30.microlib.messaging.MessageUtils;
import me.lokka30.microlib.messaging.MicroLogger;
import org.apache.commons.lang.WordUtils;
import org.bukkit.block.CreatureSpawner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
}
