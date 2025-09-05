package dev.waypointplus.commands;

import dev.waypointplus.WaypointPlus;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.generator.WorldInfo;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public final class CommandRtp implements CommandExecutor, TabCompleter {

    private final WaypointPlus plugin;

    public CommandRtp(WaypointPlus plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage(ChatColor.RED + "Players only."); return true; }
        World world = args.length >= 1 ? Bukkit.getWorld(args[0]) : p.getWorld();
        if (world == null) { sender.sendMessage(ChatColor.RED + "World not found."); return true; }

        p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.messages().getString("prefix", "") + plugin.messages().getString("rtp-start", "")));

        Location dest = findSafe(world, plugin.getConfig());
        if (dest == null) {
            p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.messages().getString("prefix", "") + plugin.messages().getString("rtp-failed", "")));
            return true;
        }
        plugin.teleports().teleport(p, dest, "rtp",
                () -> p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.messages().getString("prefix", "") + plugin.messages().getString("rtp-teleport", ""))));
        return true;
    }

    private Location findSafe(World world, org.bukkit.configuration.file.FileConfiguration cfg) {
        boolean useWB = cfg.getBoolean("rtp.use-worldborder", true);
        int radius = Math.max(1, cfg.getInt("rtp.radius", 5000));
        int attempts = Math.max(1, cfg.getInt("rtp.max-attempts", 24));
        boolean noLeaves = cfg.getBoolean("rtp.use-heightmap-no-leaves", true);
        int minY = cfg.getInt("rtp.min-y", -60);

        Set<Material> bad = new HashSet<>();
        for (String s : cfg.getStringList("rtp.safe-blocks-blacklist")) {
            try { bad.add(Material.valueOf(s)); } catch (IllegalArgumentException ignored) {}
        }

        final ThreadLocalRandom rnd = ThreadLocalRandom.current();

        for (int i = 0; i < attempts; i++) {
            double x, z;
            if (useWB) {
                WorldBorder wb = world.getWorldBorder();
                double half = wb.getSize() / 2.0 - 2.0;
                Location c = wb.getCenter();
                x = c.getX() + rnd.nextDouble(-half, half);
                z = c.getZ() + rnd.nextDouble(-half, half);
            } else {
                Location c = new Location(world, 0, 0, 0);
                x = c.getX() + rnd.nextDouble(-radius, radius);
                z = c.getZ() + rnd.nextDouble(-radius, radius);
            }

            Block top = world.getHighestBlockAt(new Location(world, x, 0, z),
                    noLeaves ? org.bukkit.HeightMap.MOTION_BLOCKING_NO_LEAVES : org.bukkit.HeightMap.MOTION_BLOCKING);

            if (top.getY() < minY) continue;

            Material m = top.getType();
            if (bad.contains(m)) continue;

            Location candidate = top.getLocation().add(0.5, 1, 0.5);
            // ensure inside border and air above
            if (useWB && !world.getWorldBorder().isInside(candidate)) continue;
            if (!candidate.clone().add(0, 1, 0).getBlock().isEmpty()) continue;
            if (!candidate.getBlock().isEmpty()) continue;
            return candidate;
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            String pref = args[0].toLowerCase(Locale.ROOT);
            return Bukkit.getWorlds().stream().map(World::getName)
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(pref))
                    .sorted().collect(Collectors.toList());
        }
        return List.of();
    }
}

