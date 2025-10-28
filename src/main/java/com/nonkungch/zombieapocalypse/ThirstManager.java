package com.nonkungch.zombieapocalypse;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ThirstManager {

    private ZombieApocalypse plugin;
    private Map<UUID, Integer> playerThirst = new HashMap<>();
    private Map<UUID, BossBar> playerBossBars = new HashMap<>();
    private final int MAX_THIRST = 100;
    private final int MIN_THIRST = 0;
    private BukkitTask thirstTask;

    public ThirstManager(ZombieApocalypse plugin) {
        this.plugin = plugin;
    }

    // --- Boss Bar Management ---
    public void addPlayer(Player player) {
        playerThirst.put(player.getUniqueId(), MAX_THIRST);
        BossBar bossBar = Bukkit.createBossBar(ChatColor.AQUA + "ความกระหายน้ำ", BarColor.BLUE, BarStyle.SOLID);
        bossBar.addPlayer(player);
        playerBossBars.put(player.getUniqueId(), bossBar);
        updateBossBar(player);
    }

    public void removePlayer(Player player) {
        playerThirst.remove(player.getUniqueId());
        BossBar bossBar = playerBossBars.remove(player.getUniqueId());
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }
    
    public void removeAllBossBars() {
        for (BossBar bossBar : playerBossBars.values()) {
            bossBar.removeAll();
        }
        playerBossBars.clear();
    }

    public void updateBossBar(Player player) {
        BossBar bossBar = playerBossBars.get(player.getUniqueId());
        int thirst = getThirst(player.getUniqueId());
        
        if (bossBar == null) return;

        bossBar.setProgress((double) thirst / MAX_THIRST);

        if (thirst <= 20) {
            bossBar.setColor(BarColor.RED);
            bossBar.setTitle(ChatColor.RED + "ขาดน้ำ");
        } else if (thirst <= 50) {
            bossBar.setColor(BarColor.YELLOW);
            bossBar.setTitle(ChatColor.YELLOW + "กระหายน้ำ");
        } else {
            bossBar.setColor(BarColor.BLUE);
            bossBar.setTitle(ChatColor.AQUA + "ความกระหายน้ำ");
        }
    }

    // --- Thirst Data Management ---
    public int getThirst(UUID playerId) {
        return playerThirst.getOrDefault(playerId, MAX_THIRST);
    }

    public void decreaseThirst(Player player, int amount) {
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            setThirst(player, MAX_THIRST);
            return;
        }
        setThirst(player, getThirst(player.getUniqueId()) - amount);
    }

    public void increaseThirst(Player player, int amount) {
        setThirst(player, getThirst(player.getUniqueId()) + amount);
        player.sendMessage(ChatColor.AQUA + "คุณรู้สึกสดชื่นขึ้น!");
    }

    private void setThirst(Player player, int amount) {
        int newThirst = Math.max(MIN_THIRST, Math.min(MAX_THIRST, amount));
        playerThirst.put(player.getUniqueId(), newThirst);
        updateBossBar(player);
    }

    // --- Main Thirst Task ---
    public void startThirstTask() {
        // Runs every 100 ticks (5 seconds)
        thirstTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
                    continue;
                }
                
                // Base thirst decrease
                decreaseThirst(player, 1); 

                // --- Apply Effects ---
                int currentThirst = getThirst(player.getUniqueId());
                if (currentThirst <= 20) {
                    // Dehydrated
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 30, 0)); // 1.5 sec
                } else if (currentThirst <= 50) {
                    // Thirsty
                    // --- FIX ---
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 110, 0)); // 5.5 sec <--- แก้ไขจาก SLOW
                    // --- END FIX ---
                }
            }
        }, 0L, 100L);
    }
}
