package com.nonkungch.zombieapocalypse;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class StatsCommand implements CommandExecutor {

    private ZombieApocalypse plugin;
    private PlayerStatsManager statsManager;

    public StatsCommand(ZombieApocalypse plugin) {
        this.plugin = plugin;
        this.statsManager = plugin.getPlayerStatsManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command is for players only.");
            return true;
        }

        Player player = (Player) sender;

        // --- (A3) /zinfo ---
        if (command.getName().equalsIgnoreCase("zinfo")) {
            sendInfoPanel(player);
            return true;
        }

        // --- (A2) /zupgrade ---
        if (command.getName().equalsIgnoreCase("zupgrade")) {
            if (args.length == 0) {
                player.sendMessage(ChatColor.RED + "Usage: /zupgrade <stat>");
                player.sendMessage(ChatColor.GRAY + "Stats: health, speed, damage, regen");
                return true;
            }
            
            String statToUpgrade = args[0];
            List<String> validStats = Arrays.asList("health", "speed", "damage", "regen");
            
            if (validStats.contains(statToUpgrade.toLowerCase())) {
                statsManager.upgradeStat(player, statToUpgrade.toLowerCase());
            } else {
                player.sendMessage(ChatColor.RED + "ไม่พบสถานะ '" + statToUpgrade + "'");
            }
            return true;
        }

        return false;
    }
    
    // เมธอดสำหรับแสดง /zinfo
    private void sendInfoPanel(Player player) {
        PlayerStatsManager.PlayerStats stats = statsManager.getPlayerStats(player);
        if (stats == null) {
            player.sendMessage(ChatColor.RED + "ไม่สามารถโหลดข้อมูลของคุณได้ ลองเข้าเกมใหม่");
            return;
        }
        
        player.sendMessage(ChatColor.DARK_GRAY + "---[" + ChatColor.AQUA + " Zombie Apocalypse Stats " + ChatColor.DARK_GRAY + "]---");
        player.sendMessage(ChatColor.YELLOW + "Upgrade Points: " + ChatColor.GREEN + stats.upgradePoints);
        player.sendMessage("");
        player.sendMessage(ChatColor.RED + "Health: " + ChatColor.WHITE + stats.healthLevel + "/" + stats.MAX_LEVEL + 
                           ChatColor.GRAY + " (เลือด " + stats.getMaxHealth() / 2 + " หัวใจ)");
                           
        player.sendMessage(ChatColor.AQUA + "Speed: " + ChatColor.WHITE + stats.speedLevel + "/" + stats.MAX_LEVEL +
                           ChatColor.GRAY + " (วิ่งเร็ว " + String.format("%.1f", (stats.getMovementSpeed() / 0.1) * 100) + "%)");
                           
        player.sendMessage(ChatColor.GOLD + "Damage: " + ChatColor.WHITE + stats.damageLevel + "/" + stats.MAX_LEVEL +
                           ChatColor.GRAY + " (โจมตี " + stats.getAttackDamage() + " หน่วย)");
                           
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Regen: " + ChatColor.WHITE + stats.regenLevel + "/" + stats.MAX_LEVEL +
                           ChatColor.GRAY + " (ฟื้นฟูระดับ " + stats.regenLevel + ")");
                           
        player.sendMessage(ChatColor.DARK_GRAY + "------------------------------------");
        player.sendMessage(ChatColor.GRAY + "ใช้ " + ChatColor.YELLOW + "/zupgrade <stat>" + ChatColor.GRAY + " เพื่ออัปเกรด");
    }
}