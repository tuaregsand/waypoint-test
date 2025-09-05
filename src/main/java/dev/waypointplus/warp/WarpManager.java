package dev.waypointplus.warp;

import dev.waypointplus.WaypointPlus;
import dev.waypointplus.store.LocationIO;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class WarpManager {
    private final WaypointPlus plugin;
    private final File file;
    private final YamlConfiguration yaml;

    public WarpManager(WaypointPlus plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "warps.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    public Map<String, Location> all() {
        Map<String, Location> out = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (yaml.isConfigurationSection("warps")) {
            for (String k : yaml.getConfigurationSection("warps").getKeys(false)) {
                Location loc = LocationIO.read(yaml.getConfigurationSection("warps." + k));
                if (loc != null) out.put(k, loc);
            }
        }
        return out;
    }

    public boolean set(String name, Location loc) {
        String path = "warps." + name;
        if (yaml.getConfigurationSection(path) == null) yaml.createSection(path);
        LocationIO.write(yaml.getConfigurationSection(path), loc);
        return save();
    }

    public boolean delete(String name) {
        if (!all().containsKey(name)) return false;
        yaml.set("warps." + name, null);
        return save();
    }

    public Optional<Location> get(String name) { return Optional.ofNullable(all().get(name)); }

    public boolean save() {
        try { yaml.save(file); return true; } catch (IOException e) { plugin.getLogger().warning("Failed saving warps.yml: " + e.getMessage()); return false; }
    }

    public void flush() { save(); }
}

