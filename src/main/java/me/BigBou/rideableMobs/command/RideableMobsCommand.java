package me.BigBou.rideableMobs.command;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import me.BigBou.rideableMobs.RideableMobs;
import me.BigBou.rideableMobs.util.LanguageManager;
import me.BigBou.rideableMobs.util.Util;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class RideableMobsCommand implements CommandExecutor, TabCompleter {
    private final RideableMobs plugin;
    private final LanguageManager lang;
    private static final Set<UUID> disabledPlayers = ConcurrentHashMap.newKeySet();
    private static final ConcurrentMap<UUID, Set<String>> deniedEntities = new ConcurrentHashMap<>();
    private static final ConcurrentMap<UUID, Set<String>> grantedEntities = new ConcurrentHashMap<>();
    private static final List<String> availableCommands = List.of("permission", "reload", "toggle");

    public static boolean isDisabled(UUID uuid) {
        return disabledPlayers.contains(uuid);
    }

    public static void togglePlayer(UUID uuid) {
        if (!disabledPlayers.add(uuid)) {
            disabledPlayers.remove(uuid);
        }
    }

    public static boolean isExplicitlyDenied(Player player, String entityType) {
        Set<String> denied = deniedEntities.get(player.getUniqueId());
        return denied != null && denied.contains(entityType);
    }

    public static boolean isExplicitlyGranted(Player player, String entityType) {
        Set<String> granted = grantedEntities.get(player.getUniqueId());
        return granted != null && granted.contains(entityType);
    }

    public static boolean hasBukkitPermission(Player player, String entityType) {
        return player.hasPermission("rideablemobs.ride.*") || player.hasPermission("rideablemobs.ride." + entityType);
    }

    public RideableMobsCommand(RideableMobs plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
        loadData(plugin);
    }

    public static void loadData(RideableMobs plugin) {
        File file = new File(plugin.getDataFolder(), "data.yml");
        if (!file.exists()) return;

        FileConfiguration data = YamlConfiguration.loadConfiguration(file);

        disabledPlayers.clear();
        for (String uuidStr : data.getStringList("disabled-players")) {
            try {
                disabledPlayers.add(UUID.fromString(uuidStr));
            } catch (IllegalArgumentException ignored) {}
        }

        deniedEntities.clear();
        // contains() is true for a hand-written empty list, but getConfigurationSection is null there
        ConfigurationSection deniedSection = data.getConfigurationSection("denied-entities");
        if (deniedSection != null) {
            for (String uuidStr : deniedSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    Set<String> set = ConcurrentHashMap.newKeySet();
                    set.addAll(data.getStringList("denied-entities." + uuidStr));
                    deniedEntities.put(uuid, set);
                } catch (IllegalArgumentException ignored) {}
            }
        }

        grantedEntities.clear();
        ConfigurationSection grantedSection = data.getConfigurationSection("granted-entities");
        if (grantedSection != null) {
            for (String uuidStr : grantedSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    Set<String> set = ConcurrentHashMap.newKeySet();
                    set.addAll(data.getStringList("granted-entities." + uuidStr));
                    grantedEntities.put(uuid, set);
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public static void saveData(RideableMobs plugin) {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        FileConfiguration data = new YamlConfiguration();

        List<String> disabledList = new ArrayList<>();
        for (UUID uuid : disabledPlayers) {
            disabledList.add(uuid.toString());
        }
        data.set("disabled-players", disabledList);

        for (UUID uuid : deniedEntities.keySet()) {
            data.set("denied-entities." + uuid.toString(), new ArrayList<>(deniedEntities.get(uuid)));
        }

        for (UUID uuid : grantedEntities.keySet()) {
            data.set("granted-entities." + uuid.toString(), new ArrayList<>(grantedEntities.get(uuid)));
        }

        try {
            data.save(new File(dataFolder, "data.yml"));
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save data.yml");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            if (!sender.hasPermission("rideablemobs.commands.permission") &&
                !sender.hasPermission("rideablemobs.commands.reload") &&
                !sender.hasPermission("rideablemobs.commands.toggle")) {
                lang.send(sender, "no-permission-command");
                return true;
            }
            lang.send(sender, "command-usage");
            return true;
        } else if ("toggle".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("rideablemobs.commands.toggle")) {
                lang.send(sender, "no-permission-command");
                return true;
            }
            if (!(sender instanceof Player player)) {
                lang.send(sender, "only-players");
                return true;
            }

            togglePlayer(player.getUniqueId());
            saveData(plugin);
            boolean isDisabled = isDisabled(player.getUniqueId());
            if (isDisabled) {
                player.leaveVehicle();
            }
            lang.send(player, isDisabled ? "toggle-disabled" : "toggle-enabled");
            return true;
        }

        if ("permission".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("rideablemobs.commands.permission")) {
                lang.send(sender, "no-permission-command");
                return true;
            }
            if (args.length < 4) {
                lang.send(sender, "permission-command-usage");
                return true;
            }

            Player player = Bukkit.getPlayer(args[1]);
            if (player == null) {
                lang.send(sender, "player-not-found");
                return true;
            }

            EntityType entityType;
            try {
                entityType = EntityType.valueOf(args[2].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                lang.send(sender, "invalid-entity-type");
                return true;
            }

            String value = args[3].toLowerCase();
            if (!value.equals("true") && !value.equals("false") && !value.equals("default")) {
                lang.send(sender, "invalid-value");
                return true;
            }

            String entityName = entityType.name().toLowerCase(Locale.ROOT);
            UUID targetUUID = player.getUniqueId();
            switch (value) {
                case "true":
                    Set<String> denied = deniedEntities.get(targetUUID);
                    if (denied != null) denied.remove(entityName);
                    grantedEntities.computeIfAbsent(targetUUID, k -> ConcurrentHashMap.newKeySet()).add(entityName);
                    break;
                case "false":
                    Set<String> granted = grantedEntities.get(targetUUID);
                    if (granted != null) granted.remove(entityName);
                    deniedEntities.computeIfAbsent(targetUUID, k -> ConcurrentHashMap.newKeySet()).add(entityName);
                    break;
                case "default":
                    Set<String> d = deniedEntities.get(targetUUID);
                    if (d != null) d.remove(entityName);
                    Set<String> g = grantedEntities.get(targetUUID);
                    if (g != null) g.remove(entityName);
                    break;
            }
            saveData(plugin);
            lang.send(sender, "default".equals(value) ? "permission-default-success" : ("true".equals(value) ? "permission-set-success" : "permission-removed-success"), "%entity_type%", entityType.name());
            if (!"default".equals(value)) {
                lang.send(player, "true".equals(value) ? "granted-permission" : "revoked-permission", "%entity_type%", entityType.name());
            }
            return true;
        } else if ("reload".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("rideablemobs.commands.reload")) {
                lang.send(sender, "no-permission-command");
                return true;
            }
            Util.onReload(sender);
            return true;
        } else {
            lang.send(sender, "invalid-command");
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        ArrayList<String> matches = new ArrayList<>();
        if (args.length == 1) {
            List<String> allowed = availableCommands.stream()
                    .filter(name -> sender.hasPermission("rideablemobs.commands." + name))
                    .toList();
            StringUtil.copyPartialMatches(args[0], allowed, matches);
        } else if (sender.hasPermission("rideablemobs.commands.permission") && "permission".equalsIgnoreCase(args[0])) {
            if (args.length == 2) {
                StringUtil.copyPartialMatches(args[1], Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), matches);
            } else if (args.length == 3) {
                StringUtil.copyPartialMatches(args[2], Arrays.stream(EntityType.values())
                        .filter(EntityType::isAlive)
                        .map(type -> type.name().toLowerCase(Locale.ROOT))
                        .toList(), matches);
            } else if (args.length == 4) {
                StringUtil.copyPartialMatches(args[3], List.of("true", "false", "default"), matches);
            }
        }
        return matches;
    }
}
