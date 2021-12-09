package me.stumper66.spawnercontrol;

import me.lokka30.microlib.messaging.MicroLogger;
import org.bukkit.block.CreatureSpawner;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class Utils {
    @NotNull
    public static final MicroLogger logger = new MicroLogger("&b&lSpawnerControl: &7");

    public static double round(final double value) {
        return Math.round(value * 100) / 100.00;
    }

    public static double round(final double value, final int digits) {
        final double scale = Math.pow(10, digits);
        return Math.round(value * scale) / scale;
    }

    public static String showSpawnerLocation(final @NotNull CreatureSpawner cs){
        return String.format("%s, %s, %s, %s, %s",
                cs.getSpawnedType(),
                cs.getLocation().getWorld().getName(),
                cs.getLocation().getBlockX(),
                cs.getLocation().getBlockY(),
                cs.getLocation().getBlockZ()
        );
    }
}
