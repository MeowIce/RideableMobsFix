package me.BigBou.rideableMobs.listener;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.bukkit.plugin.java.JavaPlugin;

public class PacketListener {
    private static JavaPlugin plugin;
    private static ProtocolManager manager;

    public PacketListener() {
    }

    public static void onEnable(JavaPlugin plugin) {
        PacketListener.plugin = plugin;
        manager = ProtocolLibrary.getProtocolManager();
        manager.addPacketListener(new InputListener(plugin));
    }
}
