package dev.waypointplus.commands;

import dev.waypointplus.WaypointPlus;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public final class CommandTpa implements CommandExecutor {

    public enum Mode { SEND, ACCEPT, DENY, CANCEL }

    private final WaypointPlus plugin;
    private final Mode mode;

    public CommandTpa(WaypointPlus plugin, Mode mode) { this.plugin = plugin; this.mode = mode; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        switch (mode) {
            case SEND -> {
                if (!(sender instanceof Player p)) { sender.sendMessage(ChatColor.RED + "Players only."); return true; }
                if (args.length < 1) { sender.sendMessage(ChatColor.RED + "Usage: /tpa <player>"); return true; }
                Player target = Bukkit.getPlayerExact(args[0]);
                if (target == null) { sender.sendMessage(ChatColor.RED + "Player not found."); return true; }
                if (target.getUniqueId().equals(p.getUniqueId())) { sender.sendMessage(ChatColor.RED + "Cannot TPA to yourself."); return true; }
                plugin.tpa().send(p, target);
            }
            case ACCEPT -> {
                if (!(sender instanceof Player p)) { sender.sendMessage(ChatColor.RED + "Players only."); return true; }
                if (!plugin.tpa().accept(p)) p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.messages().getString("prefix", "") + plugin.messages().getString("tpa-none", "")));
            }
            case DENY -> {
                if (!(sender instanceof Player p)) { sender.sendMessage(ChatColor.RED + "Players only."); return true; }
                if (!plugin.tpa().deny(p)) p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.messages().getString("prefix", "") + plugin.messages().getString("tpa-none", "")));
            }
            case CANCEL -> {
                if (!(sender instanceof Player p)) { sender.sendMessage(ChatColor.RED + "Players only."); return true; }
                if (!plugin.tpa().cancel(p.getUniqueId(), false)) p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.messages().getString("prefix", "") + plugin.messages().getString("tpa-none", "")));
            }
        }
        return true;
    }
}

