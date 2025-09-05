package dev.waypointplus.commands;

import dev.waypointplus.WaypointPlus;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public final class CommandWarps implements CommandExecutor, TabCompleter {

    public enum Mode { WARP, SETWARP, DELWARP, LISTWARPS }

    private final WaypointPlus plugin;
    private final Mode mode;

    public CommandWarps(WaypointPlus plugin, Mode mode) { this.plugin = plugin; this.mode = mode; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        switch (mode) {
            case WARP -> {
                if (!(sender instanceof Player p)) { sender.sendMessage(ChatColor.RED + "Players only."); return true; }
                if (args.length < 1) { usage(sender, cmd); return true; }
                String name = args[0];
                Optional<Location> loc = plugin.warps().get(name);
                if (loc.isEmpty()) { msg(p, "warp-missing", Map.of("warp", name)); return true; }
                plugin.teleports().teleport(p, centered(loc.get()), "warp", () -> msg(p, "warp-teleport", Map.of("warp", name)));
            }
            case SETWARP -> {
                if (!(sender instanceof Player p)) { sender.sendMessage(ChatColor.RED + "Players only."); return true; }
                if (args.length < 1) { usage(sender, cmd); return true; }
                String name = args[0];
                plugin.warps().set(name, p.getLocation());
                msg(p, "warp-set", Map.of("warp", name));
            }
            case DELWARP -> {
                if (args.length < 1) { usage(sender, cmd); return true; }
                String name = args[0];
                boolean ok = plugin.warps().delete(name);
                String key = ok ? "warp-deleted" : "warp-missing";
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.messages().getString("prefix", "") + plugin.messages().getString(key, key).replace("{warp}", name)));
            }
            case LISTWARPS -> {
                String list = plugin.warps().all().keySet().stream().sorted().collect(Collectors.joining(", "));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.messages().getString("prefix", "") + plugin.messages().getString("warp-list", "").replace("{list}", list.isEmpty() ? "-" : list)));
            }
        }
        return true;
    }

    private Location centered(Location l) { return l.clone().add(0.5, 0, 0.5); }

    private void usage(CommandSender s, Command cmd) {
        String prefix = plugin.messages().getString("prefix", "");
        String msg = plugin.messages().getString("invalid-args", "{usage}").replace("{usage}", cmd.getUsage());
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
        if (mode == Mode.WARP || mode == Mode.DELWARP) {
            if (args.length == 1) {
                return plugin.warps().all().keySet().stream()
                        .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT)))
                        .sorted().collect(java.util.stream.Collectors.toList());
            }
        }
        return List.of();
    }
}

