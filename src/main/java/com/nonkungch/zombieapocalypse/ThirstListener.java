package com.nonkungch.zombieapocalypse;

import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Arrays;
import java.util.List;

public class ThirstListener implements Listener {

    private ZombieApocalypse plugin;
    private ThirstManager thirstManager;

    private static final List<Biome> HOT_BIOMES = Arrays.asList(
            Biome.DESERT, Biome.SAVANNA, Biome.SAVANNA_PLATEAU, 
            Biome.ERODED_BADLANDS, Biome.BADLANDS, Biome.WOODED_BADLANDS,
            Biome.NETHER_WASTES, Biome.SOUL_SAND_VALLEY, 
            Biome.CRIMSON_FOREST, Biome.WARPED_FOREST, Biome.BASALT_DELTAS
    );

    public ThirstListener(ZombieApocalypse plugin) {
        this.plugin = plugin;
        this.thirstManager = plugin.getThirstManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        thirstManager.addPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        thirstManager.removePlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Check for actual block movement
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY()) {
            return;
        }

        Player player = event.getPlayer();

        // 1. Check for sprinting
        if (player.isSprinting()) {
            // (10% chance per block move to decrease thirst)
            if (Math.random() < 0.1) {
                thirstManager.decreaseThirst(player, 1);
            }
        }

        // 2. Check for hot biomes
        Biome currentBiome = player.getLocation().getBlock().getBiome();
        if (HOT_BIOMES.contains(currentBiome)) {
            // (2% chance per block move to decrease thirst)
             if (Math.random() < 0.02) {
                thirstManager.decreaseThirst(player, 1);
            }
        }
    }
}