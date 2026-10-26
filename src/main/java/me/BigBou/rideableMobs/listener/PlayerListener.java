package me.BigBou.rideableMobs.listener;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import me.BigBou.rideableMobs.RideableMobs;
import me.BigBou.rideableMobs.util.Util;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.ComplexEntityPart;
import org.bukkit.entity.ComplexLivingEntity;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.EnderDragon.Phase;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import me.BigBou.rideableMobs.util.LanguageManager;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.event.entity.EntityDismountEvent;

public class PlayerListener implements Listener {
    private final RideableMobs plugin;
    private final LanguageManager lang;
    private final HashMap<Material, Collection<PotionEffect>> foodEffects = new HashMap<>();

    public PlayerListener(RideableMobs plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
        this.foodEffects.put(Material.ENCHANTED_GOLDEN_APPLE, List.of(
                new PotionEffect(PotionEffectType.ABSORPTION, 2400, 3),
                new PotionEffect(PotionEffectType.REGENERATION, 600, 1),
                new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 6000, 0),
                new PotionEffect(PotionEffectType.RESISTANCE, 6000, 0)));
        this.foodEffects.put(Material.GOLDEN_APPLE, List.of(
                new PotionEffect(PotionEffectType.ABSORPTION, 2400, 0),
                new PotionEffect(PotionEffectType.REGENERATION, 100, 1)));
    }

    private LivingEntity resolveLivingEntity(Entity entity) {
        if (entity instanceof LivingEntity living) {
            return living;
        }
        if (entity instanceof ComplexEntityPart part && part.getParent() instanceof LivingEntity parent) {
            return parent;
        }
        return null;
    }

    private boolean rideOrFeed(Player player, Entity entity) {
        boolean shouldBeEmptyHand = this.plugin.getConfig().getBoolean("requires-empty-hand", true);
        if (shouldBeEmptyHand && !player.getInventory().getItemInMainHand().getType().isAir()) {
            ItemStack stack = player.getInventory().getItemInMainHand();
            Material material = stack.getType();
            if (material.isEdible()) {
                LivingEntity livingEntity = resolveLivingEntity(entity);
                if (livingEntity == null) {
                    return false;
                }
                AttributeInstance ai = livingEntity.getAttribute(Attribute.MAX_HEALTH);
                if (ai == null) {
                    return false;
                }
                double maxHealth = ai.getValue();
                switch (material) {
                    case ENCHANTED_GOLDEN_APPLE:
                        livingEntity.addPotionEffects(this.foodEffects.get(Material.ENCHANTED_GOLDEN_APPLE));
                        livingEntity.setHealth(maxHealth);
                        player.getWorld().spawnParticle(Particle.HEART, livingEntity.getEyeLocation(), 5);
                        if (!player.getGameMode().equals(GameMode.CREATIVE)) {
                            stack.setAmount(stack.getAmount() - 1);
                        }
                        return true;
                    case GOLDEN_APPLE:
                        livingEntity.addPotionEffects(this.foodEffects.get(Material.GOLDEN_APPLE));
                        livingEntity.setHealth(Math.min(livingEntity.getHealth() + 10.0, maxHealth));
                        player.getWorld().spawnParticle(Particle.HEART, livingEntity.getEyeLocation(), 3);
                        if (!player.getGameMode().equals(GameMode.CREATIVE)) {
                            stack.setAmount(stack.getAmount() - 1);
                        }
                        return true;
                    case APPLE:
                        livingEntity.setHealth(Math.min(livingEntity.getHealth() + 3.0, maxHealth));
                        player.getWorld().spawnParticle(Particle.HEART, livingEntity.getEyeLocation(), 1);
                        if (!player.getGameMode().equals(GameMode.CREATIVE)) {
                            stack.setAmount(stack.getAmount() - 1);
                        }
                        return true;
                    default:
                        return false;
                }
            }
            return false;
        }

        if (entity instanceof ArmorStand && !player.isSneaking()) {
            entity.addPassenger(player);
            return true;
        }
        if (entity instanceof ComplexEntityPart part) {
            ComplexLivingEntity parent = part.getParent();
            if (parent instanceof EnderDragon enderDragon) {
                enderDragon.setPhase(Phase.FLY_TO_PORTAL);
            }
            parent.addPassenger(player);
            return true;
        }
        if (entity instanceof EnderDragon enderDragon) {
            enderDragon.setPhase(Phase.FLY_TO_PORTAL);
            enderDragon.addPassenger(player);
            return true;
        }
        if (!(entity instanceof ArmorStand)) {
            entity.addPassenger(player);
            return true;
        }
        return false;
    }

    @EventHandler
    public void onInteractAtEntity(PlayerInteractAtEntityEvent e) {
        EquipmentSlot hand = e.getHand();
        if (hand != EquipmentSlot.HAND) {
            return;
        }

        Entity entity = e.getRightClicked();
        if (entity.getPassengers().isEmpty() && Util.isAllowedVehicle(entity)) {
            Player player = e.getPlayer();
            if (Util.isWorldEnabled(player)) {
                if (me.BigBou.rideableMobs.command.RideableMobsCommand.isDisabled(player.getUniqueId())) {
                    if (this.plugin.getConfig().getBoolean("plugin-disabled-player-notify")) {
                        lang.send(player, "plugin-disabled-player");
                    }
                    return;
                }

                EntityType type = entity.getType();
                String entityType = !(entity instanceof ComplexEntityPart) && !(entity instanceof ComplexLivingEntity)
                        ? type.name().toLowerCase(Locale.ROOT)
                        : "ender_dragon";

                boolean isEnabled = !(entity instanceof ComplexEntityPart) && !(entity instanceof ComplexLivingEntity)
                        ? this.plugin.getConfig().getBoolean(entityType, !"player".equals(entityType))
                        : this.plugin.getConfig().getBoolean("ender_dragon", true);

                if (me.BigBou.rideableMobs.command.RideableMobsCommand.isExplicitlyDenied(player, entityType)) {
                    lang.send(player, "no-permission-ride");
                } else if (me.BigBou.rideableMobs.command.RideableMobsCommand.isExplicitlyGranted(player, entityType)) {
                    if (rideOrFeed(player, entity)) {
                        e.setCancelled(true);
                    }
                } else if (!isEnabled) {
                    if (this.plugin.getConfig().getBoolean("entity-disabled-notify")) {
                        lang.send(player, "entity-disabled");
                    }
                } else if (!me.BigBou.rideableMobs.command.RideableMobsCommand.hasBukkitPermission(player, entityType)) {
                    if (this.plugin.getConfig().getBoolean("no-permission-notify")) {
                        lang.send(player, "no-permission-ride");
                    }
                } else {
                    if (rideOrFeed(player, entity)) {
                        e.setCancelled(true);
                    }
                }
            }
        }
    }

    // A right click sends interact_at then interact; the mount from the first must not also run vanilla behaviour.
    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        Entity clicked = e.getRightClicked();
        if (clicked.getPassengers().contains(e.getPlayer()) && Util.isAllowedVehicle(clicked)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDismount(EntityDismountEvent e) {
        if (e.getEntity() instanceof Player player) {
            Entity vehicle = e.getDismounted();
            // toggle-off must always dismount, even from a swimming mob
            if (Util.canSwim(vehicle) && vehicle.isInWater() && !player.isSneaking()
                    && !me.BigBou.rideableMobs.command.RideableMobsCommand.isDisabled(player.getUniqueId())) {
                e.setCancelled(true);
                return;
            }

            if (vehicle instanceof EnderDragon enderDragon) {
                enderDragon.setPhase(Phase.HOVER);
            }
        }
    }
}
