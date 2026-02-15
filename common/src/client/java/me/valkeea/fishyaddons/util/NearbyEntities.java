package me.valkeea.fishyaddons.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.valkeea.fishyaddons.api.skyblock.GameMode;
import me.valkeea.fishyaddons.feature.skyblock.FishingHotspot;
import me.valkeea.fishyaddons.tracker.profit.ValuableMobs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.Vec3d;

public class NearbyEntities {
    private NearbyEntities() {}

    private static int tickCounter = 0;
    private static final double RADIUS = 50.0;
    private static final Map<Integer, String> labelCache = new HashMap<>();
    private static final Map<String, String> obfuscationCache = new HashMap<>();

    public static void tick() {
        tickCounter++;
        boolean active = ValuableMobs.hasTrackedMobs();
        int scanInterval = active ? 2 : 10;
        
        if (tickCounter % scanInterval == 0) {
            checkClosest();
            if (active) labelCache.clear();
        }
    }

    private static void checkClosest() {

        if (!GameMode.skyblock()) return;

        var client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;
        
        if (tickCounter % 200 == 0) {
            labelCache.clear();
            obfuscationCache.clear();
        }
        
        List<ArmorStandEntity> nearbyHspts = new ArrayList<>();
        List<ArmorStandEntity> nearbyVals = new ArrayList<>();

        for (var armorStand : findArmorStands(RADIUS)) {

            var labelText = extractLabel(armorStand);
            if (isValidLabel(labelText)) {

                if (FishingHotspot.isHotspotType(labelText)) {
                    nearbyHspts.add(armorStand);
                    continue;
                }

                if (ValuableMobs.isValArmorstand(labelText, armorStand)) {
                    nearbyVals.add(armorStand);
                }                
            }
        }

        FishingHotspot.update(nearbyHspts);
        ValuableMobs.update(nearbyVals);
    }

    /**
     * Finds all armor stands within the specified radius
     */
    public static List<ArmorStandEntity> findArmorStands(double radius) {
        var client = MinecraftClient.getInstance();
        List<ArmorStandEntity> armorStands = new ArrayList<>();
        
        if (client.player == null || client.world == null) return armorStands;

        for (var entity : client.world.getEntitiesByClass(
                ArmorStandEntity.class,
                client.player.getBoundingBox().expand(radius),
                e -> true)) {

            if (entity instanceof ArmorStandEntity armorStand) {
                armorStands.add(armorStand);
            }
        }
        
        return armorStands;
    }

    /**
     * Checks view based on camera direction
     */
    public static boolean lookingAt(ArmorStandEntity armorStand) {
        
        var mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return false;

        double distance = mc.player.distanceTo(armorStand);
        if (distance < 8.0) return true;
        
        var cameraPos = mc.gameRenderer.getCamera().getPos();

        float yaw = mc.gameRenderer.getCamera().getYaw();
        float pitch = mc.gameRenderer.getCamera().getPitch();
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        
        var cameraDirection = new Vec3d(
            -Math.sin(yawRad) * Math.cos(pitchRad),
            -Math.sin(pitchRad),
            Math.cos(yawRad) * Math.cos(pitchRad)
        ).normalize();
        
        Vec3d toEntity = armorStand.getEntityPos().subtract(cameraPos).normalize();
        double dot = cameraDirection.dotProduct(toEntity);
        double fovCos = Math.cos(Math.toRadians(60.0));
        
        return dot > fovCos;
    }    

    /**
     * Checks if an armor stand is in radius of the player
     */
    public static boolean isInRange(ArmorStandEntity armorStand, double radius) {
        var client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return false;

        return client.player.squaredDistanceTo(armorStand) <= radius * radius;
    }

    /**
     * Extracts the label text from an armor stand.
     * Can be used for any mob after spawn.
     */
    public static String extractLabel(ArmorStandEntity armorStand) {
        if (armorStand.getCustomName() == null) { 
            return ""; 
        }
        
        int entityId = armorStand.getId();
        var cached = labelCache.get(entityId);
        if (cached != null) {
            return cached;
        }
        
        var rawLabel = armorStand.getCustomName().getString();
        var cleaned = cutObfuscation(rawLabel);
        labelCache.put(entityId, cleaned);
        return cleaned;
    }

    /**
     * Get the name of player entities, including on spawn
     */
    public static String extractDisplayName(Entity entity) {
        if (entity.getDisplayName() == null) return "";
        return entity.getDisplayName().getString();
    }

    public static boolean isValidLabel(String labelText) {
        return labelText != null && !labelText.isEmpty();
    }

    /**
     * Cleans obfuscated characters from a string.
     * Detects patterns like "aCorrupted" and removes the leading 'a' and trailing 'a'.
     */
    public static String cutObfuscation(String text) {
        if (text == null || text.isEmpty()) return text;

        var cached = obfuscationCache.get(text);
        if (cached != null) return cached;

        var obfuscationPattern = java.util.regex.Pattern.compile(".*\\b([a-z])([A-Z]\\w*).*");
        var matcher = obfuscationPattern.matcher(text);

        String result = text;
        if (matcher.find()) {
            var obfuscatedChar = matcher.group(1);
            
            if (text.endsWith(obfuscatedChar)) {
                var cleaned = text.replaceFirst("\\b" + obfuscatedChar + "(?=[A-Z])", "");
                if (cleaned.endsWith(obfuscatedChar)) {
                    cleaned = cleaned.substring(0, cleaned.length() - 1);
                }
                result = cleaned;
            }
        }
        
        obfuscationCache.put(text, result);
        return result;
    }    
}
