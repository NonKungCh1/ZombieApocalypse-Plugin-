package com.nonkungch.zombieapocalypse;

import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemListener implements Listener {

    private ZombieApocalypse plugin;

    public ItemListener(ZombieApocalypse plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerUseItem(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        Player player = event.getPlayer();

        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasCustomModelData()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        int modelData = meta.getCustomModelData();

        // --- 1. Bandage (1001) ---
        if (modelData == 1001) {
            event.setCancelled(true);
            double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            if (player.getHealth() >= maxHealth) {
                player.sendMessage(ChatColor.GRAY + "คุณแข็งแรงดีอยู่แล้ว");
                return;
            }

            // Heal 4 HP (2 hearts)
            double newHealth = Math.min(player.getHealth() + 4.0, maxHealth);
            player.setHealth(newHealth);

            player.sendMessage(ChatColor.WHITE + "คุณใช้ผ้าพันแผลรักษาบาดแผล...");
            item.setAmount(item.getAmount() - 1);
        }

        // --- 2. Antidote (1002) ---
        if (modelData == 1002) {
            event.setCancelled(true);
            if (!plugin.getInfectedPlayers().containsKey(player.getUniqueId())) {
                player.sendMessage(ChatColor.GRAY + "คุณไม่ได้ติดเชื้ออะไร");
                return;
            }

            // Cure infection (P2)
            plugin.curePlayer(player);
            item.setAmount(item.getAmount() - 1);
        }
    }
}