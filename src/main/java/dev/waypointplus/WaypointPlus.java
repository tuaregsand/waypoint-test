package dev.waypointplus;

import dev.waypointplus.back.BackManager;
import dev.waypointplus.commands.*;
import dev.waypointplus.eco.EconomyHook;
import dev.waypointplus.home.HomeManager;
import dev.waypointplus.spawn.SpawnManager;
import dev.waypointplus.tp.TeleportManager;
import dev.waypointplus.tpa.TpaManager;
import dev.waypointplus.warp.WarpManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class WaypointPlus extends JavaPlugin {

    private EconomyHook economy;
    private TeleportManager teleports;
    private HomeManager homes;
    private WarpManager warps;
    private SpawnManager spawn;
    private BackManager back;
    private TpaManager tpa;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);

        // Data dirs
        File data = getDataFolder();
        new File(data, "data/homes").mkdirs();

        // Economy (Vault)
        economy = new EconomyHook(this);
        boolean economyReady = economy.hook();
        if (getConfig().getBoolean("economy.require-vault") && !economyReady) {
            getLogger().severe("Vault required but not found. Disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        teleports = new TeleportManager(this, economy);
        homes = new HomeManager(this);
        warps = new WarpManager(this);
        spawn = new SpawnManager(this);
        back = new BackManager(this);
        tpa = new TpaManager(this, teleports);

        // Commands
        getCommand("home").setExecutor(new CommandHomes(this, CommandHomes.Mode.HOME));
        getCommand("sethome").setExecutor(new CommandHomes(this, CommandHomes.Mode.SETHOME));
        getCommand("delhome").setExecutor(new CommandHomes(this, CommandHomes.Mode.DELHOME));
        getCommand("homes").setExecutor(new CommandHomes(this, CommandHomes.Mode.LISTHOMES));
        getCommand("home").setTabCompleter(new CommandHomes(this, CommandHomes.Mode.HOME));
        getCommand("delhome").setTabCompleter(new CommandHomes(this, CommandHomes.Mode.DELHOME));

        getCommand("warp").setExecutor(new CommandWarps(this, CommandWarps.Mode.WARP));
        getCommand("setwarp").setExecutor(new CommandWarps(this, CommandWarps.Mode.SETWARP));
        getCommand("delwarp").setExecutor(new CommandWarps(this, CommandWarps.Mode.DELWARP));
        getCommand("warps").setExecutor(new CommandWarps(this, CommandWarps.Mode.LISTWARPS));
        getCommand("warp").setTabCompleter(new CommandWarps(this, CommandWarps.Mode.WARP));
        getCommand("delwarp").setTabCompleter(new CommandWarps(this, CommandWarps.Mode.DELWARP));

        getCommand("spawn").setExecutor(new CommandSpawn(this, CommandSpawn.Mode.SPAWN));
        getCommand("setspawn").setExecutor(new CommandSpawn(this, CommandSpawn.Mode.SETSPAWN));

        getCommand("back").setExecutor(new CommandBack(this));

        getCommand("rtp").setExecutor(new CommandRtp(this));
        getCommand("rtp").setTabCompleter(new CommandRtp(this));

        getCommand("tpa").setExecutor(new CommandTpa(this, CommandTpa.Mode.SEND));
        getCommand("tpaccept").setExecutor(new CommandTpa(this, CommandTpa.Mode.ACCEPT));
        getCommand("tpdeny").setExecutor(new CommandTpa(this, CommandTpa.Mode.DENY));
        getCommand("tpacancel").setExecutor(new CommandTpa(this, CommandTpa.Mode.CANCEL));

        // Events
        Bukkit.getPluginManager().registerEvents(teleports, this);
        Bukkit.getPluginManager().registerEvents(back, this);
        Bukkit.getPluginManager().registerEvents(tpa, this);

        getLogger().info("WaypointPlus enabled.");
    }

    @Override
    public void onDisable() {
        back.saveAll();
        homes.flush();
        warps.flush();
        getLogger().info("WaypointPlus disabled.");
    }

    public FileConfiguration messages() {
        return Yaml.load(this, "messages.yml");
    }

    // Accessors
    public EconomyHook economy() { return economy; }
    public TeleportManager teleports() { return teleports; }
    public HomeManager homes() { return homes; }
    public WarpManager warps() { return warps; }
    public SpawnManager spawn() { return spawn; }
    public BackManager back() { return back; }
    public TpaManager tpa() { return tpa; }

    // Simple YAML loader for messages.yml
    public static final class Yaml {
        public static org.bukkit.configuration.file.FileConfiguration load(JavaPlugin plugin, String path) {
            java.io.File file = new java.io.File(plugin.getDataFolder(), path);
            return org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        }
    }
}

