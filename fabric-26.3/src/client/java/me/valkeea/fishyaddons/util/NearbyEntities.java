package me.valkeea.fishyaddons.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.valkeea.fishyaddons.api.skyblock.GameMode;
import me.valkeea.fishyaddons.feature.skyblock.FishingHotspot;
import me.valkeea.fishyaddons.tracker.profit.ValuableMobs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;

public class NearbyEntities {
    private NearbyEntities() {}

    private static int tickCounter = 0;
    private static final double RADIUS = 50.0;
    private static final Map<Integer, String> labels = new HashMap<>();
    private static final Map<String, String> obfuscation = new HashMap<>();

    public static void tick() {
        tickCounter++;
        boolean active = ValuableMobs.hasTrackedMobs();
        int scanInterval = active ? 2 : 10;
        
        if (tickCounter % scanInterval == 0) {
            checkClosest();
            if (active) labels.clear();
        }
    }

    private static void checkClosest() {

        if (!GameMode.skyblock()) return;

        var mc = Minecraft.getInstance();
        var player = mc.player;
        var world = mc.level;
        if (world == null || player == null) return;
        
        if (tickCounter % 200 == 0) {
            labels.clear();
            obfuscation.clear();
        }
        
        List<ArmorStand> nearbyHspts = new ArrayList<>();
        List<ArmorStand> nearbyVals = new ArrayList<>();

        for (var stand : findArmorStands(world, player, RADIUS)) {

            var labelText = extractLabel(stand);
            if (isValidLabel(labelText)) {
                if (FishingHotspot.isHotspotType(labelText)) {
                    nearbyHspts.add(stand);
                } else if (ValuableMobs.isValArmorstand(labelText, stand)) {
                    nearbyVals.add(stand);
                }
            }
        }

        FishingHotspot.update(nearbyHspts);
        ValuableMobs.update(nearbyVals);
    }

    /**
     * Finds all armor stands within the specified radius
     */
    public static List<ArmorStand> findArmorStands(ClientLevel world, LocalPlayer player, double radius) {

        List<ArmorStand> stands = new ArrayList<>();

        for (var entity : world.getEntitiesOfClass(
                ArmorStand.class,
                player.getBoundingBox().inflate(radius),
                e -> true)) {

            if (entity instanceof ArmorStand stand) {
                stands.add(stand);
            }
        }
        
        return stands;
    }

    /**
     * Checks view based on camera direction
     */
    public static boolean lookingAt(ArmorStand stand) {
        
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || mc.level == null) return false;

        double distance = player.distanceTo(stand);
        if (distance < 8.0) return true;
        
        var cameraPos = mc.gameRenderer.mainCamera().position();

        float yaw = mc.gameRenderer.mainCamera().yRot();
        float pitch = mc.gameRenderer.mainCamera().xRot();
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        
        var cameraDirection = new Vec3(
            -Math.sin(yawRad) * Math.cos(pitchRad),
            -Math.sin(pitchRad),
            Math.cos(yawRad) * Math.cos(pitchRad)
        ).normalize();
        
        Vec3 toEntity = stand.position().subtract(cameraPos).normalize();
        double dot = cameraDirection.dot(toEntity);
        double fovCos = Math.cos(Math.toRadians(60.0));
        
        return dot > fovCos;
    }    

    /**
     * Checks if an armor stand is in radius of the player
     */
    public static boolean isInRange(ArmorStand stand, double radius) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || mc.level == null) return false;

        return player.distanceToSqr(stand) <= radius * radius;
    }

    /**
     * Extracts the label text from an armor stand.
     * Can be used for any mob after spawn.
     */
    public static String extractLabel(ArmorStand stand) {
        if (stand.getCustomName() == null) return "";
        
        int entityId = stand.getId();
        var cached = labels.get(entityId);
        if (cached != null) return cached;
        
        var label = stand.getCustomName();
        if (label == null) return "";

        var rawLabel = label.getString();
        var cleaned = cutObfuscation(rawLabel);
        labels.put(entityId, cleaned);
        return cleaned;
    }

    /**
     * Get the name of player entities, including on spawn
     */
    public static String extractDisplayName(Entity entity) {
        var name = entity.getDisplayName();
        return name != null ? name.getString() : "";
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

        var cached = obfuscation.get(text);
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
                result = cleaned.replace("obfuscated", "");
            }
        }
        
        obfuscation.put(text, result);
        return result;
    }    
}
