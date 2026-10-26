package me.BigBou.rideableMobs.util;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.Input;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Camel;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Flying;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Player;
import org.bukkit.entity.SkeletonHorse;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.WaterMob;
import org.bukkit.entity.ZombieHorse;
import org.bukkit.util.Vector;
import me.BigBou.rideableMobs.RideableMobs;

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
        return ENABLED_WORLDS.contains(player.getWorld().getName().toLowerCase(Locale.ROOT));
    }

    // Vanilla vehicles keep their own controls; these four are ridden through the plugin.
    public static boolean isAllowedVehicle(Entity entity) {
        return !(entity instanceof Vehicle)
                || entity instanceof SkeletonHorse
                || entity instanceof ZombieHorse
                || entity instanceof Llama
                || entity instanceof Camel;
    }

    private static void getWorlds() {
        ENABLED_WORLDS.clear();
        List<String> worlds = plugin.getConfig().getStringList("worlds");
        for (String world : worlds) {
            if (world != null && !world.isBlank()) {
                ENABLED_WORLDS.add(world.trim().toLowerCase(Locale.ROOT));
            }
        }
    }

    public static Vector horizontalVelocity(float yaw, Input input, double speed) {
        int forward = input.isForward() ? 1 : (input.isBackward() ? -1 : 0);
        int sideways = input.isLeft() ? 1 : (input.isRight() ? -1 : 0);
        double radians = Math.toRadians(yaw);
        Vector velocity = new Vector(
                (-forward) * Math.sin(radians) + sideways * Math.cos(radians),
                0.0,
                forward * Math.cos(radians) + sideways * Math.sin(radians));
        return velocity.lengthSquared() == 0 ? velocity : velocity.normalize().multiply(speed);
    }

    public static double verticalVelocity(double currentY, boolean inWater, boolean canSwim, boolean canFly,
                                          boolean onGround, boolean jump) {
        double y = currentY;
        if (inWater && !canSwim && !onGround) {
            y = -0.08;
        }
        if (canFly && !onGround) {
            y = -0.08;
        }
        if (jump && (canFly || canSwim && inWater || onGround)) {
            y = 0.5;
        }
        return y;
    }

    public static void applyMovement(Player player, Entity vehicle, Input input, double speed) {
        float yaw = player.getYaw();
        float pitch = player.getPitch();
        vehicle.setRotation(yaw, pitch);

        Vector velocity = horizontalVelocity(yaw, input, speed);
        double currentY = vehicle.getVelocity().getY();
        if (!Double.isFinite(velocity.getX())) {
            velocity.setX(0);
        }
        if (!Double.isFinite(currentY)) {
            currentY = 0;
        }
        if (!Double.isFinite(velocity.getZ())) {
            velocity.setZ(0);
        }

        velocity.setY(verticalVelocity(currentY, vehicle.isInWater(), Util.canSwim(vehicle), Util.canFly(vehicle),
                vehicle.isOnGround(), input.isJump()));

        if (vehicle instanceof EnderDragon enderDragon) {
            enderDragon.setRotation(yaw + 180.0F, pitch);
        }

        vehicle.setVelocity(velocity);
    }
}
