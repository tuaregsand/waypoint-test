package dev.waypointplus.eco;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

public final class EconomyHook {
    private final JavaPlugin plugin;
    private Economy economy;

    public EconomyHook(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean hook() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().info("Vault not present. Economy disabled.");
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().info("No Economy provider present. Economy disabled.");
            return false;
        }
        economy = rsp.getProvider();
        plugin.getLogger().info("Hooked into economy: " + economy.getName());
        return true;
    }

    public boolean isEnabled() {
        return economy != null;
    }

    public boolean charge(OfflinePlayer player, double amount) {
        if (!isEnabled() || amount <= 0) return true;
        if (economy.getBalance(player) < amount) return false;
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public String format(double amount, String fallbackFormat) {
        if (isEnabled()) return economy.format(amount);
        return String.format(fallbackFormat, amount);
    }

    public Optional<Economy> provider() {
        return Optional.ofNullable(economy);
    }
}

