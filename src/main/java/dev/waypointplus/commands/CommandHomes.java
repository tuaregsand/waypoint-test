package dev.waypointplus.commands;

import dev.waypointplus.WaypointPlus;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public final class CommandHomes implements CommandExecutor, TabCompleter {

    public enum Mode { HOME, SETHOME, DELHOME, LISTHOMES }

    private final WaypointPlus plugin;
    private final Mode mode;

    public CommandHomes(WaypointPlus plugin, Mode mode) {
        this.plugin = plugin; this.mode = mode;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage(ChatColor.RED + "Players only."); return true; }
        switch (mode) {
            case HOME -> {
                String name = args.length >= 1 ? args[0] : "home";
                Optional<Location> loc = plugin.homes().get(p.getUniqueId(), name);
                if (loc.isEmpty()) {
                    msg(p, "home-missing", Map.of("home", name));
                    return true;
                }
                plugin.teleports().teleport(p, centered(loc.get()), "home", () -> msg(p, "home-teleport", Map.of("home", name)));
            }
            case SETHOME -> {
                String name = args.length >= 1 ? args[0] : "home";
                int limit = plugin.homes().limitFor(p);
                boolean ok = plugin.homes().set(p.getUniqueId(), name, p.getLocation(), limit);
                if (!ok) {
                    msg(p, "home-limit", Map.of("limit", String.valueOf(limit)));
                } else {
                    msg(p, "home-set", Map.of("home", name));
                }
            }
            case DELHOME -> {
                if (args.length < 1) { usage(sender, cmd); return true; }
                String name = args[0];
                boolean ok = plugin.homes().delete(p.getUniqueId(), name);
                msg(p, ok ? "home-deleted" : "home-missing", Map.of("home", name));
            }
            case LISTHOMES -> {
                Map<String, Location> all = plugin.homes().list(p.getUniqueId());
                String list = all.isEmpty() ? "-" : String.join(", ", new TreeSet<>(all.keySet()));
                msg(p, "homes-list", Map.of("list", list));
            }
        }
        return true;
    }

    private Location centered(Location l) { return l.clone().add(0.5, 0, 0.5); }

    private void usage(CommandSender s, Command cmd) {
        String prefix = plugin.messages().getString("prefix", "");
        String msg = plugin.messages().getString("invalid-args", "{usage}")
                .replace("{usage}", cmd.getUsage());
        s.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + msg));
    }

    private void msg(Player p, String key, Map<String, String> vars) {
        String prefix = plugin.messages().getString("prefix", "");
        String msg = plugin.messages().getString(key, key);
        for (Map.Entry<String, String> e : vars.entrySet()) msg = msg.replace("{" + e.getKey() + "}", e.getValue());
        p.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + msg));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!(sender instanceof Player p)) return List.of();
        if (mode == Mode.HOME || mode == Mode.DELHOME) {
            if (args.length == 1) {
                Set<String> names = plugin.homes().list(p.getUniqueId()).keySet();
                return names.stream().filter(n -> n.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT))).sorted().collect(Collectors.toList());
            }
        }
        return List.of();
    }
}

