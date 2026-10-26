package me.BigBou.rideableMobs;

import me.BigBou.rideableMobs.command.RideableMobsCommand;
import me.BigBou.rideableMobs.listener.PlayerListener;
import me.BigBou.rideableMobs.util.LanguageManager;
import me.BigBou.rideableMobs.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Input;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class RideableMobs extends JavaPlugin {
    private LanguageManager languageManager;

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public RideableMobs() {
    }

    @Override
    public void onEnable() {
        this.getConfig().options().copyDefaults();
        this.saveDefaultConfig();
        this.languageManager = new LanguageManager(this);
        Util.onEnable(this);
        RideableMobsCommand rideableMobsCommand = new RideableMobsCommand(this);
        this.getCommand("rideablemobs").setExecutor(rideableMobsCommand);
        this.getCommand("rideablemobs").setTabCompleter(rideableMobsCommand);
        this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        this.getServer().getScheduler().runTaskTimer(this, () -> {
            double speed = this.getConfig().getDouble("entity-speed", 0.4);
            for (Player player : Bukkit.getOnlinePlayers()) {
                Entity vehicle = player.getVehicle();
                if (vehicle == null || !vehicle.isValid() || !Util.isAllowedVehicle(vehicle)) {
                    continue;
                }
                if (!Util.isWorldEnabled(player) || RideableMobsCommand.isDisabled(player.getUniqueId())) {
                    continue;
                }
                Input input = player.getCurrentInput();
                if (input == null || input.isSneak()) {
                    continue;
                }
                Util.applyMovement(player, vehicle, input, speed);
            }
        }, 1L, 1L);
    }

    @Override
    public void onDisable() {
        RideableMobsCommand.saveData(this);
    }
}
