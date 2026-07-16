package me.BigBou.rideableMobs;

import me.BigBou.rideableMobs.command.RideableMobsCommand;
import me.BigBou.rideableMobs.listener.InputListener;
import me.BigBou.rideableMobs.listener.InputState;
import me.BigBou.rideableMobs.listener.PacketListener;
import me.BigBou.rideableMobs.listener.PlayerListener;
import me.BigBou.rideableMobs.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class RideableMobs extends JavaPlugin {
    public RideableMobs() {
    }

    public void onEnable() {
        this.getConfig().options().copyDefaults();
        this.saveDefaultConfig();
        Util.onEnable(this);
        RideableMobsCommand rideableMobsCommand = new RideableMobsCommand(this);
        this.getCommand("rideablemobs").setExecutor(rideableMobsCommand);
        this.getCommand("rideablemobs").setTabCompleter(rideableMobsCommand);
        PacketListener.onEnable(this);
        this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        this.getServer().getScheduler().runTaskTimer(this, () -> {
            double speed = this.getConfig().getDouble("entity-speed", 0.4);
            InputListener.forEachInput((uuid, input) -> {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !Util.isWorldEnabled(player) || RideableMobsCommand.isDisabled(uuid) || input.sneak()) {
                    return;
                }
                Entity vehicle = player.getVehicle();
                if (vehicle != null && !(vehicle instanceof org.bukkit.entity.Vehicle)) {
                    Util.applyMovement(player, vehicle, input, speed);
                }
            });
        }, 1L, 1L);
    }

    public void onDisable() {
    }
}
