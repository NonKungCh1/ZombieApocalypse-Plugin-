package com.nonkungch.zombieapocalypse;

import org.bukkit.ChatColor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Random;

public class InfectionListener implements Listener {

    private ZombieApocalypse plugin;
    private Random random = new Random();

    public InfectionListener(ZombieApocalypse plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        if (event.getDamager().getType() != EntityType.ZOMBIE) {
            return;
        }

        Player player = (Player) event.getEntity();

        if (plugin.getInfectedPlayers().containsKey(player.getUniqueId())) {
            return;
        }

        // --- 25% Chance to infect ---
        if (random.nextDouble() <= 0.25) { 
            plugin.getInfectedPlayers().put(player.getUniqueId(), System.currentTimeMillis());
            player.sendMessage(ChatColor.RED + "คุณรู้สึกเจ็บแปลบที่แผล... คุณคิดว่าคุณติดเชื้อแล้ว!");
        }
    }
}