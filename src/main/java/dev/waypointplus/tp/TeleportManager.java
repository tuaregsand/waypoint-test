package dev.waypointplus.tp;

import dev.waypointplus.WaypointPlus;
import dev.waypointplus.eco.EconomyHook;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class TeleportManager implements Listener {

    private final WaypointPlus plugin;
    private final EconomyHook eco;

    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Warmup> warmups = new ConcurrentHashMap<>();
    private final Set<UUID> suppressBackCapture = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public TeleportManager(WaypointPlus plugin, EconomyHook eco) {
        this.plugin = plugin;
        this.eco = eco;
    }

    public boolean isOnCooldown(Player p, String actionKey) {
        long now = System.currentTimeMillis();
        long cd = getConfigSec(actionKey).getLong("cooldown", 0);
        long last = cooldowns.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>())
                .getOrDefault(actionKey, 0L);
        return cd > 0 && (now - last) < cd * 1000L;
    }

    public long cooldownRemaining(Player p, String actionKey) {
        long now = System.currentTimeMillis();
        long cd = getConfigSec(actionKey).getLong("cooldown", 0);
        long last = cooldowns.getOrDefault(p.getUniqueId(), Collections.emptyMap())
                .getOrDefault(actionKey, 0L);
        long left = (cd * 1000L) - (now - last);
        return Math.max(0, Math.round(left / 1000.0));
    }

    private ConfigurationSection getConfigSec(String key) {
        return plugin.getConfig().getConfigurationSection("teleport." + key);
    }

    public void teleport(Player p, Location to, String actionKey, Runnable after) {
        // cooldown
        if (isOnCooldown(p, actionKey)) {
            long left = cooldownRemaining(p, actionKey);
            send(p, "cooldown", Map.of("seconds", String.valueOf(left), "action", actionKey));
            return;
        }

        // charge preview
        double cost = getConfigSec(actionKey).getDouble("cost", 0.0);
        if (cost > 0 && eco.isEnabled() && eco.provider().isPresent()) {
            if (eco.provider().get().getBalance(p) < cost) {
                send(p, "insufficient-funds", Map.of("amount", eco.format(cost, plugin.getConfig().getString("economy.format", "$%.2f"))));
                return;
            }
        }

        int warm = getConfigSec(actionKey).getInt("warmup", 0);
        boolean cancelOnMove = getConfigSec(actionKey).getBoolean("cancel-on-move", true);
        if (warm > 0) {
            // start warmup
            send(p, "warmup-start", Map.of("seconds", String.valueOf(warm)));
            Warmup w = new Warmup(p.getUniqueId(), p.getLocation().toVector(), cancelOnMove);
            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                warmups.remove(p.getUniqueId());
                doTeleport(p, to, actionKey, cost, after);
            }, warm * 20L);
            w.task = task;
            warmups.put(p.getUniqueId(), w);
        } else {
            doTeleport(p, to, actionKey, cost, after);
        }
    }

    private void doTeleport(Player p, Location to, String actionKey, double cost, Runnable after) {
        // charge
        if (cost > 0 && eco.isEnabled()) {
            boolean ok = eco.charge(p, cost);
            if (!ok) {
                send(p, "insufficient-funds", Map.of("amount", eco.format(cost, plugin.getConfig().getString("economy.format", "$%.2f"))));
                return;
            }
            send(p, "charged", Map.of("amount", eco.format(cost, plugin.getConfig().getString("economy.format", "$%.2f"))));
        }

        // mark cooldown
        cooldowns.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>()).put(actionKey, System.currentTimeMillis());

        // capture back if configured
        if (plugin.getConfig().getBoolean("back.save-on-teleport")) {
            plugin.back().setBack(p.getUniqueId(), p.getLocation());
        }

        suppressBackCapture.add(p.getUniqueId());
        p.teleportAsync(to).thenRun(() -> {
            suppressBackCapture.remove(p.getUniqueId());
            if (after != null) after.run();
        });
    }

    private void send(Player p, String key, Map<String, String> vars) {
        String prefix = plugin.messages().getString("prefix", "");
        String msg = plugin.messages().getString(key, key);
        for (Map.Entry<String, String> e : vars.entrySet()) {
            msg = msg.replace("{" + e.getKey() + "}", e.getValue());
        }
        p.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', prefix + msg));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Warmup w = warmups.get(e.getPlayer().getUniqueId());
        if (w == null || !w.cancelOnMove) return;
        if (e.getFrom().getBlockX() != e.getTo().getBlockX()
                || e.getFrom().getBlockY() != e.getTo().getBlockY()
                || e.getFrom().getBlockZ() != e.getTo().getBlockZ()) {
            // cancel
            w.task.cancel();
            warmups.remove(e.getPlayer().getUniqueId());
            String prefix = plugin.messages().getString("prefix", "");
            String msg = plugin.messages().getString("warmup-cancelled", "warmup-cancelled");
            e.getPlayer().sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', prefix + msg));
        }
    }

    public boolean isSuppressingBackCapture(UUID uuid) {
        return suppressBackCapture.contains(uuid);
    }

    private static final class Warmup {
        final UUID player;
        final org.bukkit.util.Vector start;
        final boolean cancelOnMove;
        BukkitTask task;

        Warmup(UUID player, org.bukkit.util.Vector start, boolean cancelOnMove) {
            this.player = player;
            this.start = start;
            this.cancelOnMove = cancelOnMove;
        }
    }
}

