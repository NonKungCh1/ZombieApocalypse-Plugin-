package com.nonkungch.zombieapocalypse;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ZombieApocalypse extends JavaPlugin {

    // (ตัวแปรทั้งหมดเหมือนเดิม)
    private Map<UUID, Long> infectedPlayers = new HashMap<>();
    private ThirstManager thirstManager;
    private Set<UUID> boomers = new HashSet<>();
    private SafeZoneManager safeZoneManager;
    private PlayerStatsManager playerStatsManager;
    private NamespacedKey bandageKey, antidoteKey, dirtyWaterKey, batKey, knifeKey, zoneDefinerKey, zoneCoreKey;
    public static ItemStack DIRTY_WATER_ITEM;
    public static ItemStack PURIFIED_WATER_ITEM;
    public static ItemStack ZONE_DEFINER_ITEM;
    public static ItemStack ZONE_CORE_ITEM;


    @Override
    public void onEnable() {
        // (โค้ด onEnable ทั้งหมดเหมือนเดิม)
        thirstManager = new ThirstManager(this);
        safeZoneManager = new SafeZoneManager(this);
        playerStatsManager = new PlayerStatsManager(this);
        createWaterItems();
        createWaterRecipes();
        createBandageRecipe();
        createAntidoteRecipe();
        createBaseballBatRecipe();
        createCombatKnifeRecipe();
        createSafeZoneItems(); // <--- เมธอดนี้จะถูกอัปเดต
        getServer().getPluginManager().registerEvents(new ZombieListener(this), this);
        getServer().getPluginManager().registerEvents(new InfectionListener(this), this);
        getServer().getPluginManager().registerEvents(new ItemListener(this), this);
        getServer().getPluginManager().registerEvents(new ThirstListener(this), this);
        getServer().getPluginManager().registerEvents(new WaterListener(this), this);
        getServer().getPluginManager().registerEvents(new SafeZoneListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerStatsListener(this), this);
        getCommand("safezone").setExecutor(new SafeZoneCommands(this));
        getCommand("zinfo").setExecutor(new StatsCommand(this));
        getCommand("zupgrade").setExecutor(new StatsCommand(this));
        new InfectionTask(this).runTaskTimer(this, 0L, 20L);
        thirstManager.startThirstTask();
        safeZoneManager.startZoneEffectTask();
        playerStatsManager.startRegenTask();
        getLogger().info("ZombieApocalypse Plugin (v1.1) Enabled!");
    }

    @Override
    public void onDisable() {
        // (โค้ด onDisable ทั้งหมดเหมือนเดิม)
        if (bandageKey != null) Bukkit.removeRecipe(bandageKey);
        if (antidoteKey != null) Bukkit.removeRecipe(antidoteKey);
        if (dirtyWaterKey != null) Bukkit.removeRecipe(dirtyWaterKey);
        if (batKey != null) Bukkit.removeRecipe(batKey);
        if (knifeKey != null) Bukkit.removeRecipe(knifeKey);
        if (zoneDefinerKey != null) Bukkit.removeRecipe(zoneDefinerKey);
        if (zoneCoreKey != null) Bukkit.removeRecipe(zoneCoreKey);
        if (thirstManager != null) thirstManager.removeAllBossBars();
        if (safeZoneManager != null) safeZoneManager.removeAllSafeZones();
        getLogger().info("ZombieApocalypse Plugin Disabled.");
    }

    // (Getters และเมธอดอื่นๆ ทั้งหมดเหมือนเดิม)
    public Map<UUID, Long> getInfectedPlayers() { return infectedPlayers; }
    public ThirstManager getThirstManager() { return thirstManager; }
    public SafeZoneManager getSafeZoneManager() { return safeZoneManager; }
    public PlayerStatsManager getPlayerStatsManager() { return playerStatsManager; }
    public void addBoomer(UUID zombieId) { boomers.add(zombieId); }
    public void removeBoomer(UUID zombieId) { boomers.remove(zombieId); }
    public boolean isBoomer(UUID zombieId) { return boomers.contains(zombieId); }
    public void curePlayer(Player player) { /* ... โค้ดเดิม ... */ }
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) { /* ... โค้ดเดิม ... */ return false; }
    private void createBandageRecipe() { /* ... โค้ดเดิม ... */ }
    private void createAntidoteRecipe() { /* ... โค้ดเดิม ... */ }
    private void createWaterItems() { /* ... โค้ดเดิม ... */ }
    private void createWaterRecipes() { /* ... โค้ดเดิม ... */ }
    private void createBaseballBatRecipe() { /* ... โค้ดเดิม ... */ }
    private void createCombatKnifeRecipe() { /* ... โค้ดเดิม ... */ }
    
    // --- (P7) อัปเดตเมธอดนี้ ---
    private void createSafeZoneItems() {
        // 1. "ไม้เท้ากำหนดเขต" (Zone Definer)
        // (ส่วนนี้เหมือนเดิม - คราฟจาก Gold+Iron+Stick)
        ZONE_DEFINER_ITEM = new ItemStack(Material.BLAZE_ROD);
        ItemMeta definerMeta = ZONE_DEFINER_ITEM.getItemMeta();
        definerMeta.setDisplayName(ChatColor.GOLD + "ไม้เท้ากำหนดเขต");
        definerMeta.setLore(Arrays.asList(
            ChatColor.GRAY + "ใช้กำหนดพื้นที่ปลอดภัย:",
            ChatColor.YELLOW + "คลิกซ้าย: " + ChatColor.WHITE + "เลือกจุดที่ 1",
            ChatColor.YELLOW + "คลิกขวา: " + ChatColor.WHITE + "เลือกจุดที่ 2"
        ));
        definerMeta.setCustomModelData(1007);
        ZONE_DEFINER_ITEM.setItemMeta(definerMeta);

        zoneDefinerKey = new NamespacedKey(this, "zone_definer");
        ShapedRecipe definerRecipe = new ShapedRecipe(zoneDefinerKey, ZONE_DEFINER_ITEM);
        definerRecipe.shape( " G ", " I ", " S " );
        definerRecipe.setIngredient('G', Material.GOLD_INGOT);
        definerRecipe.setIngredient('I', Material.IRON_INGOT);
        definerRecipe.setIngredient('S', Material.STICK);
        Bukkit.addRecipe(definerRecipe);


        // --- (FIX) 2. "แกนพลังงานเซฟโซน" (Safe Zone Core) ---
        // (ผลลัพธ์ยังเป็น Beacon เหมือนเดิม)
        ZONE_CORE_ITEM = new ItemStack(Material.BEACON);
        ItemMeta coreMeta = ZONE_CORE_ITEM.getItemMeta();
        coreMeta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "แกนพลังงานเซฟโซน");
        coreMeta.setLore(Arrays.asList(
            ChatColor.GRAY + "ไอเท็มสำหรับสร้างพื้นที่ปลอดภัย",
            ChatColor.GRAY + "(จะถูกใช้ 1 ชิ้นต่อการสร้าง 1 ครั้ง)"
        ));
        coreMeta.setCustomModelData(1008);
        ZONE_CORE_ITEM.setItemMeta(coreMeta);

        zoneCoreKey = new NamespacedKey(this, "zone_core");
        ShapedRecipe coreRecipe = new ShapedRecipe(zoneCoreKey, ZONE_CORE_ITEM);
        
        // (สูตรคราฟใหม่ตามที่คุณยืนยัน)
        coreRecipe.shape(
            "IOI", // I = Iron Block, O = Obsidian
            "ODO", // D = Diamond Block
            "IOI"
        );
        coreRecipe.setIngredient('I', Material.IRON_BLOCK);
        coreRecipe.setIngredient('O', Material.OBSIDIAN);
        coreRecipe.setIngredient('D', Material.DIAMOND_BLOCK);
        // --- END FIX ---
        
        Bukkit.addRecipe(coreRecipe);
    }
}
