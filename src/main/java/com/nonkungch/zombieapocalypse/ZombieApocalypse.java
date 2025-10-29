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

    // (ตัวแปรทั้งหมด)
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
        thirstManager = new ThirstManager(this);
        safeZoneManager = new SafeZoneManager(this);
        playerStatsManager = new PlayerStatsManager(this);

        // --- 2. สร้างสูตรคราฟทั้งหมด ---
        // (ไฟล์ที่แล้วผมลืมใส่ 4 อันนี้กลับเข้ามา)
        createWaterItems();
        createWaterRecipes();
        createBandageRecipe(); // <--- คืนกลับมาแล้ว
        createAntidoteRecipe(); // <--- คืนกลับมาแล้ว
        createBaseballBatRecipe(); // <--- คืนกลับมาแล้ว (และแก้ไขแล้ว)
        createCombatKnifeRecipe(); // <--- คืนกลับมาแล้ว
        createSafeZoneItems(); // (สูตร Core แก้ไขแล้ว)
        // ---------------------------------
        
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
        // (ลบสูตรคราฟทั้งหมด)
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

    // (Getters และ curePlayer, onCommand[cureme] เหมือนเดิม)
    public Map<UUID, Long> getInfectedPlayers() { return infectedPlayers; }
    public ThirstManager getThirstManager() { return thirstManager; }
    public SafeZoneManager getSafeZoneManager() { return safeZoneManager; }
    public PlayerStatsManager getPlayerStatsManager() { return playerStatsManager; }
    public void addBoomer(UUID zombieId) { boomers.add(zombieId); }
    public void removeBoomer(UUID zombieId) { boomers.remove(zombieId); }
    public boolean isBoomer(UUID zombieId) { return boomers.contains(zombieId); }
    public void curePlayer(Player player) {
        if (infectedPlayers.containsKey(player.getUniqueId())) {
            infectedPlayers.remove(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "คุณรู้สึกดีขึ้นแล้ว! การติดเชื้อหายไปแล้ว");
            player.removePotionEffect(PotionEffectType.POISON);
            player.removePotionEffect(PotionEffectType.HUNGER);
            player.removePotionEffect(PotionEffectType.WEAKNESS);
            player.removePotionEffect(PotionEffectType.SLOWNESS);
        } else {
            player.sendMessage(ChatColor.GRAY + "คุณไม่ได้ติดเชื้ออยู่");
        }
    }
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("cureme")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                curePlayer(player);
                return true;
            } else {
                sender.sendMessage("This command can only be run by a player.");
                return true;
            }
        }
        return false;
    }

    // --- (P3) ผ้าพันแผล ---
    private void createBandageRecipe() {
        ItemStack bandageItem = new ItemStack(Material.PAPER, 1);
        ItemMeta meta = bandageItem.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "" + ChatColor.BOLD + "ผ้าพันแผล");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "ใช้สำหรับห้ามเลือดและรักษาบาดแผล", ChatColor.BLUE + "ฟื้นฟู 2 หัวใจ"));
        meta.setCustomModelData(1001); 
        bandageItem.setItemMeta(meta);

        bandageKey = new NamespacedKey(this, "bandage");
        ShapelessRecipe recipe = new ShapelessRecipe(bandageKey, bandageItem);
        recipe.addIngredient(3, Material.STRING);
        recipe.addIngredient(1, Material.PAPER);
        Bukkit.addRecipe(recipe);
    }

    // --- (P3) ยาต้านเชื้อ ---
    private void createAntidoteRecipe() {
        ItemStack antidoteItem = new ItemStack(Material.POTION, 1); 
        ItemMeta meta = antidoteItem.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "ยาต้านเชื้อ");
        meta.setLore(Arrays.asList(ChatColor.AQUA + "ใช้รักษาการติดเชื้อซอมบี้โดยเฉพาะ", ChatColor.DARK_RED + "(ไม่สามารถรักษาบาดแผลทั่วไปได้)"));
        meta.setCustomModelData(1002);
        antidoteItem.setItemMeta(meta);

        antidoteKey = new NamespacedKey(this, "antidote");
        ShapedRecipe recipe = new ShapedRecipe(antidoteKey, antidoteItem);
        recipe.shape(" N ", " A ", " B ");
        recipe.setIngredient('N', Material.NETHER_WART);
        recipe.setIngredient('A', Material.GOLDEN_APPLE);
        recipe.setIngredient('B', Material.GLASS_BOTTLE);
        Bukkit.addRecipe(recipe);
    }

    // --- (P4) ระบบน้ำ ---
    private void createWaterItems() {
        DIRTY_WATER_ITEM = new ItemStack(Material.POTION, 1);
        PotionMeta dirtyMeta = (PotionMeta) DIRTY_WATER_ITEM.getItemMeta();
        dirtyMeta.setBasePotionData(new PotionData(PotionType.WATER));
        dirtyMeta.setDisplayName(ChatColor.DARK_GREEN + "น้ำสกปรก");
        dirtyMeta.setLore(Arrays.asList(ChatColor.GRAY + "น้ำจากแหล่งน้ำธรรมชาติ", ChatColor.RED + "ควรนำไปต้มก่อนดื่ม"));
        dirtyMeta.setCustomModelData(1003);
        DIRTY_WATER_ITEM.setItemMeta(dirtyMeta);

        PURIFIED_WATER_ITEM = new ItemStack(Material.POTION, 1);
        PotionMeta cleanMeta = (PotionMeta) PURIFIED_WATER_ITEM.getItemMeta();
        cleanMeta.setBasePotionData(new PotionData(PotionType.WATER));
        cleanMeta.setDisplayName(ChatColor.AQUA + "น้ำสะอาด");
        cleanMeta.setLore(Arrays.asList(ChatColor.GRAY + "น้ำที่ผ่านการต้มแล้ว", ChatColor.BLUE + "ปลอดภัยสำหรับดื่ม"));
        cleanMeta.setCustomModelData(1004);
        PURIFIED_WATER_ITEM.setItemMeta(cleanMeta);
    }
    private void createWaterRecipes() {
        dirtyWaterKey = new NamespacedKey(this, "purified_water");
        FurnaceRecipe recipe = new FurnaceRecipe(dirtyWaterKey, PURIFIED_WATER_ITEM, Material.POTION, 0.1f, 100);
        Bukkit.addRecipe(recipe);
    }

    // --- (P6) ไม้เบสบอล (แก้ไข) ---
    private void createBaseballBatRecipe() {
        ItemStack batItem = new ItemStack(Material.WOODEN_SWORD);
        ItemMeta meta = batItem.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "ไม้เบสบอล");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "อาวุธทื่อๆ สำหรับคุมฝูงชน", ChatColor.YELLOW + "พลังผลักสูง"));
        meta.setCustomModelData(1005);
        meta.setUnbreakable(true);

        AttributeModifier damage = new AttributeModifier(UUID.randomUUID(), "generic.attackDamage", 5.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND);
        AttributeModifier knockback = new AttributeModifier(UUID.randomUUID(), "generic.attackKnockback", 1.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND);
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, damage);
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_KNOCKBACK, knockback);
        batItem.setItemMeta(meta);

        batKey = new NamespacedKey(this, "baseball_bat");
        ShapedRecipe recipe = new ShapedRecipe(batKey, batItem);
        recipe.shape("L", "L", "S"); // L = Log, S = Stick
        
        // --- (FIX) เปลี่ยนจาก Plank เป็น Log ---
        recipe.setIngredient('L', Material.OAK_LOG); 
        // --- END FIX ---
        
        recipe.setIngredient('S', Material.STICK);
        Bukkit.addRecipe(recipe);
    }

    // --- (P6) มีดคอมแบท ---
    private void createCombatKnifeRecipe() {
        ItemStack knifeItem = new ItemStack(Material.IRON_SWORD);
        ItemMeta meta = knifeItem.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "มีดคอมแบท");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "อาวุธระยะประชิด ความเร็วสูง", ChatColor.YELLOW + "ความเร็วโจมตีสูงมาก"));
        meta.setCustomModelData(1006);
        meta.setUnbreakable(true);

        AttributeModifier damage = new AttributeModifier(UUID.randomUUID(), "generic.attackDamage", 4.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND);
        AttributeModifier speed = new AttributeModifier(UUID.randomUUID(), "generic.attackSpeed", 3.2, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND);
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, damage);
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, speed);
        knifeItem.setItemMeta(meta);

        knifeKey = new NamespacedKey(this, "combat_knife");
        ShapedRecipe recipe = new ShapedRecipe(knifeKey, knifeItem);
        recipe.shape("I", "S");
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('S', Material.STICK);
        Bukkit.addRecipe(recipe);
    }
    
    // --- (P7) ระบบเซฟโซน (สูตรแก้ไขแล้ว) ---
    private void createSafeZoneItems() {
        // 1. "ไม้เท้ากำหนดเขต"
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


        // 2. "แกนพลังงานเซฟโซน"
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
        
        // (สูตรคราฟใหม่ที่ยืนยัน)
        coreRecipe.shape(
            "IOI", // I = Iron Block, O = Obsidian
            "ODO", // D = Diamond Block
            "IOI"
        );
        coreRecipe.setIngredient('I', Material.IRON_BLOCK);
        coreRecipe.setIngredient('O', Material.OBSIDIAN);
        coreRecipe.setIngredient('D', Material.DIAMOND_BLOCK);
        Bukkit.addRecipe(coreRecipe);
    }
}