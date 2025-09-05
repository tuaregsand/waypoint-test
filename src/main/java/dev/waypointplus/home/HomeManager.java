package dev.waypointplus.home;

import dev.waypointplus.WaypointPlus;
import dev.waypointplus.store.LocationIO;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class HomeManager {
    private final WaypointPlus plugin;

    public HomeManager(WaypointPlus plugin) { this.plugin = plugin; }

    private File fileFor(UUID uuid) {
        return new File(plugin.getDataFolder(), "data/homes/" + uuid + ".yml");
    }

    private YamlConfiguration yamlFor(UUID uuid) {
        return YamlConfiguration.loadConfiguration(fileFor(uuid));
    }

    public Map<String, Location> list(UUID uuid) {
        Map<String, Location> out = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        YamlConfiguration y = yamlFor(uuid);
        if (y.isConfigurationSection("homes")) {
            for (String name : y.getConfigurationSection("homes").getKeys(false)) {
                Location loc = LocationIO.read(y.getConfigurationSection("homes." + name));
                if (loc != null) out.put(name, loc);
            }
        }
        return out;
    }

    public boolean set(UUID uuid, String name, Location loc, int limit) {
        Map<String, Location> all = list(uuid);
        if (!all.containsKey(name) && all.size() >= limit) return false;
        YamlConfiguration y = yamlFor(uuid);
        String path = "homes." + name;
        if (y.getConfigurationSection(path) == null) y.createSection(path);
        LocationIO.write(y.getConfigurationSection(path), loc);
        try { y.save(fileFor(uuid)); } catch (IOException ignored) {}
        return true;
    }

    public boolean delete(UUID uuid, String name) {
        YamlConfiguration y = yamlFor(uuid);
        if (!y.isConfigurationSection("homes") || !y.getConfigurationSection("homes").contains(name)) return false;
        y.set("homes." + name, null);
        try { y.save(fileFor(uuid)); } catch (IOException ignored) {}
        return true;
    }

    public Optional<Location> get(UUID uuid, String name) {
        return Optional.ofNullable(list(uuid).get(name));
    }

    public void flush() { /* per-file saves already handled */ }

    public int limitFor(org.bukkit.entity.Player p) {
        int def = plugin.getConfig().getInt("homes.default-limit", 1);
        // permission override waypointplus.homes.<n>
        int best = def;
        for (org.bukkit.permissions.PermissionAttachmentInfo pai : p.getEffectivePermissions()) {
            String perm = pai.getPermission().toLowerCase(Locale.ROOT);
            if (perm.startsWith("waypointplus.homes.")) {
                try {
                    int n = Integer.parseInt(perm.substring("waypointplus.homes.".length()));
                    if (n > best) best = n;
                } catch (NumberFormatException ignored) {}
            }
        }
        return best;
    }
}

