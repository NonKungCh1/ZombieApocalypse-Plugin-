package com.nonkungch.zombieapocalypse;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class InfectionTask extends BukkitRunnable {

    private ZombieApocalypse plugin;

    public InfectionTask(ZombieApocalypse plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        // ใช้ .keySet() เพื่อให้ลบได้ปลอดภัย
        for (UUID playerId : plugin.getInfectedPlayers().keySet()) {
            
            Player player = Bukkit.getPlayer(playerId);

            if (player == null || !player.isOnline() || player.isDead() || player.getGameMode() == GameMode.CREATIVE) {
                continue; 
            }

            long timeInfectedStart = plugin.getInfectedPlayers().get(playerId);
            long timeElapsedMillis = System.currentTimeMillis() - timeInfectedStart;
            int secondsElapsed = (int) (timeElapsedMillis / 1000);
            
            int stage1Time = 60;  // 1 นาที
            int stage2Time = 180; // 3 นาที
            int stage3Time = 300; // 5 นาที
            int deathTime = 420;  // 7 นาที

            if (secondsElapsed >= deathTime) {
                // Stage 4: Death
                if (secondsElapsed == deathTime) { // (FIX F1) ส่งข้อความแค่ครั้งเดียว
                    player.sendMessage(ChatColor.DARK_RED + "การติดเชื้อได้ครอบงำร่างกายคุณแล้ว...");
                }
                player.setHealth(0.0);
                plugin.getInfectedPlayers().remove(playerId);
                
            } else if (secondsElapsed >= stage3Time) {
                // Stage 3: Critical
                if (secondsElapsed == stage3Time) { // (FIX F1)
                    player.sendMessage(ChatColor.RED + "ร่างกายของคุณกำลังล้มเหลว...");
                }
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 40, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 0));
                
            } else if (secondsElapsed >= stage2Time) {
                // Stage 2: Worsening
                if (secondsElapsed == stage2Time) { // (FIX F1)
                    player.sendMessage(ChatColor.GOLD + "คุณรู้สึกเจ็บปวดไปทั่วร่าง...");
                }
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 40, 0));
                
            } else if (secondsElapsed >= stage1Time) {
                // Stage 1: Infected
                if (secondsElapsed == stage1Time) { // (FIX F1)
                    player.sendMessage(ChatColor.YELLOW + "คุณรู้สึกไม่ค่อยสบายตัวและหิวโหย...");
                }
                player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 40, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0)); // (แก้ไขแล้ว)
            }
        }
    }
}