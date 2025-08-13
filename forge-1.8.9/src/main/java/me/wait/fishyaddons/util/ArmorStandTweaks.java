package me.wait.fishyaddons.util;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.util.AxisAlignedBB;

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
    public static boolean shouldHide(String labelText) {
        if (!isValidLabel(labelText)) return false;
        if (!isHsptHidingEnabled()) return false;
        if (!isHspt(labelText)) return false;

        float configuredDistance = me.wait.fishyaddons.handlers.SkyblockCleaner.getHotspotDistance();
        if (configuredDistance <= 0.0f) {
            return true;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return false;

        return isNear(mc, labelText, configuredDistance);
    }

    private static boolean isValidLabel(String labelText) {
        return labelText != null && !labelText.isEmpty();
    }

    private static boolean isHsptHidingEnabled() {
        return me.wait.fishyaddons.handlers.SkyblockCleaner.shouldHideHotspot();
    }

    private static boolean isNear(Minecraft mc, String labelText, float configuredDistance) {
        AxisAlignedBB searchArea = mc.thePlayer.getEntityBoundingBox().expand(configuredDistance + 1.0, configuredDistance + 1.0, configuredDistance + 1.0);
        
        List<Entity> entities = mc.theWorld.getEntitiesWithinAABB(EntityArmorStand.class, searchArea);
        
        for (Entity entity : entities) {
            if (entity instanceof EntityArmorStand) {
                EntityArmorStand armorStand = (EntityArmorStand) entity;
                
                if (nameMatches(armorStand, labelText)) {
                    double distance = mc.thePlayer.getDistanceToEntity(armorStand);
                    return distance <= configuredDistance;
                }
            }
        }
        return false;
    }

    private static boolean nameMatches(EntityArmorStand armorStand, String labelText) {
        if (armorStand.hasCustomName()) {
            String customName = armorStand.getCustomNameTag();
            if (customName != null && customName.equals(labelText)) {
                return true;
            }
        }
        
        String entityName = armorStand.getName();
        if (entityName != null && entityName.equals(labelText)) {
            return true;
        }
        
        return false;
    }
}
