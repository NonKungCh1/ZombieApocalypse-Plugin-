package com.nonkungch.zombieapocalypse;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SafeZoneManager {

    private ZombieApocalypse plugin;
    
    // Temp storage for player selections
    private Map<UUID, Location> playerPos1 = new HashMap<>();
    private Map<UUID, Location> playerPos2 = new HashMap<>();
    
    // Storage for active zones
    private Map<String, List<SafeZone>> activeZones = new HashMap<>();
    
    // Storage for visual markers
    private List<ArmorStand> visualMarkers = new ArrayList<>();

    private final int MAX_ZONE_SIZE = 30;

    public SafeZoneManager(ZombieApocalypse plugin) {
        this.plugin = plugin;
    }

    // --- 1. Selection Management ---
    public void setPlayerSelection(UUID playerId, Location pos1, Location pos2) {
        if (pos1 != null) {
            playerPos1.put(playerId, pos1);
        }
        if (pos2 != null) {
            playerPos2.put(playerId, pos2);
        }
    }

    // --- 2. Zone Creation (called by command) ---
    public void createSafeZone(Player player) {
        Location pos1 = playerPos1.get(player.getUniqueId());
        Location pos2 = playerPos2.get(player.getUniqueId());

        if (pos1 == null || pos2 == null) {
            player.sendMessage(ChatColor.RED + "คุณต้องเลือกจุดที่ 1 และ 2 ก่อน!");
            return;
        }

        if (!pos1.getWorld().equals(pos2.getWorld())) {
            player.sendMessage(ChatColor.RED + "จุดที่ 1 และ 2 ต้องอยู่ในโลกเดียวกัน!");
            return;
        }

        // Check size limit (30x30)
        int sizeX = Math.abs(pos1.getBlockX() - pos2.getBlockX()) + 1;
        int sizeZ = Math.abs(pos1.getBlockZ() - pos2.getBlockZ()) + 1;

        if (sizeX > MAX_ZONE_SIZE || sizeZ > MAX_ZONE_SIZE) {
            player.sendMessage(ChatColor.RED + "พื้นที่ของคุณใหญ่เกินไป! (จำกัด " + MAX_ZONE_SIZE + "x" + MAX_ZONE_SIZE + " บล็อก)");
            return;
        }
        
        // Check for "Safe Zone Core" (1008)
        if (!player.getInventory().containsAtLeast(ZombieApocalypse.ZONE_CORE_ITEM, 1)) {
            player.sendMessage(ChatColor.RED + "คุณไม่มี 'แกนพลังงานเซฟโซน'!");
            return;
        }
        
        // --- Success ---
        player.getInventory().removeItem(ZombieApocalypse.ZONE_CORE_ITEM); // Consume item
        
        SafeZone newZone = new SafeZone(pos1, pos2);
        activeZones.computeIfAbsent(pos1.getWorld().getName(), k -> new ArrayList<>()).add(newZone);

        createVisualMarker(newZone);

        player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "สร้างพื้นที่ปลอดภัยสำเร็จ!");
        playerPos1.remove(player.getUniqueId());
        playerPos2.remove(player.getUniqueId());
    }
    
    private void createVisualMarker(SafeZone zone) {
        Location center = zone.getCenter();
        Location markerLocation = center.clone().add(0, 1.5, 0); // Float in the air

        ArmorStand as = (ArmorStand) center.getWorld().spawnEntity(markerLocation, EntityType.ARMOR_STAND);
        as.setGravity(false);
        as.setCanPickupItems(false);
        as.setCustomName(ChatColor.AQUA + "Safe Zone Projector");
        as.setCustomNameVisible(true);
        as.setVisible(false); // Invisible body
        as.setMarker(true); // No hitbox
        
        as.getEquipment().setHelmet(ZombieApocalypse.ZONE_CORE_ITEM.clone());
        as.addEquipmentLock(EquipmentSlot.HEAD, ArmorStand.LockType.REMOVING_OR_CHANGING);
        
        visualMarkers.add(as);
    }
    
    public void removeAllSafeZones() {
        for (ArmorStand as : visualMarkers) {
            as.remove();
        }
        visualMarkers.clear();
        activeZones.clear();
    }

    // --- 3. Location Checking (for Listeners) ---
    public boolean isLocationInSafeZone(Location location) {
        List<SafeZone> zonesInWorld = activeZones.get(location.getWorld().getName());
        if (zonesInWorld == null) return false;

        for (SafeZone zone : zonesInWorld) {
            if (zone.contains(location)) {
                return true;
            }
        }
        return false;
    }

    public boolean isNearVillager(Location location, double radius) {
        for (Entity entity : location.getWorld().getEntitiesByClass(EntityType.VILLAGER.getEntityClass())) {
            if (entity.getLocation().distance(location) <= radius) {
                return true;
            }
        }
        return false;
    }

    // --- 4. Monster Pushing Task ---
    public void startZoneEffectTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (String worldName : activeZones.keySet()) {
                World world = Bukkit.getWorld(worldName);
                if (world == null) continue;

                for (Monster monster : world.getEntitiesByClass(Monster.class)) {
                    Location loc = monster.getLocation();
                    
                    if (isLocationInSafeZone(loc) || isNearVillager(loc, 60.0)) {
                        pushEntityAway(monster, 1.2);
                    }
                }
            }
        }, 100L, 40L); // Start after 5s, run every 2s
    }
    
    private void pushEntityAway(Entity entity, double power) {
        // Push entity backwards
        Vector direction = entity.getLocation().getDirection().multiply(-1).setY(0.5);
        if (direction.lengthSquared() == 0) {
             direction = new Vector(0, 0.5, -1);
        }
        entity.setVelocity(direction.normalize().multiply(power));
    }
}


// --- (Internal Class for SafeZone data) ---
class SafeZone {
    private String worldName;
    private int minX, minY, minZ;
    private int maxX, maxY, maxZ;
    private Location center;

    public SafeZone(Location pos1, Location pos2) {
        this.worldName = pos1.getWorld().getName();
        this.minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        this.minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        this.minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        this.maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        this.maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        this.maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        
        this.center = new Location(
            pos1.getWorld(),
            minX + (double)(maxX - minX) / 2.0,
            minY + (double)(maxY - minY) / 2.0,
            minZ + (double)(maxZ - minZ) / 2.0
        );
    }

    public Location getCenter() {
        return center;
    }

    public boolean contains(Location loc) {
        if (!loc.getWorld().getName().equals(worldName)) {
            return false;
        }
        return loc.getX() >= minX && loc.getX() <= maxX + 1 && // +1 for inclusivity
               loc.getY() >= minY && loc.getY() <= maxY + 1 &&
               loc.getZ() >= minZ && loc.getZ() <= maxZ + 1;
    }
}