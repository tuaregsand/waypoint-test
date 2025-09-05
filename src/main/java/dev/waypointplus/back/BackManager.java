package dev.waypointplus.back;

import dev.waypointplus.WaypointPlus;
import dev.waypointplus.store.LocationIO;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class BackManager implements Listener {

    private final WaypointPlus plugin;
    private final Map<UUID, Location> backs = new HashMap<>();
    private final File file;
    private final YamlConfiguration yaml;

    public BackManager(WaypointPlus plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/playerdata.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
        loadAll();
    }

    public void setBack(UUID uuid, Location loc) {
        backs.put(uuid, loc.clone());
    }

    public Optional<Location> getBack(UUID uuid) {
        return Optional.ofNullable(backs.get(uuid));
    }

    private void loadAll() {
        if (yaml.isConfigurationSection("backs")) {
            for (String key : yaml.getConfigurationSection("backs").getKeys(false)) {
                Location loc = LocationIO.read(yaml.getConfigurationSection("backs." + key));
                if (loc != null) backs.put(UUID.fromString(key), loc);
            }
        }
    }

    public void saveAll() {
        yaml.set("backs", null);
        for (Map.Entry<UUID, Location> e : backs.entrySet()) {
            String path = "backs." + e.getKey();
            yaml.createSection(path);
            LocationIO.write(yaml.getConfigurationSection(path), e.getValue());
        }
        try { yaml.save(file); } catch (IOException ex) { plugin.getLogger().warning("Failed saving backs: " + ex.getMessage()); }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent e) {
        if (!plugin.getConfig().getBoolean("back.save-on-death")) return;
        Player p = e.getEntity();
        setBack(p.getUniqueId(), p.getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        if (!plugin.getConfig().getBoolean("back.save-on-teleport")) return;
        if (plugin.teleports().isSuppressingBackCapture(e.getPlayer().getUniqueId())) return;
        setBack(e.getPlayer().getUniqueId(), e.getFrom());
    }
}

