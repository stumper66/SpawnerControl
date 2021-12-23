package me.stumper66.spawnercontrol.processing;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class BasicLocation {
    public BasicLocation(final @NotNull Location location){
        this.location = location;
    }

    private final @NotNull Location location;

    public @NotNull Location getLocation(){
        return this.location;
    }

    public boolean isSameLocation(final @NotNull BasicLocation comparingBasicLocation){
        return isSameLocation(comparingBasicLocation.location);
    }

    public boolean isSameLocation(final @NotNull Location comparingLocation){
        return (
                this.location.getWorld() == comparingLocation.getWorld() &&
                        this.location.getBlockX() == comparingLocation.getBlockX() &&
                        this.location.getBlockY() == comparingLocation.getBlockY() &&
                        this.location.getBlockZ() == comparingLocation.getBlockZ()
                );
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null) return false;
        if (o == this) return true;
        if (!(o instanceof BasicLocation)) return false;

        return this.isSameLocation(((BasicLocation) o));
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.location.getWorld(), this.location.getBlockX(), this.location.getBlockY(), this.location.getBlockZ());
    }

    public String toString(){
        return String.format("%s, %s,%s,%s",
                this.location.getWorld().getName(), this.location.getBlockX(), this.location.getBlockY(), this.location.getBlockZ());
    }
}
