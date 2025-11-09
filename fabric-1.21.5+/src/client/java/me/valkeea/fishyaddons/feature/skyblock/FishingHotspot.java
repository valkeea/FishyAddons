package me.valkeea.fishyaddons.feature.skyblock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.valkeea.fishyaddons.api.skyblock.GameChat;
import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.feature.waypoints.TempWaypoint;
import me.valkeea.fishyaddons.tracker.fishing.ScStats;
import me.valkeea.fishyaddons.util.FishyNotis;
import me.valkeea.fishyaddons.util.NearbyEntities;
import me.valkeea.fishyaddons.util.text.ChatButton;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class FishingHotspot {
    private FishingHotspot() {}

    // --- Hotspot Tracker ---
    private static final Set<String> visited = new HashSet<>();
    private static boolean track = false;
    private static boolean trackActivity = false;
    private static boolean announce = false;

    public static void refresh() {
        track = FishyConfig.getState(Key.TRACK_HOTSPOT, false);
        trackActivity = FishyConfig.getState(Key.TRACK_HOTSPOT, false) || FishyConfig.getState(Key.TRACK_SCS, false);
        announce = FishyConfig.getState(Key.ANNOUNCE_HOTSPOT, false);
        visited.clear();
    }

    /**
     * Warns if a tracked hotspot armor stand is removed and the player is still within range
     */
    public static void onEntityRemoved(Entity entity) {
        if (!track) return;
        if (entity instanceof ArmorStandEntity armorStand) {
            String labelText = extractLabel(armorStand);
            if (!isValidLabel(labelText)) return;

            if (visited.contains(createKey(armorStand.getBlockPos(), labelText))) {
                ArmorStandEntity closest = findClosestVisible(MinecraftClient.getInstance());

                if (closest != null && extractLabel(closest).equals(labelText)) {
                    FishyNotis.warn2("Hotspot expired!");
                    me.valkeea.fishyaddons.tool.PlaySound.hotspotAlarm();
                }
            }
        }
    }

    private static ArmorStandEntity findClosestVisible(MinecraftClient client) {
        if (visited.size() > 3) {
            visited.clear();
        }

        ArmorStandEntity target = null;
        double targetDistance = Double.MAX_VALUE;
        
        for (ArmorStandEntity candidate : findHotspotArmorStands(50.0, false)) {
            double distance = client.player.distanceTo(candidate);
            if (distance < targetDistance) {
                targetDistance = distance;
                target = candidate;
            }
        }
        return target;
    }    

    /**
     * Finds armor stands that match hotspot criteria
     */
    public static List<ArmorStandEntity> findHotspotArmorStands(double radius, boolean forHiding) {
        List<ArmorStandEntity> hotspots = new ArrayList<>();
        
        for (ArmorStandEntity armorStand : NearbyEntities.findArmorStands(radius)) {
            String labelText = extractLabel(armorStand);
            boolean isValid = isValidLabel(labelText);

            boolean isRelevantHotspot = isValid &&
                (forHiding ? isHotspotForHiding(labelText) : isHotspotType(labelText));

            if (isRelevantHotspot) {
                hotspots.add(armorStand);
            }
        }
        
        return hotspots;
    }      

    /**
     * Called by EntityTracker to check if an armor stand represents a hotspot
     */
    public static void update(List<ArmorStandEntity> nearbyHspts) {
        if (!trackActivity) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        ArmorStandEntity closestHotspot = null;

        for (ArmorStandEntity armorStand : nearbyHspts) {
            if (NearbyEntities.lookingAt(armorStand)) {
                Text label = armorStand.getCustomName() != null
                    ? armorStand.getCustomName()
                    : Text.literal("");
                String labelString = label.getString();
                var pos = armorStand.getBlockPos();
                if (client.player.distanceTo(armorStand) <= 14.0) {
                    closestHotspot = armorStand;
                }
                String hotspotKey = createKey(pos, labelString);

                if (!visited.contains(hotspotKey) && track) {
                    announce(armorStand, labelString, label);
                    visited.add(hotspotKey);
                }
            }
        }

        ScStats.setSubArea(closestHotspot);
    }

    public static boolean isHotspot(String labelText) {
        return isHotspotType(labelText);
    }
    
    private static String createKey(BlockPos pos, String labelText) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ() + ":" + labelText;
    }    

    // --- Hotspot Hider ---
    public static boolean shouldHide(String labelText) {
        if (!isValidLabel(labelText)) return false;
        if (!SkyblockCleaner.shouldHideHotspot()) return false;
        if (!isHotspotForHiding(labelText)) return false;

        float configuredDistance = SkyblockCleaner.getHotspotDistance();
        if (configuredDistance <= 0.0f) {
            return true;
        }

        return isWithinDistance(labelText, configuredDistance);
    }

    private static boolean isWithinDistance(String labelText, float configuredDistance) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return false;

        for (ArmorStandEntity armorStand : findHotspotArmorStands(configuredDistance + 1.0, true)) {
            if (nameMatches(armorStand, labelText)) {
                double distance = client.player.distanceTo(armorStand);
                return distance <= configuredDistance;
            }
        }
        return false;
    }    

    private static boolean nameMatches(ArmorStandEntity armorStand, String labelText) {
        String name = NearbyEntities.extractLabel(armorStand);
        return name.equals(labelText);
    }
    
    private static String extractLabel(ArmorStandEntity armorStand) {
        return NearbyEntities.extractLabel(armorStand);
    }

    private static boolean isValidLabel(String labelText) {
        return NearbyEntities.isValidLabel(labelText);
    }

    /**
     * Checks if a label represents a hotspot type
     */
    public static boolean isHotspotType(String labelText) {
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
     * Checks if a label represents a hotspot for hiding purposes
     */
    public static boolean isHotspotForHiding(String labelText) {
        return isHotspotLabel(labelText) || isHotspotType(labelText);
    }

    /**
     * Checks if a label contains "HOTSPOT"
     */
    public static boolean isHotspotLabel(String labelText) {
        return labelText.contains("HOTSPOT");
    }    

    private static void announce(ArmorStandEntity hotspot, String labelText, Text label) {

        var blockPos = hotspot.getBlockPos();
        double x = blockPos.getX();
        double y = blockPos.getY();
        double z = blockPos.getZ();
        var coords = String.format("x: %.0f, y: %.0f, z: %.0f", x, y, z);

        if (announce && GameChat.isInParty()) {
            MinecraftClient.getInstance().player.networkHandler.sendChatMessage(
                "/pc " + coords + " " + labelText);

        } else if (FishyConfig.getState(Key.HSPT_COORDS, false)) {
            var pos = String.format("%.0f %.0f %.0f", x, y, z);

            String chatCmd = GameChat.channelPrefix() + coords + " " + labelText;
            String hspCmd = "/fa coords redraw " + pos + " " + labelText;
            String hideCmd = "/fa coords hide " + pos;

            var shareBtn = ChatButton.create(chatCmd, "Chat");
            var redrawBtn = ChatButton.create(hspCmd, "Redraw");
            var hideBtn = ChatButton.create(hideCmd, "Hide");

            FishyNotis.alert(label.copy().append(Text.literal(" " + coords + " ").styled(
                style -> style.withColor(0xFF808080))).append(shareBtn).append(redrawBtn).append(hideBtn));

            var color = label.getSiblings().isEmpty()
                ? null
                : label.getSiblings().get(0).getStyle().getColor();

            int hsptColor = color != null ? color.getRgb() : 0xFF1E90FF;    
            TempWaypoint.setBeacon(hotspot.getBlockPos(), hsptColor, labelText, 15000L);
        }
    }    
}
