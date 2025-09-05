package dev.waypointplus.store;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public final class LocationIO {
    private LocationIO() {}

    public static void write(ConfigurationSection sec, Location loc) {
        sec.set("world", loc.getWorld().getName());
        sec.set("x", loc.getX());
        sec.set("y", loc.getY());
        sec.set("z", loc.getZ());
        sec.set("yaw", loc.getYaw());
        sec.set("pitch", loc.getPitch());
    }

    public static Location read(ConfigurationSection sec) {
        if (sec == null) return null;
        String worldName = sec.getString("world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        double x = sec.getDouble("x");
        double y = sec.getDouble("y");
        double z = sec.getDouble("z");
        float yaw = (float) sec.getDouble("yaw");
        float pitch = (float) sec.getDouble("pitch");
        return new Location(world, x, y, z, yaw, pitch);
    }
}

