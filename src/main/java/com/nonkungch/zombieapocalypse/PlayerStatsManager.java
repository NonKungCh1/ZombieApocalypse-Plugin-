package com.nonkungch.zombieapocalypse;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

// --- (เพิ่ม Import 4 บรรทัดนี้) ---
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
// --- (จบส่วน Import) ---

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerStatsManager {

    private ZombieApocalypse plugin;
    
    // คลาสสำหรับเก็บข้อมูล (เหมือนเดิม)
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

    // --- (เพิ่มตัวแปร 2 บรรทัดนี้) ---
    private File playerDataFile;
    private FileConfiguration playerDataConfig;

    public PlayerStatsManager(ZombieApocalypse plugin) {
        this.plugin = plugin;
        // --- (เพิ่ม 2 บรรทัดนี้) ---
        setupDataFile(); // เรียกใช้เมธอดสร้าง/โหลดไฟล์
        loadAllPlayerDataFromFile(); // โหลดข้อมูลผู้เล่นทั้งหมดจากไฟล์มาเก็บใน Map
    }
    
    // --- (เมธอดใหม่: สำหรับจัดการไฟล์ YML) ---
    private void setupDataFile() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }
        playerDataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!playerDataFile.exists()) {
            try {
                playerDataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create playerdata.yml!");
                e.printStackTrace();
            }
        }
        playerDataConfig = YamlConfiguration.loadConfiguration(playerDataFile);
    }

    // --- (เมธอดใหม่: โหลดข้อมูลจากไฟล์มาใส่ Map ตอนเริ่มปลั๊กอิน) ---
    private void loadAllPlayerDataFromFile() {
        if (playerDataConfig == null) return;
        
        for (String uuidString : playerDataConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidString);
                String path = uuidString + ".";
                
                PlayerStats stats = new PlayerStats();
                stats.healthLevel = playerDataConfig.getInt(path + "healthLevel", 0);
                stats.speedLevel = playerDataConfig.getInt(path + "speedLevel", 0);
                stats.damageLevel = playerDataConfig.getInt(path + "damageLevel", 0);
                stats.regenLevel = playerDataConfig.getInt(path + "regenLevel", 0);
                stats.upgradePoints = playerDataConfig.getInt(path + "upgradePoints", 0);
                
                playerStatsMap.put(uuid, stats);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load stats for UUID: " + uuidString);
            }
        }
        plugin.getLogger().info("Loaded stats for " + playerStatsMap.size() + " players.");
    }
    
    // --- (เมธอดใหม่: เซฟข้อมูลผู้เล่นคนเดียวลงไฟล์) ---
    public void savePlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerStats stats = playerStatsMap.get(uuid);
        
        if (stats == null) return; // ไม่มีข้อมูลให้เซฟ

        String path = uuid.toString() + ".";
        playerDataConfig.set(path + "healthLevel", stats.healthLevel);
        playerDataConfig.set(path + "speedLevel", stats.speedLevel);
        playerDataConfig.set(path + "damageLevel", stats.damageLevel);
        playerDataConfig.set(path + "regenLevel", stats.regenLevel);
        playerDataConfig.set(path + "upgradePoints", stats.upgradePoints);

        try {
            playerDataConfig.save(playerDataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save player data for " + player.getName());
            e.printStackTrace();
        }
    }
    
    // --- (เมธอดใหม่: เซฟข้อมูลผู้เล่น *ทุกคน* ตอนปิดเซิร์ฟ) ---
    public void saveAllPlayerData() {
        if (playerDataConfig == null || playerStatsMap.isEmpty()) return;
        
        for (UUID uuid : playerStatsMap.keySet()) {
             PlayerStats stats = playerStatsMap.get(uuid);
             String path = uuid.toString() + ".";
             playerDataConfig.set(path + "healthLevel", stats.healthLevel);
             playerDataConfig.set(path + "speedLevel", stats.speedLevel);
             playerDataConfig.set(path + "damageLevel", stats.damageLevel);
             playerDataConfig.set(path + "regenLevel", stats.regenLevel);
             playerDataConfig.set(path + "upgradePoints", stats.upgradePoints);
        }
        
        try {
            playerDataConfig.save(playerDataFile);
            plugin.getLogger().info("Saved all player stats (" + playerStatsMap.size() + " entries).");
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save all player data!");
            e.printStackTrace();
        }
    }

    // --- (แก้ไข) โหลด/ล้าง ข้อมูล ---
    
    public void loadPlayer(Player player) {
        // (แก้ไข) ดึงข้อมูลจาก Map (ที่โหลดจากไฟล์มาแล้ว)
        PlayerStats stats = playerStatsMap.get(player.getUniqueId());
        
        // ถ้าไม่มีข้อมูลใน Map (ผู้เล่นใหม่)
        if (stats == null) {
            stats = new PlayerStats();
            playerStatsMap.put(player.getUniqueId(), stats); // สร้างโปรไฟล์ใหม่ใน Memory
            plugin.getLogger().info("Created new stats profile for " + player.getName());
        } else {
            plugin.getLogger().info("Loaded stats for " + player.getName());
        }
        
        applyStats(player);
        // เติมเลือดให้เต็มเมื่อเข้าเกม
        player.setHealth(stats.getMaxHealth());
    }

    public void unloadPlayer(Player player) {
        // (แก้ไข) เปลี่ยนจาก "ลบ" เป็น "เซฟ"
        savePlayerData(player); // <--- เซฟข้อมูลลงไฟล์ playerdata.yml
        
        // ลบออกจาก Map เพื่อประหยัด Memory (เมื่อเขาออกเกมไปแล้ว)
        playerStatsMap.remove(player.getUniqueId());
        plugin.getLogger().info("Saved and unloaded stats for " + player.getName());
    }

    public PlayerStats getPlayerStats(Player player) {
        return playerStatsMap.get(player.getUniqueId());
    }

    // --- (โค้ดส่วนที่เหลือเหมือนเดิมทุกประการ) ---
    
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
    
    public void addUpgradePoint(Player player, int amount) {
        PlayerStats stats = getPlayerStats(player);
        if (stats == null) return;
        
        stats.upgradePoints += amount;
        player.sendMessage(ChatColor.GREEN + "+1 Upgrade Point! " + ChatColor.GRAY + "(แต้มทั้งหมด: " + stats.upgradePoints + ")");
        player.sendMessage(ChatColor.GREEN + "พิมพ์ " + ChatColor.AQUA + "/zinfo" + ChatColor.GREEN + " เพื่อดูสถานะ");
    }

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
                    
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 120, stats.regenLevel - 1, true, false));
                }
            }
        }.runTaskTimer(plugin, 0L, 100L); // ทำงานทุก 5 วินาที
    }
}
