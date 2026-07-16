package me.BigBou.rideableMobs.listener;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import me.BigBou.rideableMobs.util.Util;
import org.bukkit.ChatColor;
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
import org.bukkit.entity.SkeletonHorse;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.ZombieHorse;
import org.bukkit.entity.EnderDragon.Phase;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.event.entity.EntityDismountEvent;

public class PlayerListener implements Listener {
    private final JavaPlugin plugin;
    private final HashMap<Material, Collection<PotionEffect>> foodEffects = new HashMap();

    public PlayerListener(JavaPlugin plugin) {
        this.plugin = plugin;
        this.foodEffects.put(Material.ENCHANTED_GOLDEN_APPLE, List.of(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 3), new PotionEffect(PotionEffectType.REGENERATION, 600, 1), new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 6000, 0), new PotionEffect(PotionEffectType.RESISTANCE, 6000, 0)));
        this.foodEffects.put(Material.GOLDEN_APPLE, List.of(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 0), new PotionEffect(PotionEffectType.REGENERATION, 100, 1)));
    }

    @EventHandler
    public void onInteractAtEntity(PlayerInteractAtEntityEvent e) {
        EquipmentSlot hand = e.getHand();
        if (hand == EquipmentSlot.HAND) {
            Entity entity = e.getRightClicked();
            if (entity.getPassengers().size() < 1 && (!(entity instanceof Vehicle) || entity instanceof ZombieHorse || entity instanceof SkeletonHorse)) {
                Player player = e.getPlayer();
                if (Util.isWorldEnabled(player)) {
                    if (me.BigBou.rideableMobs.command.RideableMobsCommand.isDisabled(player.getUniqueId())) {
                        String message = this.plugin.getConfig().getString("plugin-disabled-player", "&cYou have disabled RideableMobs.");
                        if (!message.isEmpty()) {
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
                        }
                        return;
                    }
                    EntityType type = entity.getType();
                    String entityType = !(entity instanceof ComplexEntityPart) && !(entity instanceof ComplexLivingEntity) ? type.name().toLowerCase() : "ender_dragon";
                    boolean isEnabled = !(entity instanceof ComplexEntityPart) && !(entity instanceof ComplexLivingEntity) ? this.plugin.getConfig().getBoolean(entityType) : this.plugin.getConfig().getBoolean("ender_dragon");
                    if (!isEnabled) {
                        String message = this.plugin.getConfig().getString("entity-disabled");

                        assert message != null;

                        if (!message.isEmpty()) {
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
                        }

                    } else if (!player.hasPermission("rideablemobs.ride.*") && !player.hasPermission("rideablemobs.ride." + entityType)) {
                        String message = this.plugin.getConfig().getString("no-permission-ride");

                        assert message != null;

                        if (!message.isEmpty()) {
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
                        }

                    } else {
                        boolean shouldBeEmptyHand = this.plugin.getConfig().getBoolean("requires-empty-hand");
                        if (shouldBeEmptyHand && !player.getInventory().getItemInMainHand().getType().isAir()) {
                            ItemStack stack = player.getInventory().getItemInMainHand();
                            Material material = stack.getType();
                            if (material.isEdible()) {
                                LivingEntity livingEntity = (LivingEntity)entity;
                                AttributeInstance ai = livingEntity.getAttribute(Attribute.MAX_HEALTH);
                                switch (material) {
                                    case ENCHANTED_GOLDEN_APPLE:
                                        livingEntity.addPotionEffects((Collection)this.foodEffects.get(Material.ENCHANTED_GOLDEN_APPLE));

                                        assert ai != null;

                                        livingEntity.setHealth(ai.getValue());
                                        player.getWorld().spawnParticle(Particle.HEART, livingEntity.getEyeLocation(), 5);
                                        if (!player.getGameMode().equals(GameMode.CREATIVE)) {
                                            stack.setAmount(stack.getAmount() - 1);
                                        }
                                        break;
                                    case GOLDEN_APPLE:
                                        livingEntity.addPotionEffects((Collection)this.foodEffects.get(Material.GOLDEN_APPLE));

                                        assert ai != null;

                                        livingEntity.setHealth(Math.min(livingEntity.getHealth() + (double)10.0F, ai.getValue()));
                                        player.getWorld().spawnParticle(Particle.HEART, livingEntity.getEyeLocation(), 3);
                                        if (!player.getGameMode().equals(GameMode.CREATIVE)) {
                                            stack.setAmount(stack.getAmount() - 1);
                                        }
                                        break;
                                    case APPLE:
                                        assert ai != null;

                                        livingEntity.setHealth(Math.min(livingEntity.getHealth() + (double)3.0F, ai.getValue()));
                                        player.getWorld().spawnParticle(Particle.HEART, livingEntity.getEyeLocation(), 1);
                                        if (!player.getGameMode().equals(GameMode.CREATIVE)) {
                                            stack.setAmount(stack.getAmount() - 1);
                                        }
                                }
                            }

                        } else {
                            if (entity instanceof ArmorStand && !player.isSneaking()) {
                                entity.addPassenger(player);
                            } else if (entity instanceof ComplexEntityPart) {
                                ComplexEntityPart entityPart = (ComplexEntityPart)entity;
                                ComplexLivingEntity parent = entityPart.getParent();
                                EnderDragon enderDragon = (EnderDragon)parent;
                                enderDragon.setPhase(Phase.FLY_TO_PORTAL);
                                enderDragon.addPassenger(player);
                            } else if (!(entity instanceof ArmorStand)) {
                                entity.addPassenger(player);
                            }

                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onDismount(EntityDismountEvent e) {
        Entity vehicle = e.getEntity();
        if (vehicle instanceof Player player) {
            vehicle = e.getDismounted();
            if (vehicle instanceof EnderDragon enderDragon) {
                enderDragon.setPhase(Phase.HOVER);
            }

            if (Util.canSwim(vehicle) && vehicle.isInWater() && !player.isSneaking()) {
                e.setCancelled(true);
            }

        }
    }
}
