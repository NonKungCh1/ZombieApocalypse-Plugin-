package com.nonkungch.zombieapocalypse;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerStatsListener implements Listener {

    private ZombieApocalypse plugin;
    private PlayerStatsManager statsManager;

    public PlayerStatsListener(ZombieApocalypse plugin) {
        this.plugin = plugin;
        this.statsManager = plugin.getPlayerStatsManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // (A1) ตั้งค่าเลือด 2 แถว (40 HP) เป็นค่าเริ่มต้น
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(40.0);
        // (A2) โหลด/สร้าง Stats
        statsManager.loadPlayer(player);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        // (A1) ตั้งค่าเลือด 2 แถว หลังเกิด
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(40.0);
        
        // (A2) ใช้ Stats อีกครั้ง
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            statsManager.applyStats(player);
            // เติมเลือดให้เต็มหลังเกิด
            player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue()); 
        }, 1L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // (A2) ล้างข้อมูลผู้เล่นออกจาก memory
        statsManager.unloadPlayer(event.getPlayer());
    }

    // (A2) ระบบรับแต้มอัปเกรด
    @EventHandler
    public void onMonsterKill(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null) {
            return;
        }
        
        if (!(event.getEntity() instanceof Monster)) {
            return;
        }

        Player player = event.getEntity().getKiller();

        // โอกาส 10% ที่จะได้รับ 1 แต้ม
        if (Math.random() <= 0.10) {
            statsManager.addUpgradePoint(player, 1);
        }
    }
}