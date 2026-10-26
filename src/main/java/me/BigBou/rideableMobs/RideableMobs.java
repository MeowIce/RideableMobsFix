package me.BigBou.rideableMobs;

import me.BigBou.rideableMobs.command.RideableMobsCommand;
import me.BigBou.rideableMobs.listener.InputListener;
import me.BigBou.rideableMobs.listener.PacketListener;
import me.BigBou.rideableMobs.listener.PlayerListener;
import me.BigBou.rideableMobs.util.LanguageManager;
import me.BigBou.rideableMobs.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.entity.Camel;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Player;
import org.bukkit.entity.SkeletonHorse;
import org.bukkit.entity.ZombieHorse;
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
        PacketListener.onEnable(this);
        this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        this.getServer().getScheduler().runTaskTimer(this, () -> {
            double speed = this.getConfig().getDouble("entity-speed", 0.4);
            InputListener.forEachInput((uuid, input) -> {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline()) {
                    InputListener.removeInput(uuid);
                    return;
                }

                Entity vehicle = player.getVehicle();
                if (vehicle == null || !vehicle.isValid()) {
                    InputListener.removeInput(uuid);
                    return;
                }

                if (!Util.isWorldEnabled(player) || RideableMobsCommand.isDisabled(uuid) || input.sneak()) {
                    return;
                }

                boolean isRideable = !(vehicle instanceof org.bukkit.entity.Vehicle)
                        || vehicle instanceof SkeletonHorse
                        || vehicle instanceof ZombieHorse
                        || vehicle instanceof Llama
                        || vehicle instanceof Camel;

                if (isRideable) {
                    Util.applyMovement(player, vehicle, input, speed);
                }
            });
        }, 1L, 1L);
    }

    @Override
    public void onDisable() {
        RideableMobsCommand.saveData(this);
    }
}
