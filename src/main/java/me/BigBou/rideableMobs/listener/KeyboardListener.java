package me.BigBou.rideableMobs.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.PacketType.Play.Client;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import me.BigBou.rideableMobs.util.Util;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

public class KeyboardListener extends PacketAdapter {
    private static final Logger log = LoggerFactory.getLogger(KeyboardListener.class);

    public KeyboardListener(Plugin plugin) {
        super(plugin, new PacketType[]{PacketType.Play.Server.ABILITIES});
    }

    public void onPacketSending(PacketEvent event) {
        Player player = event.getPlayer();
        if (Util.isWorldEnabled(player)) {
            Entity vehicle = player.getVehicle();
            if (vehicle != null && !(vehicle instanceof Vehicle)) {
                PacketContainer packet = event.getPacket();
                InputState input = InputListener.getInput(player.getUniqueId());
                if (input == null) {
                    return; // No input data available
                }
                if (!input.sneak()) {
                    // Inside the update/tick loop
                    int forward = input.forward() ? 1 : (input.backward() ? -1 : 0);
                    int sideways = input.left() ? 1 : (input.right() ? -1 : 0);

                    // Get player's yaw and pitch
                    Location playerLocation = player.getLocation();
                    float yaw = playerLocation.getYaw();
                    float pitch = playerLocation.getPitch();
                    vehicle.setRotation(yaw, pitch);

                    // Convert yaw from degrees to radians
                    double radians = Math.toRadians(yaw);

                    // Calculate movement in X and Z directions
                    double x = (-forward) * Math.sin(radians) + sideways * Math.cos(radians);
                    double z = forward * Math.cos(radians) + sideways * Math.sin(radians);

                    // Create a velocity vector
                    Vector velocity = new Vector(x, 0.0, z).normalize().multiply(this.plugin.getConfig().getDouble("entity-speed"));  // Adjust speed as needed

                    // Retain the vertical velocity (e.g., gravity, flying, etc.)
                    velocity.setY(vehicle.getVelocity().getY());

                    // Ensure no non-finite values in the velocity vector
                    if (!Double.isFinite(velocity.getX())) {
                        velocity.setX(0);
                    }
                    if (!Double.isFinite(velocity.getZ())) {
                        velocity.setZ(0);
                    }

                    // Handle special movement cases (e.g., in water, flying)
                    if (vehicle.isInWater() && !Util.canSwim(vehicle) && !vehicle.isOnGround()) {
                        velocity.setY(-0.08);  // Slight downward velocity to simulate sinking
                    }

                    if (Util.canFly(vehicle) && !vehicle.isOnGround()) {
                        velocity.setY(-0.08);  // Slight downward velocity to simulate flying gravity
                    }

                    // Jumping logic (if the player is jumping and can fly or is on the ground)
                    if (input.jump() && (Util.canFly(vehicle) || Util.canSwim(vehicle) && vehicle.isInWater() || vehicle.isOnGround())) {
                        velocity.setY(0.5);  // Apply upward velocity for jumping
                    }

                    // Special handling for EnderDragon (reverse its rotation)
                    if (vehicle instanceof EnderDragon) {
                        EnderDragon enderDragon = (EnderDragon)vehicle;
                        enderDragon.setRotation(yaw + 180.0F, pitch);  // Reverse the dragon's rotation
                    }

                    // Apply the velocity to the vehicle
                    vehicle.setVelocity(velocity);

                }
            }
        }
    }
    public static void showPacket(JavaPlugin plugin, PacketContainer packet) {
        plugin.getLogger().info("=== Packet Dump: " + packet.getType() + " ===");

        StructureModifier<Object> modifier = packet.getModifier();

        for (int i = 0; i < modifier.size(); i++) {
            try {
                Object value = modifier.readSafely(i);
                Class<?> valueClass = (value != null) ? value.getClass() : Object.class;

                plugin.getLogger().info("Field " + i + " [" + valueClass.getSimpleName() + "] = " + value);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to read field " + i + ": " + e.getMessage());
            }
        }
    }

}
