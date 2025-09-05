package dev.waypointplus.spawn;

import dev.waypointplus.WaypointPlus;
import dev.waypointplus.store.LocationIO;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public final class SpawnManager {
    private final File file;
    private final YamlConfiguration yaml;
    private final WaypointPlus plugin;

    public SpawnManager(WaypointPlus plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "spawns.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    public void set(Location loc) {
        if (yaml.getConfigurationSection("spawn") == null) yaml.createSection("spawn");
        LocationIO.write(yaml.getConfigurationSection("spawn"), loc);
        save();
    }

    public Location get() {
        Location l = LocationIO.read(yaml.getConfigurationSection("spawn"));
        if (l != null) return l;
        // fallback: default world spawn
        World w = Bukkit.getWorlds().get(0);
        return w.getSpawnLocation().clone().add(0.5, 0, 0.5);
    }

    private void save() {
        try { yaml.save(file); } catch (IOException e) { plugin.getLogger().warning("Failed saving spawns.yml: " + e.getMessage()); }
    }
}

