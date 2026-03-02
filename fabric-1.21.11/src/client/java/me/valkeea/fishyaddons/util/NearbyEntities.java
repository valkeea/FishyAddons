package me.valkeea.fishyaddons.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.valkeea.fishyaddons.api.skyblock.GameMode;
import me.valkeea.fishyaddons.feature.skyblock.CatchAlert;
import me.valkeea.fishyaddons.feature.skyblock.FishingHotspot;
import me.valkeea.fishyaddons.tracker.profit.ValuableMobs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.Vec3d;

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

        var client = MinecraftClient.getInstance();
        var player = client.player;
        var world = client.world;
        if (world == null || player == null) return;
        
        if (tickCounter % 200 == 0) {
            labels.clear();
            obfuscation.clear();
        }
        
        List<ArmorStandEntity> nearbyHspts = new ArrayList<>();
        List<ArmorStandEntity> nearbyVals = new ArrayList<>();
        boolean fishReady = false;

        for (var stand : findArmorStands(world, player, RADIUS)) {

            var labelText = extractLabel(stand);
            if (isValidLabel(labelText)) {
                if (FishingHotspot.isHotspotType(labelText)) {
                    nearbyHspts.add(stand);
                } else if (ValuableMobs.isValArmorstand(labelText, stand)) {
                    nearbyVals.add(stand);
                } else if (CatchAlert.isFishingAlert(labelText)) fishReady = true;
            }
        }

        FishingHotspot.update(nearbyHspts);
        ValuableMobs.update(nearbyVals);
        CatchAlert.update(fishReady);
    }

    /**
     * Finds all armor stands within the specified radius
     */
    public static List<ArmorStandEntity> findArmorStands(ClientWorld world, ClientPlayerEntity player, double radius) {

        List<ArmorStandEntity> stands = new ArrayList<>();

        for (var entity : world.getEntitiesByClass(
                ArmorStandEntity.class,
                player.getBoundingBox().expand(radius),
                e -> true)) {

            if (entity instanceof ArmorStandEntity stand) {
                stands.add(stand);
            }
        }
        
        return stands;
    }

    /**
     * Checks view based on camera direction
     */
    public static boolean lookingAt(ArmorStandEntity stand) {
        
        var mc = MinecraftClient.getInstance();
        var player = mc.player;
        if (player == null || mc.world == null) return false;

        double distance = player.distanceTo(stand);
        if (distance < 8.0) return true;
        
        var cameraPos = mc.gameRenderer.getCamera().getCameraPos();

        float yaw = mc.gameRenderer.getCamera().getYaw();
        float pitch = mc.gameRenderer.getCamera().getPitch();
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        
        var cameraDirection = new Vec3d(
            -Math.sin(yawRad) * Math.cos(pitchRad),
            -Math.sin(pitchRad),
            Math.cos(yawRad) * Math.cos(pitchRad)
        ).normalize();
        
        Vec3d toEntity = stand.getEntityPos().subtract(cameraPos).normalize();
        double dot = cameraDirection.dotProduct(toEntity);
        double fovCos = Math.cos(Math.toRadians(60.0));
        
        return dot > fovCos;
    }    

    /**
     * Checks if an armor stand is in radius of the player
     */
    public static boolean isInRange(ArmorStandEntity stand, double radius) {
        var client = MinecraftClient.getInstance();
        var player = client.player;
        if (player == null || client.world == null) return false;

        return player.squaredDistanceTo(stand) <= radius * radius;
    }

    /**
     * Extracts the label text from an armor stand.
     * Can be used for any mob after spawn.
     */
    public static String extractLabel(ArmorStandEntity stand) {
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
