package me.BigBou.rideableMobs.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Flying;
import org.bukkit.entity.Player;
import org.bukkit.entity.WaterMob;
import org.bukkit.entity.EnderDragon;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import me.BigBou.rideableMobs.listener.InputListener;
import org.bukkit.plugin.Plugin;

public class Util {
    private static Plugin plugin;
    public static final Set<World> ENABLED_WORLDS = new HashSet();

    public Util() {
    }

    public static void onEnable(Plugin plugin) {
        Util.plugin = plugin;
        getWorlds();
    }

    public static void onReload(CommandSender sender) {
        plugin.reloadConfig();
        ENABLED_WORLDS.clear();
        getWorlds();
        String msg = plugin.getConfig().getString("reload-complete", "&aRideableMobs reload complete.");
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
    }

    public static boolean canSwim(Entity entity) {
        boolean var10000;
        switch (entity.getType()) {
            case DROWNED:
            case GUARDIAN:
            case TURTLE:
            case AXOLOTL:
                var10000 = true;
                break;
            default:
                var10000 = entity instanceof WaterMob;
        }

        return var10000;
    }

    public static boolean canFly(Entity entity) {
        boolean var10000;
        switch (entity.getType()) {
            case ALLAY:
            case BAT:
            case BEE:
            case BLAZE:
            case ENDER_DRAGON:
            case PARROT:
            case VEX:
            case WITHER:
                var10000 = true;
                break;
            default:
                var10000 = entity instanceof Flying;
        }

        return var10000;
    }

    public static boolean isWorldEnabled(Player player) {
        return ENABLED_WORLDS.contains(player.getWorld());
    }

    private static void getWorlds() {
        List<String> worlds = plugin.getConfig().getStringList("worlds");
        worlds.forEach((world) -> {
            World w = Bukkit.getWorld(world);
            ENABLED_WORLDS.add(w);
        });
    }

    public static void applyMovement(Player player, Entity vehicle, InputListener.WrappedInput input, double speed) {
        int forward = input.isForward() ? 1 : (input.isBackward() ? -1 : 0);
        int sideways = input.isLeft() ? 1 : (input.isRight() ? -1 : 0);

        Location playerLocation = player.getLocation();
        float yaw = playerLocation.getYaw();
        float pitch = playerLocation.getPitch();
        vehicle.setRotation(yaw, pitch);

        double radians = Math.toRadians(yaw);
        double x = (-forward) * Math.sin(radians) + sideways * Math.cos(radians);
        double z = forward * Math.cos(radians) + sideways * Math.sin(radians);

        Vector velocity = new Vector(x, 0.0, z);
        if (velocity.lengthSquared() > 0) {
            velocity.normalize().multiply(speed);
        }

        velocity.setY(vehicle.getVelocity().getY());

        if (!Double.isFinite(velocity.getX())) {
            velocity.setX(0);
        }
        if (!Double.isFinite(velocity.getZ())) {
            velocity.setZ(0);
        }

        if (vehicle.isInWater() && !Util.canSwim(vehicle) && !vehicle.isOnGround()) {
            velocity.setY(-0.08);
        }

        if (Util.canFly(vehicle) && !vehicle.isOnGround()) {
            velocity.setY(-0.08);
        }

        if (input.isJump() && (Util.canFly(vehicle) || Util.canSwim(vehicle) && vehicle.isInWater() || vehicle.isOnGround())) {
            velocity.setY(0.5);
        }

        if (vehicle instanceof EnderDragon) {
            EnderDragon enderDragon = (EnderDragon) vehicle;
            enderDragon.setRotation(yaw + 180.0F, pitch);
        }

        vehicle.setVelocity(velocity);
    }
}