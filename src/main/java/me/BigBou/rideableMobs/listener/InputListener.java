package me.BigBou.rideableMobs.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import me.BigBou.rideableMobs.util.Util;
import org.bukkit.Input;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InputListener extends PacketAdapter {
    private static final Logger log = LoggerFactory.getLogger(InputListener.class);

    public InputListener(Plugin plugin) {
        super(plugin, new PacketType[]{PacketType.Play.Client.STEER_VEHICLE});
    }

    private static final ConcurrentMap<UUID, InputState> playerInputs = new ConcurrentHashMap<>();

    public static InputState getInput(UUID playerId) {
        return playerInputs.get(playerId);
    }

    public static void removeInput(UUID playerId) {
        playerInputs.remove(playerId);
    }

    public static void forEachInput(BiConsumer<UUID, InputState> consumer) {
        playerInputs.forEach(consumer);
    }

    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        if (Util.isWorldEnabled(player)) {
            Entity vehicle = player.getVehicle();
            if (vehicle != null && !(vehicle instanceof Vehicle)) {
                PacketContainer packet = event.getPacket();
                Object object = packet.getModifier().read(0);
                InputState input = readInput(object);
                if (input != null) {
                    playerInputs.put(player.getUniqueId(), input);
                }
            }
        }
    }

    private static InputState readInput(Object handle) {
        if (handle == null) {
            return null;
        }
        try {
            Class<?> type = handle.getClass();
            return new InputState(
                    read(type, handle, "forward"),
                    read(type, handle, "backward"),
                    read(type, handle, "left"),
                    read(type, handle, "right"),
                    read(type, handle, "jump"),
                    read(type, handle, "shift"),
                    read(type, handle, "sprint"));
        } catch (ReflectiveOperationException | RuntimeException e) {
            log.warn("Ignoring malformed steer vehicle input: {}", e.getMessage());
            return null;
        }
    }

    private static boolean read(Class<?> type, Object handle, String method) throws ReflectiveOperationException {
        return (boolean) type.getMethod(method).invoke(handle);
    }
}
