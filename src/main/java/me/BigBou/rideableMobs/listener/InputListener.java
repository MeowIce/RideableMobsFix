package me.BigBou.rideableMobs.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import me.BigBou.rideableMobs.util.Util;
import org.bukkit.entity.Camel;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Player;
import org.bukkit.entity.SkeletonHorse;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.ZombieHorse;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;

public class InputListener extends PacketAdapter {
    private static final Logger log = LoggerFactory.getLogger(InputListener.class);

    private static final ConcurrentMap<UUID, InputState> playerInputs = new ConcurrentHashMap<>();

    private static Method forwardMethod;
    private static Method backwardMethod;
    private static Method leftMethod;
    private static Method rightMethod;
    private static Method jumpMethod;
    private static Method shiftMethod;
    private static Method sprintMethod;
    private static volatile boolean reflectionInitialized = false;

    public InputListener(Plugin plugin) {
        super(plugin, new PacketType[]{PacketType.Play.Client.STEER_VEHICLE});
    }

    public static InputState getInput(UUID playerId) {
        return playerInputs.get(playerId);
    }

    public static void removeInput(UUID playerId) {
        playerInputs.remove(playerId);
    }

    public static void forEachInput(BiConsumer<UUID, InputState> consumer) {
        playerInputs.forEach(consumer);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        if (Util.isWorldEnabled(player)) {
            Entity vehicle = player.getVehicle();
            boolean isAllowedVehicle = vehicle != null && (!(vehicle instanceof Vehicle)
                    || vehicle instanceof SkeletonHorse
                    || vehicle instanceof ZombieHorse
                    || vehicle instanceof Llama
                    || vehicle instanceof Camel);

            if (isAllowedVehicle) {
                PacketContainer packet = event.getPacket();
                InputState input = readPacketInput(packet);
                if (input != null) {
                    playerInputs.put(player.getUniqueId(), input);
                }
            }
        }
    }

    public static InputState readPacketInput(PacketContainer packet) {
        if (packet == null) {
            return null;
        }

        if (packet.getModifier().size() > 0) {
            Object handle = packet.getModifier().read(0);
            InputState state = readInput(handle);
            if (state != null) {
                return state;
            }
        }

        // Fallback for classic ProtocolLib steer packet (1.21.0 - 1.21.3)
        if (packet.getFloat().size() >= 2 && packet.getBooleans().size() >= 2) {
            float sideways = packet.getFloat().read(0);
            float forward = packet.getFloat().read(1);
            boolean jump = packet.getBooleans().read(0);
            boolean sneak = packet.getBooleans().read(1);
            return new InputState(forward > 0, forward < 0, sideways > 0, sideways < 0, jump, sneak, false);
        }

        return null;
    }

    private static synchronized void initReflection(Class<?> type) {
        if (reflectionInitialized) {
            return;
        }
        try {
            forwardMethod = type.getMethod("forward");
            backwardMethod = type.getMethod("backward");
            leftMethod = type.getMethod("left");
            rightMethod = type.getMethod("right");
            jumpMethod = type.getMethod("jump");
            shiftMethod = type.getMethod("shift");
            sprintMethod = type.getMethod("sprint");
            forwardMethod.setAccessible(true);
            backwardMethod.setAccessible(true);
            leftMethod.setAccessible(true);
            rightMethod.setAccessible(true);
            jumpMethod.setAccessible(true);
            shiftMethod.setAccessible(true);
            sprintMethod.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            log.warn("Could not bind record methods on {}: {}", type.getName(), e.getMessage());
        } finally {
            reflectionInitialized = true;
        }
    }

    private static InputState readInput(Object handle) {
        if (handle == null) {
            return null;
        }

        if (!reflectionInitialized) {
            initReflection(handle.getClass());
        }

        if (forwardMethod == null) {
            return null;
        }

        try {
            return new InputState(
                    (boolean) forwardMethod.invoke(handle),
                    (boolean) backwardMethod.invoke(handle),
                    (boolean) leftMethod.invoke(handle),
                    (boolean) rightMethod.invoke(handle),
                    (boolean) jumpMethod.invoke(handle),
                    (boolean) shiftMethod.invoke(handle),
                    (boolean) sprintMethod.invoke(handle));
        } catch (ReflectiveOperationException | RuntimeException e) {
            log.warn("Ignoring malformed steer vehicle input: {}", e.getMessage());
            return null;
        }
    }
}
