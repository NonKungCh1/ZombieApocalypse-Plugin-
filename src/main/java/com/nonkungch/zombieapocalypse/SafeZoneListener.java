package com.nonkungch.zombieapocalypse;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class SafeZoneListener implements Listener {

    private ZombieApocalypse plugin;
    private SafeZoneManager szManager;

    public SafeZoneListener(ZombieApocalypse plugin) {
        this.plugin = plugin;
        this.szManager = plugin.getSafeZoneManager();
    }

    // --- 1. Handle "Zone Definer" (1007) clicks ---
    @EventHandler
    public void onPlayerUseDefiner(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasCustomModelData() ||
            item.getItemMeta().getCustomModelData() != 1007) {
            return;
        }

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true); // Prevent breaking block
            Location pos1 = event.getClickedBlock().getLocation();
            szManager.setPlayerSelection(player.getUniqueId(), pos1, null);
            player.sendMessage(ChatColor.GREEN + "ตั้งค่าจุดที่ 1 เรียบร้อย!");
        } 
        else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true); // Prevent interacting with block
            Location pos2 = event.getClickedBlock().getLocation();
            szManager.setPlayerSelection(player.getUniqueId(), null, pos2);
            player.sendMessage(ChatColor.GREEN + "ตั้งค่าจุดที่ 2 เรียบร้อย!");
        }
    }

    // --- 2. Prevent Mob Spawning in Safe Zones ---
    @EventHandler
    public void onMobSpawn(EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof Monster)) {
            return;
        }

        Location spawnLocation = event.getLocation();

        // Check Player-Made Safe Zones
        if (szManager.isLocationInSafeZone(spawnLocation)) {
            event.setCancelled(true);
            return;
        }

        // Check Village (Automatic Safe Zone)
        // Checks for any villager within 60 blocks
        if (szManager.isNearVillager(spawnLocation, 60.0)) {
            event.setCancelled(true);
        }
    }
}