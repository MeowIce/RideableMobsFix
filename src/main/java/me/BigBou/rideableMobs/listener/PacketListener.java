package me.BigBou.rideableMobs.listener;

import com.comphenix.protocol.ProtocolLibrary;
import org.bukkit.plugin.java.JavaPlugin;

public class PacketListener {
    public PacketListener() {
    }

    public static void onEnable(JavaPlugin plugin) {
        ProtocolLibrary.getProtocolManager().addPacketListener(new InputListener(plugin));
    }
}
