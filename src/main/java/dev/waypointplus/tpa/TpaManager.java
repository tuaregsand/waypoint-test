package dev.waypointplus.tpa;

import dev.waypointplus.WaypointPlus;
import dev.waypointplus.tp.TeleportManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.Duration;
import java.util.*;

public final class TpaManager implements Listener {

    public record Request(UUID from, UUID to, long expiresAtMillis) {}

    private final WaypointPlus plugin;
    private final TeleportManager tp;
    private final Map<UUID, Request> byTarget = new HashMap<>();
    private final Map<UUID, Request> bySender = new HashMap<>();
    private final long expirySeconds = 60;

    public TpaManager(WaypointPlus plugin, TeleportManager tp) {
        this.plugin = plugin;
        this.tp = tp;
    }

    public void send(Player from, Player to) {
        // Override previous outgoing
        cancel(from.getUniqueId(), false);
        long exp = System.currentTimeMillis() + Duration.ofSeconds(expirySeconds).toMillis();
        Request req = new Request(from.getUniqueId(), to.getUniqueId(), exp);
        byTarget.put(to.getUniqueId(), req);
        bySender.put(from.getUniqueId(), req);
        String pf = plugin.messages().getString("prefix", "");
        String m1 = plugin.messages().getString("tpa-sent", "").replace("{player}", to.getName()).replace("{seconds}", String.valueOf(expirySeconds));
        from.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', pf + m1));
        String m2 = plugin.messages().getString("tpa-received", "").replace("{player}", from.getName());
        to.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', pf + m2));

        // Expiry task
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Request r = byTarget.get(to.getUniqueId());
            if (r != null && r.expiresAtMillis <= System.currentTimeMillis()) {
                cancel(from.getUniqueId(), true);
            }
        }, expirySeconds * 20L);
    }

    public boolean accept(Player target) {
        Request r = byTarget.remove(target.getUniqueId());
        if (r == null) return false;
        bySender.remove(r.from);

        Player from = Bukkit.getPlayer(r.from);
        if (from == null || !from.isOnline()) return false;

        Location dest = target.getLocation();
        tp.teleport(from, dest, "tpa", () -> {
            String pf = plugin.messages().getString("prefix", "");
            from.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', pf + plugin.messages().getString("tpa-accepted", "")));
            target.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', pf + plugin.messages().getString("tpa-accepted", "")));
        });
        return true;
    }

    public boolean deny(Player target) {
        Request r = byTarget.remove(target.getUniqueId());
        if (r == null) return false;
        bySender.remove(r.from);
        Player from = Bukkit.getPlayer(r.from);
        String pf = plugin.messages().getString("prefix", "");
        if (from != null) from.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', pf + plugin.messages().getString("tpa-denied", "")));
        target.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', pf + plugin.messages().getString("tpa-denied", "")));
        return true;
    }

    public boolean cancel(UUID sender, boolean silent) {
        Request r = bySender.remove(sender);
        if (r == null) return false;
        byTarget.remove(r.to);
        if (!silent) {
            Player s = Bukkit.getPlayer(sender);
            if (s != null) {
                String pf = plugin.messages().getString("prefix", "");
                s.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', pf + plugin.messages().getString("tpa-cancelled", "")));
            }
        }
        return true;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        cancel(e.getPlayer().getUniqueId(), true);
        byTarget.remove(e.getPlayer().getUniqueId());
    }
}

