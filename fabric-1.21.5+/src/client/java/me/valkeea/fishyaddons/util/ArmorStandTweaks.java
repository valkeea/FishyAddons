package me.valkeea.fishyaddons.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;


public class ArmorStandTweaks {
    private ArmorStandTweaks() {}

    public static boolean isHspt(String labelText) {
        if (labelText == null || labelText.isEmpty()) return false;
        
        if (labelText.contains("HOTSPOT")) {
            return true;
        }

        if (labelText.contains("Fishing Speed") && labelText.contains("☂")) {
            return true;
        }
        
        if (labelText.contains("⛃") && labelText.contains("Treasure Chance")) {
            return true;
        }

        if (labelText.contains("♔") && labelText.contains("Trophy Fish Chance")) {
            return true;
        }        

        if (labelText.contains("Sea Creature Chance")) {
            return true;
        }
        
        return labelText.contains("Double Hook Chance") && labelText.contains("⚓");
    }

    /**
     * Determines if an armor stand label should be hidden from rendering based on distance
     * @param labelText The text of the label to check
     * @return true if the label should be hidden, false otherwise
     */
    public static boolean shouldHideArmorStand(String labelText) {
        if (!isValidLabel(labelText)) return false;
        if (!isHsptHidingEnabled()) return false;
        if (!isHspt(labelText)) return false;

        float configuredDistance = me.valkeea.fishyaddons.handler.SkyblockCleaner.getHotspotDistance();
        if (configuredDistance <= 0.0f) {
            return true;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return false;

        return isArmorStandWithinDistance(client, labelText, configuredDistance);
    }

    private static boolean isValidLabel(String labelText) {
        return labelText != null && !labelText.isEmpty();
    }

    private static boolean isHsptHidingEnabled() {
        return me.valkeea.fishyaddons.handler.SkyblockCleaner.shouldHideHotspot();
    }

    private static boolean isArmorStandWithinDistance(MinecraftClient client, String labelText, float configuredDistance) {
        for (Entity entity : client.world.getEntitiesByClass(
                ArmorStandEntity.class,
                client.player.getBoundingBox().expand(configuredDistance + 1.0),
                e -> true)) {
            if (!(entity instanceof ArmorStandEntity armorStand)) continue;

            if (nameMatches(armorStand, labelText)) {
                double distance = client.player.distanceTo(armorStand);
                return distance <= configuredDistance;
            }
        }
        return false;
    }

    private static boolean nameMatches(ArmorStandEntity armorStand, String labelText) {
        String[] possibleNames = {
            armorStand.getName().getString(),
            armorStand.getCustomName() != null ? armorStand.getCustomName().getString() : null,
            armorStand.getDisplayName().getString()
        };

        for (String nameToCheck : possibleNames) {
            if (nameToCheck != null && nameToCheck.equals(labelText)) {
                return true;
            }
        }
        return false;
    }
}
