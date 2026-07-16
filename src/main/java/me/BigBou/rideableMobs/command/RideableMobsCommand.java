package me.BigBou.rideableMobs.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;
import me.BigBou.rideableMobs.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;

public class RideableMobsCommand implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private static final Set<UUID> disabledPlayers = new HashSet<>();
    // Tracks explicitly denied entity types per player (overrides OP permissions)
    private static final HashMap<UUID, Set<String>> deniedEntities = new HashMap<>();
    // Tracks explicitly granted entity types per player
    private static final HashMap<UUID, Set<String>> grantedEntities = new HashMap<>();
    private static final List<String> availableCommands = List.of("permission", "reload", "toggle");

    public static boolean isDisabled(UUID uuid) {
        return disabledPlayers.contains(uuid);
    }

    public static void togglePlayer(UUID uuid) {
        if (disabledPlayers.contains(uuid)) {
            disabledPlayers.remove(uuid);
        } else {
            disabledPlayers.add(uuid);
        }
    }

    /**
     * Checks if a player can ride a specific entity type.
     * Explicitly denied entities (via /rm permission) always take priority.
     * Then explicitly granted entities are checked.
     * Finally falls back to Bukkit permission system.
     */
    public static boolean canRide(Player player, String entityType) {
        UUID uuid = player.getUniqueId();
        // Check if explicitly denied via /rm permission
        Set<String> denied = deniedEntities.get(uuid);
        if (denied != null && denied.contains(entityType)) {
            return false;
        }
        // Check if explicitly granted via /rm permission
        Set<String> granted = grantedEntities.get(uuid);
        if (granted != null && granted.contains(entityType)) {
            return true;
        }
        // Fallback to Bukkit permission system
        return player.hasPermission("rideablemobs.ride.*") || player.hasPermission("rideablemobs.ride." + entityType);
    }

    public RideableMobsCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private void send(CommandSender sender, String key, String fallback) {
        String message = plugin.getConfig().getString(key, fallback);
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            if (!sender.hasPermission("rideablemobs.commands.permission") &&
                !sender.hasPermission("rideablemobs.commands.reload") &&
                !sender.hasPermission("rideablemobs.commands.toggle")) {
                send(sender, "no-permission-command", "&cYou do not have permission to use this command.");
                return false;
            }
            send(sender, "command-usage", "&cInvalid usage. Use /rideablemobs <command>.");
            return false;
        } else if ("toggle".equals(args[0])) {
            if (!sender.hasPermission("rideablemobs.commands.toggle")) {
                send(sender, "no-permission-command", "&cYou do not have permission to use this command.");
                return false;
            }
            if (!(sender instanceof Player)) {
                send(sender, "only-players", "&cOnly players can use this command.");
                return true;
            }
            Player player = (Player) sender;
            
            togglePlayer(player.getUniqueId());
            boolean isDisabled = isDisabled(player.getUniqueId());
            if (isDisabled) {
                player.leaveVehicle();
            }
            send(player, isDisabled ? "toggle-disabled" : "toggle-enabled", isDisabled ? "&cRideableMobs has been disabled for you." : "&aRideableMobs has been enabled for you.");
            return true;
        }

        if ("permission".equals(args[0])) {
            if (!sender.hasPermission("rideablemobs.commands.permission")) {
                send(sender, "no-permission-command", "&cYou do not have permission to use this command.");
                return false;
            }
            if (args.length < 4) {
                send(sender, "permission-command-usage", "&cInvalid usage. Use /rideablemobs permission <player> <entity_type> <true/false>.");
                return false;
            } else {
                Player player = Bukkit.getPlayer(args[1]);
                if (player == null) {
                    send(sender, "player-not-found", "&cPlayer not found.");
                    return false;
                } else {
                    EntityType entityType;
                    try {
                        entityType = EntityType.valueOf(args[2].toUpperCase());
                    } catch (IllegalArgumentException var11) {
                        send(sender, "invalid-entity-type", "&cInvalid entity type.");
                        return false;
                    }

                    if (!args[3].equalsIgnoreCase("true") && !args[3].equalsIgnoreCase("false")) {
                        send(sender, "invalid-value", "&cInvalid value. Use true or false.");
                        return false;
                    }
                    boolean value = Boolean.parseBoolean(args[3]);

                    String entityName = entityType.name().toLowerCase();
                    UUID targetUUID = player.getUniqueId();
                    if (value) {
                        // Grant: remove from denied, add to granted
                        Set<String> denied = deniedEntities.get(targetUUID);
                        if (denied != null) denied.remove(entityName);
                        grantedEntities.computeIfAbsent(targetUUID, k -> new HashSet<>()).add(entityName);
                    } else {
                        // Deny: remove from granted, add to denied
                        Set<String> granted = grantedEntities.get(targetUUID);
                        if (granted != null) granted.remove(entityName);
                        deniedEntities.computeIfAbsent(targetUUID, k -> new HashSet<>()).add(entityName);
                    }
                    send(sender, value ? "permission-set-success" : "permission-removed-success", value ? "&aPermission set successfully." : "&aPermission removed successfully.");
                    String message = plugin.getConfig().getString(value ? "granted-permission" : "revoked-permission");
                    if (message != null && !message.isEmpty()) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message.replace("%entity_type%", entityType.name())));
                    }
                    return true;
                }
            }
        } else if ("reload".equals(args[0])) {
            if (!sender.hasPermission("rideablemobs.commands.reload")) {
                send(sender, "no-permission-command", "&cYou do not have permission to use this command.");
                return false;
            }
            Util.onReload(sender);
            return true;
        } else {
            send(sender, "invalid-command", "&cInvalid command.");
            return false;
        }
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        ArrayList<String> matches = new ArrayList<>();
        if (args.length == 1) {
            List<String> allowed = availableCommands.stream()
                    .filter(name -> sender.hasPermission("rideablemobs.commands." + name))
                    .toList();
            StringUtil.copyPartialMatches(args[0], allowed, matches);
        } else if (sender.hasPermission("rideablemobs.commands.permission") && "permission".equals(args[0])) {
            if (args.length == 2) {
                StringUtil.copyPartialMatches(args[1], Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), matches);
            } else if (args.length == 3) {
                StringUtil.copyPartialMatches(args[2], Arrays.stream(EntityType.values())
                        .filter(EntityType::isAlive)
                        .map(type -> type.name().toLowerCase())
                        .toList(), matches);
            } else if (args.length == 4) {
                StringUtil.copyPartialMatches(args[3], List.of("true", "false"), matches);
            }
        }
        return matches;
    }
}
