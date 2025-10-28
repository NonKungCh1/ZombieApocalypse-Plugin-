package com.nonkungch.zombieapocalypse;

import org.bukkit.Bukkit; // (เพิ่ม F2)
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
import org.bukkit.inventory.PlayerInventory; // (เพิ่ม F2)
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
            if (event.getClickedBlock().getBlockData() instanceof Levelled) {
                Levelled waterData = (Levelled) event.getClickedBlock().getBlockData();
                if (waterData.getLevel() != 0) { return; }
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
        if (source.getType() == Material.POTION && source.hasItemMeta() &&
            source.getItemMeta().hasCustomModelData() &&
            source.getItemMeta().getCustomModelData() == 1003) {
            event.setResult(ZombieApocalypse.PURIFIED_WATER_ITEM.clone());
        }
    }

    // --- 3. Drink Water (แก้ไข F2) ---
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDrink(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        Player player = event.getPlayer();

        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasCustomModelData()) {
            if (item.getType() == Material.POTION) {
                 plugin.getThirstManager().increaseThirst(player, 5);
                 player.sendMessage(ChatColor.GRAY + "ยาขวดนี้ไม่ค่อยช่วยแก้กระหายเลย...");
            }
            return;
        }

        int modelData = item.getItemMeta().getCustomModelData();
        
        if (modelData == 1004) { // "Purified Water"
            event.setCancelled(true);
            plugin.getThirstManager().increaseThirst(player, 40);
            replaceItemWithBottle(player, item); // (FIX F2)
        }
        
        if (modelData == 1003) { // "Dirty Water"
            event.setCancelled(true);
            plugin.getThirstManager().increaseThirst(player, 15);
            player.sendMessage(ChatColor.RED + "น้ำนี่รสชาติแย่มาก...");

            if (Math.random() < 0.30) {
                player.sendMessage(ChatColor.DARK_RED + "คุณรู้สึกปั่นป่วนในท้อง!");
                if (Math.random() < 0.5) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 600, 0));
                } else {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 0));
                }
            }
            replaceItemWithBottle(player, item); // (FIX F2)
        }
    }

    // (FIX F2) เมธอดใหม่สำหรับจัดการไอเท็มหลังดื่ม
    private void replaceItemWithBottle(Player player, ItemStack consumedItem) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            PlayerInventory inv = player.getInventory();
            ItemStack bottle = new ItemStack(Material.GLASS_BOTTLE);

            if (inv.getItemInMainHand().isSimilar(consumedItem)) {
                if (inv.getItemInMainHand().getAmount() > 1) {
                    inv.getItemInMainHand().setAmount(inv.getItemInMainHand().getAmount() - 1);
                    inv.addItem(bottle);
                } else {
                    inv.setItemInMainHand(bottle);
                }
            } else if (inv.getItemInOffHand().isSimilar(consumedItem)) {
                if (inv.getItemInOffHand().getAmount() > 1) {
                    inv.getItemInOffHand().setAmount(inv.getItemInOffHand().getAmount() - 1);
                    inv.addItem(bottle);
                } else {
                    inv.setItemInOffHand(bottle);
                }
            }
            player.updateInventory();
        }, 1L);
    }
}