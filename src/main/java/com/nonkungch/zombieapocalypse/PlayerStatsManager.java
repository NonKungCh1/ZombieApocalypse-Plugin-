package com.nonkungch.zombieapocalypse;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerStatsManager {

    private ZombieApocalypse plugin;
    
    // คลาสสำหรับเก็บข้อมูล
    public class PlayerStats {
        int healthLevel = 0;
        int speedLevel = 0;
        int damageLevel = 0;
        int regenLevel = 0;
        int upgradePoints = 0;
        
        final int MAX_LEVEL = 10;
        
        public double getMaxHealth() {
            return 40.0 + (healthLevel * 2.0); // เริ่มที่ 40 (2 แถว)
        }
        public double getMovementSpeed() {
            return 0.1 + (speedLevel * 0.005); // (Base 0.1)
        }
        public double getAttackDamage() {
            return 1.0 + (damageLevel * 0.5); // (Base 1.0)
        }
    }

    private Map<UUID, PlayerStats> playerStatsMap = new HashMap<>();

    public PlayerStatsManager(ZombieApocalypse plugin) {
        this.plugin = plugin;
    }

    // --- โหลด/ล้าง ข้อมูล ---
    
    public void loadPlayer(Player player) {
        // (ในอนาคต ตรงนี้คือส่วนที่โหลดจากไฟล์)
        PlayerStats stats = playerStatsMap.getOrDefault(player.getUniqueId(), new PlayerStats());
        playerStatsMap.put(player.getUniqueId(), stats);
        
        applyStats(player);
        // เติมเลือดให้เต็มเมื่อเข้าเกม
        player.setHealth(stats.getMaxHealth());
    }

    public void unloadPlayer(Player player) {
        // (ในอนาคต ตรงนี้คือส่วนที่เซฟลงไฟล์)
        playerStatsMap.remove(player.getUniqueId());
    }

    public PlayerStats getPlayerStats(Player player) {
        return playerStatsMap.get(player.getUniqueId());
    }

    // --- ใช้ Stats กับ Player ---
    
    public void applyStats(Player player) {
        PlayerStats stats = getPlayerStats(player);
        if (stats == null) return;

        AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        AttributeInstance moveSpeed = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        AttributeInstance attackDamage = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        
        if (maxHealth != null) maxHealth.setBaseValue(stats.getMaxHealth());
        if (moveSpeed != null) moveSpeed.setBaseValue(stats.getMovementSpeed());
        if (attackDamage != null) attackDamage.setBaseValue(stats.getAttackDamage());
    }
    
    // --- ระบบแต้ม ---
    
    public void addUpgradePoint(Player player, int amount) {
        PlayerStats stats = getPlayerStats(player);
        if (stats == null) return;
        
        stats.upgradePoints += amount;
        player.sendMessage(ChatColor.GREEN + "+1 Upgrade Point! " + ChatColor.GRAY + "(แต้มทั้งหมด: " + stats.upgradePoints + ")");
        player.sendMessage(ChatColor.GREEN + "พิมพ์ " + ChatColor.AQUA + "/zinfo" + ChatColor.GREEN + " เพื่อดูสถานะ");
    }

    // --- ระบบอัปเกรด ---
    
    public boolean upgradeStat(Player player, String statName) {
        PlayerStats stats = getPlayerStats(player);
        if (stats == null) return false;
        
        if (stats.upgradePoints <= 0) {
            player.sendMessage(ChatColor.RED + "คุณไม่มีแต้มอัปเกรดเหลือ!");
            return false;
        }

        boolean success = false;
        
        switch (statName.toLowerCase()) {
            case "health":
                if (stats.healthLevel < stats.MAX_LEVEL) {
                    stats.healthLevel++;
                    success = true;
                }
                break;
            case "speed":
                if (stats.speedLevel < stats.MAX_LEVEL) {
                    stats.speedLevel++;
                    success = true;
                }
                break;
            case "damage":
                if (stats.damageLevel < stats.MAX_LEVEL) {
                    stats.damageLevel++;
                    success = true;
                }
                break;
            case "regen":
                 if (stats.regenLevel < stats.MAX_LEVEL) {
                    stats.regenLevel++;
                    success = true;
                }
                break;
            default:
                player.sendMessage(ChatColor.RED + "ไม่พบสถานะ: " + statName);
                return false;
        }

        if (success) {
            stats.upgradePoints--;
            applyStats(player);
            player.sendMessage(ChatColor.GREEN + "อัปเกรด " + statName + " สำเร็จ! (Level " + getStatLevel(stats, statName) + ")");
            player.sendMessage(ChatColor.GRAY + "แต้มคงเหลือ: " + stats.upgradePoints);
        } else {
            player.sendMessage(ChatColor.RED + "สถานะ " + statName + " ตันแล้ว (Max Lvl " + stats.MAX_LEVEL + ")");
        }
        return success;
    }
    
    public int getStatLevel(PlayerStats stats, String statName) {
         switch (statName.toLowerCase()) {
            case "health": return stats.healthLevel;
            case "speed": return stats.speedLevel;
            case "damage": return stats.damageLevel;
            case "regen": return stats.regenLevel;
            default: return 0;
         }
    }

    // --- Task สำหรับ Regen (A2) ---
    public void startRegenTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID playerId : playerStatsMap.keySet()) {
                    Player player = Bukkit.getPlayer(playerId);
                    PlayerStats stats = playerStatsMap.get(playerId);
                    
                    if (player == null || !player.isOnline() || stats.regenLevel == 0) {
                        continue;
                    }
                    
                    // Level 1 = Regen 0 (I)
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 120, stats.regenLevel - 1, true, false));
                }
            }
        }.runTaskTimer(plugin, 0L, 100L); // ทำงานทุก 5 วินาที
    }
}