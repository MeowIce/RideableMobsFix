package me.BigBou.rideableMobs.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;

public class RideableMobsCommand implements CommandExecutor, TabCompleter {
    private JavaPlugin plugin;
    private static HashMap<UUID, PermissionAttachment> permissions;
    private static final Set<UUID> disabledPlayers = new HashSet<>();
    private static List<String> availableCommands = new ArrayList<String>() {
        {
            this.add("permission");
            this.add("reload");
            this.add("toggle");
        }
    };

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

    public RideableMobsCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        permissions = new HashMap();
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            if (!sender.hasPermission("rideablemobs.commands.permission") &&
                !sender.hasPermission("rideablemobs.commands.reload") &&
                !sender.hasPermission("rideablemobs.commands.toggle")) {
                String msg = this.plugin.getConfig().getString("no-permission-command", "&cYou do not have permission to use this command.");
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                return false;
            }
            String msg = this.plugin.getConfig().getString("command-usage", "&cInvalid usage. Use /rideablemobs <command>.");
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
            return false;
        } else if ("toggle".equals(args[0])) {
            if (!sender.hasPermission("rideablemobs.commands.toggle")) {
                String msg = this.plugin.getConfig().getString("no-permission-command", "&cYou do not have permission to use this command.");
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                return false;
            }
            if (!(sender instanceof Player)) {
                String msg = this.plugin.getConfig().getString("only-players", "&cOnly players can use this command.");
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                return true;
            }
            Player player = (Player) sender;
            
            togglePlayer(player.getUniqueId());
            boolean isDisabled = isDisabled(player.getUniqueId());
            String msg = this.plugin.getConfig().getString(isDisabled ? "toggle-disabled" : "toggle-enabled", isDisabled ? "&cRideableMobs has been disabled for you." : "&aRideableMobs has been enabled for you.");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
            return true;
        }

        if ("permission".equals(args[0])) {
            if (!sender.hasPermission("rideablemobs.commands.permission")) {
                String msg = this.plugin.getConfig().getString("no-permission-command", "&cYou do not have permission to use this command.");
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                return false;
            }
            if (args.length < 4) {
                String msg = this.plugin.getConfig().getString("permission-command-usage", "&cInvalid usage. Use /rideablemobs permission <player> <entity_type> <true/false>.");
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                return false;
            } else {
                Player player = Bukkit.getPlayer(args[1]);
                if (player == null) {
                    String msg = this.plugin.getConfig().getString("player-not-found", "&cPlayer not found.");
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                    return false;
                } else {
                    EntityType entityType;
                    try {
                        entityType = EntityType.valueOf(args[2].toUpperCase());
                    } catch (IllegalArgumentException var11) {
                        String msg = this.plugin.getConfig().getString("invalid-entity-type", "&cInvalid entity type.");
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                        return false;
                    }

                    if (!args[3].equalsIgnoreCase("true") && !args[3].equalsIgnoreCase("false")) {
                        String msg = this.plugin.getConfig().getString("invalid-value", "&cInvalid value. Use true or false.");
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                        return false;
                    }
                    boolean value = Boolean.parseBoolean(args[3]);

                    PermissionAttachment attachment = (PermissionAttachment)permissions.computeIfAbsent(player.getUniqueId(), (playerUUID) -> player.addAttachment(this.plugin));
                    String var10001 = entityType.name().toLowerCase();
                    attachment.setPermission("rideablemobs.ride." + var10001, value);
                    String msg = this.plugin.getConfig().getString(value ? "permission-set-success" : "permission-removed-success", value ? "&aPermission set successfully." : "&aPermission removed successfully.");
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                    Optional<String> message = Optional.of(ChatColor.translateAlternateColorCodes('&', (String)Objects.requireNonNull(this.plugin.getConfig().getString(value ? "granted-permission" : "revoked-permission"))));
                    Optional<String> var10000 = message.map((m) -> m.replaceAll("%entity_type%", entityType.name()));
                    Objects.requireNonNull(player);
                    var10000.ifPresent(player::sendMessage);
                    return true;
                }
            }
        } else if ("reload".equals(args[0])) {
            if (!sender.hasPermission("rideablemobs.commands.reload")) {
                String msg = this.plugin.getConfig().getString("no-permission-command", "&cYou do not have permission to use this command.");
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                return false;
            }
            Util.onReload(sender);
            return true;
        } else {
            String msg = this.plugin.getConfig().getString("invalid-command", "&cInvalid command.");
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
            return false;
        }
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        ArrayList var10000;
        switch (args.length) {
            case 1 -> var10000 = (ArrayList)StringUtil.copyPartialMatches(args[0], availableCommands, new ArrayList());
            case 2 -> {
                if ("permission".equals(args[0])) {
                    var10000 = (ArrayList)StringUtil.copyPartialMatches(args[1], Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), new ArrayList());
                } else {
                    var10000 = new ArrayList();
                }
            }
            case 3 -> {
                if ("permission".equals(args[0])) {
                    var10000 = (ArrayList)StringUtil.copyPartialMatches(args[2], Arrays.stream(EntityType.values()).filter(EntityType::isAlive).map((entityType) -> entityType.name().toLowerCase()).toList(), new ArrayList());
                } else {
                    var10000 = new ArrayList();
                }
            }
            case 4 -> {
                if ("permission".equals(args[0])) {
                    var10000 = (ArrayList)StringUtil.copyPartialMatches(args[3], List.of("true", "false"), new ArrayList());
                } else {
                    var10000 = new ArrayList();
                }
            }
            default -> var10000 = null;
        }

        return var10000;
    }
}
