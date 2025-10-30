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
import org.bukkit.event.entity.EntityCombustByBlockEvent; 
import org.bukkit.event.entity.EntityCombustByEntityEvent;
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
    // --- (แก้ไข) onEntitySpawn ทั้งหมด ---
    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        
        if (event.getEntityType() == EntityType.ZOMBIE) {
            
            Zombie zombie = (Zombie) event.getEntity();
            
            // --- (เพิ่มใหม่) ดึงค่า Moon Phase ---
            ZombieApocalypse.MoonPhase moon = plugin.getCurrentMoonPhase();
            // --- (จบส่วนเพิ่มใหม่) ---


            // --- (P1) Base Buffs ---
            zombie.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(30.0);
            zombie.setHealth(30.0);
            zombie.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.26);
            zombie.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(4.0);
            zombie.setCanBreakDoors(true);
            zombie.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(50.0);


            // --- (เพิ่มใหม่) ตรวจสอบ Moon Phase ---
            if (moon == ZombieApocalypse.MoonPhase.RED_MOON) {
                // ตีแรงขึ้น (เพิ่มพลังโจมตี +3)
                zombie.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(7.0); 
            } 
            else if (moon == ZombieApocalypse.MoonPhase.BLUE_MOON) {
                // อ่อนแอลง (1-2 ทีตาย)
                zombie.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(2.0); // 1 หัวใจ
                zombie.setHealth(2.0);
                // ดาเมจ 1-2 (0.5 - 1 หัวใจ)
                zombie.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(1.0); // 0.5 หัวใจ
            }
            // --- (จบส่วนเพิ่มใหม่) ---


            // --- (P5) Special Infected Chance (20% total) ---
            
            // (แก้ไข) ถ้าเป็น Blue Moon จะไม่มีซอมบี้พิเศษ
            if (moon != ZombieApocalypse.MoonPhase.BLUE_MOON) {
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
                
                // (ถ้าเป็น Red Moon อาจจะบัฟซอมบี้พิเศษเพิ่มอีกก็ได้)
                if (moon == ZombieApocalypse.MoonPhase.RED_MOON && zombie.getCustomName() != null) {
                     zombie.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(
                         zombie.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getBaseValue() + 2.0 // ตีแรงขึ้นอีก
                     );
                }
            }
        }
    }
    // --- (จบส่วนแก้ไข) ---

    // --- (P1) Sun Proofing (แก้ไข) ---
    @EventHandler
    public void onZombieBurn(EntityCombustEvent event) {
        
        if (event.getEntityType() != EntityType.ZOMBIE) {
            return;
        }
        if (event instanceof EntityCombustByBlockEvent) {
            return;
        }
        if (event instanceof EntityCombustByEntityEvent) {
            return;
        }
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
