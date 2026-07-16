package me.BigBou.rideableMobs;

import me.BigBou.rideableMobs.command.RideableMobsCommand;
import me.BigBou.rideableMobs.listener.InputListener;
import me.BigBou.rideableMobs.listener.InputState;
import me.BigBou.rideableMobs.listener.PacketListener;
import me.BigBou.rideableMobs.listener.PlayerListener;
import me.BigBou.rideableMobs.util.Util;
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
            for (Player player : this.getServer().getOnlinePlayers()) {
                if (Util.isWorldEnabled(player)) {
                    if (RideableMobsCommand.isDisabled(player.getUniqueId())) {
                        continue;
                    }
                    Entity vehicle = player.getVehicle();
                    if (vehicle != null && !(vehicle instanceof org.bukkit.entity.Vehicle)) {
                        InputState input = InputListener.getInput(player.getUniqueId());
                        if (input != null && !input.sneak()) {
                            Util.applyMovement(player, vehicle, input, this.getConfig().getDouble("entity-speed", 0.4));
                        }
                    }
                }
            }
        }, 1L, 1L);
    }

    public void onDisable() {
    }
}
