package me.valkeea.fishyaddons.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.valkeea.fishyaddons.tracker.TrackerUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;

public class EntityTracker {
    private static final Set<Entity> trackedEntities = ConcurrentHashMap.newKeySet();
    private static final Map<Entity, TrackedMob> mobData = new ConcurrentHashMap<>();
    private static final Map<Entity, Long> armorStandSpawnTimes = new ConcurrentHashMap<>();
    private static final double TRACKING_DISTANCE = 32.0;
    private static final double ASSOCIATION_DISTANCE = 10.0;
    private static boolean foundVal = false;
    
    private static final Pattern VALUABLE_MOB_PATTERN = Pattern.compile(
        ".*\\b(lord jawbus|thunder)\\b.*",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern MOB_INFO_PATTERN = Pattern.compile(
        ".*\\[Lv(\\d+)\\]\\s*(?:[^\\s\\w]+\\s*)*(.+?)\\s+(\\d+(?:\\.\\d+)?[kmbtKMBT]?)/(\\d+(?:\\.\\d+)?[kmbtKMBT]?)❤.*",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern VALUABLE_PLAYERENTITY_PATTERN = Pattern.compile(
        ".*\\b(great white shark|minos inquisitor|grim reaper|minotaur|minos champion)\\b.*",
        Pattern.CASE_INSENSITIVE
    );
    
    private EntityTracker() {}
    
    public static class TrackedMob {
        public final Entity mobEntity;
        public final String mobType;
        private String displayName;
        private int level = 0;
        private long spawnTime;
        private Entity nameArmorStand;
        
        public TrackedMob(Entity mobEntity, String mobType) {
            this.mobEntity = mobEntity;
            this.mobType = mobType;
            this.spawnTime = System.currentTimeMillis();
        }
        
        public boolean isValuable() {
            if (displayName == null) return false;
            if (VALUABLE_MOB_PATTERN.matcher(displayName).matches()) {
                return true;
            }
            
            if (VALUABLE_PLAYERENTITY_PATTERN.matcher(displayName).matches()) {
                return true;
            }

            if (mobEntity instanceof PlayerEntity && mobType.startsWith("player_")) {
                return VALUABLE_PLAYERENTITY_PATTERN.matcher(displayName).matches();
            }
            
            return false;
        }
        
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public int getLevel() { return level; }
        public void setLevel(int level) { this.level = level; }
        public long getSpawnTime() { return spawnTime; }
        public Entity getNameArmorStand() { return nameArmorStand; }
        public void setNameArmorStand(Entity nameArmorStand) { this.nameArmorStand = nameArmorStand; }
    }
    
    public static void onEntityAdded(Entity entity) {
        if (entity == null) return;
        if (!TrackerUtils.isEnabled() && !TrackerUtils.isOn()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        double distance = client.player.distanceTo(entity);
        if (distance > TRACKING_DISTANCE) return;
        if (trackedEntities.add(entity) && !mobData.containsKey(entity)) {
            handleNewEntity(entity);
        }
    }
    
    public static void onEntityRemoved(Entity entity) {
        if (entity == null) return;
        if (!TrackerUtils.isEnabled() && !TrackerUtils.isOn()) return;

        trackedEntities.remove(entity);
        TrackedMob trackedMob = mobData.remove(entity);
        if (trackedMob != null && trackedMob.isValuable()) {
            handleMobDeath();
        }
    
        armorStandSpawnTimes.remove(entity);
        mobData.values().removeIf(mob -> mob.getNameArmorStand() == entity);
    }
    
    private static void handleNewEntity(Entity entity) {
        switch (entity) {
            case MobEntity mobEntity -> handleMobDetected(mobEntity);
            case ArmorStandEntity armorStand -> handleArmorStandDetected(armorStand);
            case PlayerEntity player -> handlePlayerEntityDetected(player);
            default -> {
                break;
            }
        }
    }
    
    private static void handlePlayerEntityDetected(PlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (player == client.player) {
            return;
        }
    
        String playerName = player.getName().getString();
        String displayName = player.getDisplayName().getString();
        boolean isValuablePlayer = VALUABLE_PLAYERENTITY_PATTERN.matcher(playerName).matches() ||
                                   VALUABLE_PLAYERENTITY_PATTERN.matcher(displayName).matches();
        
        if (!isValuablePlayer && !hasNearbyMobArmorStand(player)) {
            return;
        }
        String mobType = "player_" + playerName.toLowerCase().replace(" ", "_");
        TrackedMob trackedPlayer = new TrackedMob(player, mobType);
        trackedPlayer.setDisplayName(displayName.isEmpty() ? playerName : displayName);
        mobData.put(player, trackedPlayer);
        foundVal = true;
        scheduleArmorStandAssociation(trackedPlayer);
    }
    
    /**
     * Check if a player entity has nearby armor stands that suggest it's a mob-like entity
     */
    private static boolean hasNearbyMobArmorStand(PlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return false;
        
        return client.world.getOtherEntities(player, player.getBoundingBox().expand(3.0))
            .stream()
            .anyMatch(entity -> {
                if (!(entity instanceof ArmorStandEntity armorStand)) return false;
                
                String[] possibleNames = {
                    armorStand.getName().getString(),
                    armorStand.getCustomName() != null ? armorStand.getCustomName().getString() : null,
                    armorStand.getDisplayName().getString()
                };
                
                for (String nameToCheck : possibleNames) {
                    if (nameToCheck == null) continue;
                    if (nameToCheck.contains("❤") || 
                        nameToCheck.matches(".*\\d+/\\d+.*") ||
                        nameToCheck.matches(".*\\[Lv\\d+\\].*") ||
                        VALUABLE_MOB_PATTERN.matcher(nameToCheck).find() ||
                        VALUABLE_PLAYERENTITY_PATTERN.matcher(nameToCheck).find()) {
                        return true;
                    }
                }
                
                return false;
            });
    }
    
    private static void handleMobDetected(MobEntity mob) {
        if (mobData.containsKey(mob)) {
            return;
        }
        
        String mobType = mob.getClass().getSimpleName().toLowerCase();
        TrackedMob trackedMob = new TrackedMob(mob, mobType);
        mobData.put(mob, trackedMob);
        
        // Try to find associated armor stand within a short time window
        scheduleArmorStandAssociation(trackedMob);
    }
    
    private static void handleArmorStandDetected(ArmorStandEntity armorStand) {
        armorStandSpawnTimes.put(armorStand, System.currentTimeMillis());
    }
    
    private static void scheduleArmorStandAssociation(TrackedMob trackedMob) {
        Thread.ofVirtual().start(() -> {
            try {
                Thread.sleep(100);
                tryAssociateWithNearbyArmorStands(trackedMob);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    private static void tryAssociateWithNearbyArmorStands(TrackedMob trackedMob) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;
        
        for (Map.Entry<Entity, Long> entry : armorStandSpawnTimes.entrySet()) {
            Entity entity = entry.getKey();
            Long spawnTime = entry.getValue();

            if (entity instanceof ArmorStandEntity armorStand) {
                // Check if armor stand spawned recently (within last 2 seconds)
                long timeDiff = System.currentTimeMillis() - spawnTime;
                if (timeDiff <= 2000) {
                    double distance = trackedMob.mobEntity.distanceTo(armorStand);
                    if (distance <= ASSOCIATION_DISTANCE) {
                        tryAll(armorStand, trackedMob);
                    }
                }
            }
        }
    }

    private static void tryAll(ArmorStandEntity armorStand, TrackedMob trackedMob) {
        String[] possibleNames = {
            armorStand.getName().getString(),
            armorStand.getCustomName() != null ? armorStand.getCustomName().getString() : null,
            armorStand.getDisplayName().getString()
        };

        for (String nameToCheck : possibleNames) {
            if (nameToCheck != null && tryExtractAndAssociateMobInfo(armorStand, nameToCheck, trackedMob)) {
                return;
            }
        }        
    }
    
    private static boolean tryExtractAndAssociateMobInfo(ArmorStandEntity armorStand, String nameToCheck, TrackedMob trackedMob) {
        if (!nameToCheck.contains("[Lv") || !nameToCheck.contains("❤")) {  return false;   }
        Matcher matcher = MOB_INFO_PATTERN.matcher(nameToCheck);
        if (matcher.matches()) {
            String mobName = matcher.group(2).trim();
            int level = Integer.parseInt(matcher.group(1));
            
            trackedMob.setDisplayName(mobName);
            trackedMob.setLevel(level);
            trackedMob.setNameArmorStand(armorStand);
            
            if (trackedMob.isValuable()) {
                foundVal = true;
            }
            return true;
        }
        return false;
    }
    
    private static void handleMobDeath() {
        scanInventoryForDrops();
        cleanup();
    }

    public static void onDmgAni() {
        if (foundVal) {
            me.valkeea.fishyaddons.tracker.InventoryTracker.onValuableEntityDamaged();
        }
    }
    
    private static void scanInventoryForDrops() {
        me.valkeea.fishyaddons.tracker.InventoryTracker.onValuableEntityDeath();
    }
    
    public static void cleanup() {
        trackedEntities.removeIf(entity -> entity.isRemoved() || !entity.isAlive());
        mobData.entrySet().removeIf(entry -> 
            entry.getKey().isRemoved() || !entry.getKey().isAlive());
    
        foundVal = mobData.values().stream().anyMatch(TrackedMob::isValuable);
        
        // Clean up old armor stand spawn times
        long currentTime = System.currentTimeMillis();
        armorStandSpawnTimes.entrySet().removeIf(entry -> 
            currentTime - entry.getValue() > 10000 || entry.getKey().isRemoved());
        
        // Clean up tracked player entities that haven't been confirmed as valuable
        mobData.entrySet().removeIf(entry -> {
            TrackedMob mob = entry.getValue();
            if (mob.mobEntity instanceof PlayerEntity && 
                mob.mobType.startsWith("player_") &&
                !mob.isValuable() &&
                (currentTime - mob.getSpawnTime()) > 5000) {
                
                trackedEntities.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }

    public static void cleanVal() {
        if (foundVal) {
            foundVal = false;
        }
    }
    
    public static Set<Entity> getTrackedEntities() {
        return new HashSet<>(trackedEntities);
    }
    
    public static Map<Entity, TrackedMob> getTrackedMobs() {
        return new HashMap<>(mobData);
    }
    
    public static boolean isEntityTracked(Entity entity) {
        return trackedEntities.contains(entity);
    }
    
    public static List<TrackedMob> getValuableMobs() {
        return mobData.values().stream()
                .filter(TrackedMob::isValuable)
                .collect(ArrayList::new, List::add, List::addAll);
    }
}
