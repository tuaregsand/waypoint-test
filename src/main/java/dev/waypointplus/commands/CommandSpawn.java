package dev.waypointplus.commands;

import dev.waypointplus.WaypointPlus;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public final class CommandSpawn implements CommandExecutor {

    public enum Mode { SPAWN, SETSPAWN }

    private final WaypointPlus plugin;
    private final Mode mode;

    public CommandSpawn(WaypointPlus plugin, Mode mode) { this.plugin = plugin; this.mode = mode; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        switch (mode) {
            case SPAWN -> {
                if (!(sender instanceof Player p)) { sender.sendMessage(ChatColor.RED + "Players only."); return true; }
                plugin.teleports().teleport(p, plugin.spawn().get(), "spawn",
                        () -> p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                plugin.messages().getString("prefix", "") + plugin.messages().getString("spawn-teleport", ""))));
            }
            case SETSPAWN -> {
                if (!(sender instanceof Player p)) { sender.sendMessage(ChatColor.RED + "Players only."); return true; }
                plugin.spawn().set(p.getLocation());
                p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.messages().getString("prefix", "") + plugin.messages().getString("spawn-set", "")));
            }
        }
        return true;
    }
}

