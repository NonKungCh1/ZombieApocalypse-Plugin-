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
import org.bukkit.potion.PotionEffectType; // ตรวจสอบว่า import นี้มี
import org.bukkit.potion.PotionType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ZombieApocalypse extends JavaPlugin {

    // --- (P2: Infection) ---
    private Map<UUID, Long> infectedPlayers = new HashMap<>();

    // --- (P4: Thirst) ---
    private ThirstManager thirstManager;

    // --- (P5: Special Infected) ---
    private Set<UUID> boomers = new HashSet<>();
    
    // --- (P7: Safe Zones) ---
    private SafeZoneManager safeZoneManager;

    // --- Recipe Keys ---
    private NamespacedKey bandageKey, antidoteKey, dirtyWaterKey, batKey, knifeKey, zoneDefinerKey, zoneCoreKey;

    // --- Static Item Stacks ---
    public static ItemStack DIRTY_WATER_ITEM;
    public static ItemStack PURIFIED_WATER_ITEM;
    public static ItemStack ZONE_DEFINER_ITEM;
    public static ItemStack ZONE_CORE_ITEM;


    @Override
    public void onEnable() {
        // --- 1. Initialize Managers ---
        thirstManager = new ThirstManager(this);
        safeZoneManager = new SafeZoneManager(this);

        // --- 2. Create Items & Recipes ---
        createWaterItems(); // P4
        createWaterRecipes(); // P4
        createBandageRecipe(); // P3
        createAntidoteRecipe(); // P3
        createBaseballBatRecipe(); // P6
        createCombatKnifeRecipe(); // P6
        createSafeZoneItems(); // P7 (with updated recipe)
        
        // --- 3. Register Listeners ---
        getServer().getPluginManager().registerEvents(new ZombieListener(this), this); // P1, P5
        getServer().getPluginManager().registerEvents(new InfectionListener(this), this); // P2
        getServer().getPluginManager().registerEvents(new ItemListener(this), this); // P3
        getServer().getPluginManager().registerEvents(new ThirstListener(this), this); // P4
        getServer().getPluginManager().registerEvents(new WaterListener(this), this); // P4
        getServer().getPluginManager().registerEvents(new SafeZoneListener(this), this); // P7

        // --- 4. Register Commands ---
        getCommand("safezone").setExecutor(new SafeZoneCommands(this)); // P7
        // (cureme command is handled in this file)

        // --- 5. Start Tasks ---
        new InfectionTask(this).runTaskTimer(this, 0L, 20L); // P2 Task (1 sec)
        thirstManager.startThirstTask(); // P4 Task (5 sec)
        safeZoneManager.startZoneEffectTask(); // P7 Task (2 sec)

        getLogger().info("ZombieApocalypse Plugin (v1.0) Enabled!");
    }

    @Override
    public void onDisable() {
        // Remove recipes
        if (bandageKey != null) Bukkit.removeRecipe(bandageKey);
        if (antidoteKey != null) Bukkit.removeRecipe(antidoteKey);
        if (dirtyWaterKey != null) Bukkit.removeRecipe(dirtyWaterKey);
        if (batKey != null) Bukkit.removeRecipe(batKey);
        if (knifeKey != null) Bukkit.removeRecipe(knifeKey);
        if (zoneDefinerKey != null) Bukkit.removeRecipe(zoneDefinerKey);
        if (zoneCoreKey != null) Bukkit.removeRecipe(zoneCoreKey);
        
        // Clean up
        if (thirstManager != null) thirstManager.removeAllBossBars();
        if (safeZoneManager != null) safeZoneManager.removeAllSafeZones();

        getLogger().info("ZombieApocalypse Plugin Disabled.");
    }

    // --- Getters for Managers/Data ---
    public Map<UUID, Long> getInfectedPlayers() { return infectedPlayers; }
    public ThirstManager getThirstManager() { return thirstManager; }
    public SafeZoneManager getSafeZoneManager() { return safeZoneManager; }
    public void addBoomer(UUID zombieId) { boomers.add(zombieId); }
    public void removeBoomer(UUID zombieId) { boomers.remove(zombieId); }
    public boolean isBoomer(UUID zombieId) { return boomers.contains(zombieId); }

    // --- (P2) Infection Cure Method ---
    public void curePlayer(Player player) {
        if (infectedPlayers.containsKey(player.getUniqueId())) {
            infectedPlayers.remove(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "คุณรู้สึกดีขึ้นแล้ว! การติดเชื้อหายไปแล้ว");
            // Clear infection-related effects
            player.removePotionEffect(org.bukkit.potion.PotionEffectType.POISON);
            player.removePotionEffect(org.bukkit.potion.PotionEffectType.HUNGER);
            player.removePotionEffect(org.bukkit.potion.PotionEffectType.WEAKNESS);
            // --- FIX ---
            player.removePotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS); // <--- แก้ไขจาก SLOW
            // --- END FIX ---
        } else {
            player.sendMessage(ChatColor.GRAY + "คุณไม่ได้ติดเชื้ออยู่");
        }
    }

    // --- (P2) Command Handling ---
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

    // --- (P3) Item Recipes ---
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

    // --- (P4) Water Items & Recipes ---
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
        // We check the input item in the WaterListener, so Material.POTION is fine
        FurnaceRecipe recipe = new FurnaceRecipe(dirtyWaterKey, PURIFIED_WATER_ITEM, Material.POTION, 0.1f, 100);
        Bukkit.addRecipe(recipe);
    }

    // --- (P6) Weapon Recipes ---
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
        recipe.shape("P", "P", "S");
        recipe.setIngredient('P', Material.OAK_PLANKS);
        recipe.setIngredient('S', Material.STICK);
        Bukkit.addRecipe(recipe);
    }

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
    
    // --- (P7) Safe Zone Item Recipes ---
    private void createSafeZoneItems() {
        // 1. "ไม้เท้ากำหนดเขต" (Zone Definer)
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
        
        // --- (สูตรใหม่ตามที่คุณขอ) ---
        definerRecipe.shape(
            " G ", // G = Gold Ingot
            " I ", // I = Iron Ingot
            " S "  // S = Stick
        );
        definerRecipe.setIngredient('G', Material.GOLD_INGOT);
        definerRecipe.setIngredient('I', Material.IRON_INGOT);
        definerRecipe.setIngredient('S', Material.STICK);
        Bukkit.addRecipe(definerRecipe);

        // 2. "แกนพลังงานเซฟโซน" (Safe Zone Core)
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
        coreRecipe.shape("EGE", "GNG", "OBO");
        coreRecipe.setIngredient('E', Material.EMERALD_BLOCK);
        coreRecipe.setIngredient('G', Material.GOLD_BLOCK);
        coreRecipe.setIngredient('N', Material.NETHER_STAR);
        coreRecipe.setIngredient('O', Material.OBSIDIAN);
        coreRecipe.setIngredient('B', Material.BEACON);
        Bukkit.addRecipe(coreRecipe);
    }
}
