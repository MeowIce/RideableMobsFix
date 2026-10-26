package me.BigBou.rideableMobs.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Flying;
import org.bukkit.entity.Player;
import org.bukkit.entity.WaterMob;
import org.bukkit.util.Vector;
import me.BigBou.rideableMobs.RideableMobs;
import me.BigBou.rideableMobs.listener.InputState;

public class Util {
    private static RideableMobs plugin;
    private static LanguageManager lang;
    public static final Set<String> ENABLED_WORLDS = new HashSet<>();

    public Util() {
    }

    public static void onEnable(RideableMobs plugin) {
        Util.plugin = plugin;
        Util.lang = plugin.getLanguageManager();
        getWorlds();
    }

    public static void onReload(CommandSender sender) {
        plugin.reloadConfig();
        lang.reload();
        getWorlds();
        lang.send(sender, "reload-complete");
    }

    public static boolean canSwim(Entity entity) {
        if (entity == null) {
            return false;
        }
        return switch (entity.getType()) {
            case DROWNED, GUARDIAN, ELDER_GUARDIAN, TURTLE, AXOLOTL, FROG -> true;
            default -> entity instanceof WaterMob;
        };
    }

    public static boolean canFly(Entity entity) {
        if (entity == null) {
            return false;
        }
        return switch (entity.getType()) {
            case ALLAY, BAT, BEE, BLAZE, ENDER_DRAGON, PARROT, VEX, WITHER -> true;
            default -> entity instanceof Flying;
        };
    }

    public static boolean isWorldEnabled(Player player) {
        if (player == null || player.getWorld() == null) {
            return false;
        }
        return ENABLED_WORLDS.contains(player.getWorld().getName().toLowerCase());
    }

    private static void getWorlds() {
        ENABLED_WORLDS.clear();
        List<String> worlds = plugin.getConfig().getStringList("worlds");
        for (String world : worlds) {
            if (world != null && !world.isBlank()) {
                ENABLED_WORLDS.add(world.trim().toLowerCase());
            }
        }
    }

    public static Vector horizontalVelocity(float yaw, InputState input, double speed) {
        int forward = input.forward() ? 1 : (input.backward() ? -1 : 0);
        int sideways = input.left() ? 1 : (input.right() ? -1 : 0);
        double radians = Math.toRadians(yaw);
        Vector velocity = new Vector(
                (-forward) * Math.sin(radians) + sideways * Math.cos(radians),
                0.0,
                forward * Math.cos(radians) + sideways * Math.sin(radians));
        return velocity.lengthSquared() == 0 ? velocity : velocity.normalize().multiply(speed);
    }

    public static void applyMovement(Player player, Entity vehicle, InputState input, double speed) {
        Location playerLocation = player.getLocation();
        float yaw = playerLocation.getYaw();
        float pitch = playerLocation.getPitch();
        vehicle.setRotation(yaw, pitch);

        Vector velocity = horizontalVelocity(yaw, input, speed);
        velocity.setY(vehicle.getVelocity().getY());

        if (!Double.isFinite(velocity.getX())) {
            velocity.setX(0);
        }
        if (!Double.isFinite(velocity.getY())) {
            velocity.setY(0);
        }
        if (!Double.isFinite(velocity.getZ())) {
            velocity.setZ(0);
        }

        boolean canSwim = Util.canSwim(vehicle);
        boolean canFly = Util.canFly(vehicle);
        if (vehicle.isInWater() && !canSwim && !vehicle.isOnGround()) {
            velocity.setY(-0.08);
        }

        if (canFly && !vehicle.isOnGround()) {
            velocity.setY(-0.08);
        }

        if (input.jump() && (canFly || canSwim && vehicle.isInWater() || vehicle.isOnGround())) {
            velocity.setY(0.5);
        }

        if (vehicle instanceof EnderDragon enderDragon) {
            enderDragon.setRotation(yaw + 180.0F, pitch);
        }

        vehicle.setVelocity(velocity);
    }
}
