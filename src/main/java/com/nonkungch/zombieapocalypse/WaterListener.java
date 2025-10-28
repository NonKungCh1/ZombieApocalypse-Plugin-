package com.nonkungch.zombieapocalypse;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class WaterListener implements Listener {

    private ZombieApocalypse plugin;

    public WaterListener(ZombieApocalypse plugin) {
        this.plugin = plugin;
    }

    // --- 1. Fill Bottle (Get Dirty Water) ---
    @EventHandler
    public void onPlayerFillBottle(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getItem() == null || event.getItem().getType() != Material.GLASS_BOTTLE) return;
        if (event.getClickedBlock().getType() == Material.WATER) {
            
            // Ensure it's a source block
            if (event.getClickedBlock().getBlockData() instanceof Levelled) {
                Levelled waterData = (Levelled) event.getClickedBlock().getBlockData();
                if (waterData.getLevel() != 0) { 
                    return; 
                }
            }

            event.setCancelled(true);
            event.getItem().setAmount(event.getItem().getAmount() - 1);
            player.getInventory().addItem(ZombieApocalypse.DIRTY_WATER_ITEM.clone());
            player.sendMessage(ChatColor.GRAY + "คุณตักน้ำสกปรกขึ้นมา");
        }
    }

    // --- 2. Smelt Water ---
    @EventHandler
    public void onWaterSmelt(FurnaceSmeltEvent event) {
        ItemStack source = event.getSource();
        
        // Check if input is "Dirty Water" (1003)
        if (source.getType() == Material.POTION && source.hasItemMeta() &&
            source.getItemMeta().hasCustomModelData() &&
            source.getItemMeta().getCustomModelData() == 1003) {
            
            event.setResult(ZombieApocalypse.PURIFIED_WATER_ITEM.clone());
        }
    }

    // --- 3. Drink Water ---
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDrink(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        Player player = event.getPlayer();

        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasCustomModelData()) {
            // Handle vanilla potions (give very little thirst)
            if (item.getType() == Material.POTION) {
                 plugin.getThirstManager().increaseThirst(player, 5);
                 player.sendMessage(ChatColor.GRAY + "ยาขวดนี้ไม่ค่อยช่วยแก้กระหายเลย...");
            }
            return;
        }

        int modelData = item.getItemMeta().getCustomModelData();
        
        // --- Drink "Purified Water" (1004) ---
        if (modelData == 1004) {
            event.setCancelled(true); // Cancel vanilla Potion (Water) effect
            plugin.getThirstManager().increaseThirst(player, 40);
            item.setAmount(item.getAmount() - 1);
            player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE));
        }
        
        // --- Drink "Dirty Water" (1003) ---
        if (modelData == 1003) {
            event.setCancelled(true);
            plugin.getThirstManager().increaseThirst(player, 15);
            player.sendMessage(ChatColor.RED + "น้ำนี่รสชาติแย่มาก...");

            // 30% chance for sickness
            if (Math.random() < 0.30) {
                player.sendMessage(ChatColor.DARK_RED + "คุณรู้สึกปั่นป่วนในท้อง!");
                if (Math.random() < 0.5) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 600, 0)); // 30 sec
                } else {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 0)); // 5 sec
                }
            }
            item.setAmount(item.getAmount() - 1);
            player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE));
        }
    }
}