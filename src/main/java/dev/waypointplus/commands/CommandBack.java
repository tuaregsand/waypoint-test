package dev.waypointplus.commands;

import dev.waypointplus.WaypointPlus;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;

public final class CommandBack implements CommandExecutor {

    private final WaypointPlus plugin;

    public CommandBack(WaypointPlus plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage(ChatColor.RED + "Players only."); return true; }
        Optional<Location> back = plugin.back().getBack(p.getUniqueId());
        if (back.isEmpty()) {
            p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.messages().getString("prefix", "") + plugin.messages().getString("back-missing", "")));
            return true;
        }
        plugin.teleports().teleport(p, back.get(), "back",
                () -> p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.messages().getString("prefix", "") + plugin.messages().getString("back-teleport", ""))));
        return true;
    }
}

