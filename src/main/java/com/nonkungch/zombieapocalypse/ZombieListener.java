package com.nonkungch.zombieapocalypse;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityCombustByBlockEvent; // (แก้ไข)
import org.bukkit.event.entity.EntityCombustByEntityEvent; // (แก้ไข)
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.attribute.Attribute;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

public class ZombieListener implements Listener {

    private ZombieApocalypse plugin;
    private Random random = new Random();

    public ZombieListener(ZombieApocalypse plugin) {
        this.plugin = plugin;
    }

    // --- (P1) Zombie Stats & (P5) Special Infected ---
    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        
        if (event.getEntityType() == EntityType.ZOMBIE) {
            
            Zombie zombie = (Zombie) event.getEntity();

            // --- (P1) Base Buffs ---
            zombie.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(30.0);
            zombie.setHealth(30.0);
            zombie.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.26);
            zombie.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(4.0);
            zombie.setCanBreakDoors(true);
            zombie.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(50.0);

            // --- (P5) Special Infected Chance (20% total) ---
            double chance = random.nextDouble(); 

            if (chance < 0.07) {
                // --- Sprinter (7% chance) ---
                zombie.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.40);
                zombie.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20.0);
                zombie.setHealth(20.0);
                zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, false, false));
                zombie.setCustomName(ChatColor.AQUA + "Sprinter");
                zombie.setCustomNameVisible(true);

            } else if (chance < 0.14) {
                // --- Tank (7% chance) ---
                zombie.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(80.0);
                zombie.setHealth(80.0);
                zombie.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.20);
                zombie.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(8.0);
                zombie.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(1.0);
                zombie.setCustomName(ChatColor.DARK_RED + "Tank");
                zombie.setCustomNameVisible(true);

            } else if (chance < 0.20) {
                // --- Boomer (6% chance) ---
                zombie.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(40.0);
                zombie.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.22);
                zombie.setCustomName(ChatColor.DARK_GREEN + "Boomer");
                zombie.setCustomNameVisible(true);
                plugin.addBoomer(zombie.getUniqueId());
            }
        }
    }

    // --- (P1) Sun Proofing (แก้ไข) ---
    @EventHandler
    public void onZombieBurn(EntityCombustEvent event) {
        
        if (event.getEntityType() != EntityType.ZOMBIE) {
            return;
        }
        // (กันการเผาไหม้จาก ไฟ/ลาวา)
        if (event instanceof EntityCombustByBlockEvent) {
            return;
        }
        // (กันการเผาไหม้จาก Entity อื่น)
        if (event instanceof EntityCombustByEntityEvent) {
            return;
        }
        // ถ้าเป็นการเผาไหม้แบบปกติ (แสงแดด)
        event.setCancelled(true);
    }

    // --- (P5) Boomer Explosion ---
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (plugin.isBoomer(event.getEntity().getUniqueId())) {
            
            plugin.removeBoomer(event.getEntity().getUniqueId());
            Location loc = event.getEntity().getLocation();
            
            loc.getWorld().createExplosion(loc, 3.0f, false, true);
            
            for (Entity nearby : loc.getWorld().getNearbyEntities(loc, 6, 6, 6)) {
                if (nearby instanceof Player) {
                    Player player = (Player) nearby;
                    
                    if (!plugin.getInfectedPlayers().containsKey(player.getUniqueId())) {
                        plugin.getInfectedPlayers().put(player.getUniqueId(), System.currentTimeMillis());
                        player.sendMessage(ChatColor.DARK_RED + "คุณโดนสารคัดหลั่งของ Boomer! คุณติดเชื้อแล้ว!");
                    }
                }
            }
        }
    }
}