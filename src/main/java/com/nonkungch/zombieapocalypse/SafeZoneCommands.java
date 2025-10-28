package com.nonkungch.zombieapocalypse;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SafeZoneCommands implements CommandExecutor {

    private ZombieApocalypse plugin;
    private SafeZoneManager szManager;

    public SafeZoneCommands(ZombieApocalypse plugin) {
        this.plugin = plugin;
        this.szManager = plugin.getSafeZoneManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command is for players only.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length > 0 && args[0].equalsIgnoreCase("create")) {
            // Call the create method from the manager
            szManager.createSafeZone(player);
            return true;
        }

        player.sendMessage(ChatColor.YELLOW + "Usage: /safezone create");
        return true;
    }
}