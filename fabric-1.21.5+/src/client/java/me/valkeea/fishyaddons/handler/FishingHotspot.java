package me.valkeea.fishyaddons.handler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.tool.FishyMode;
import me.valkeea.fishyaddons.util.FishyNotis;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class FishingHotspot {
    private FishingHotspot() {}

    // --- Hotspot Tracker ---
    private static final Set<String> visited = new HashSet<>();
    private static boolean track = false;
    private static boolean announce = false;
    private static int tickCounter = 0;

    public static void refresh() {
        track = FishyConfig.getState(Key.TRACK_HOTSPOT, false);
        announce = FishyConfig.getState(Key.ANNOUNCE_HOTSPOT, false);
        visited.clear();
        tickCounter = 0;
    }

    /**
     * Checks if a tracked hotspot armor stand is removed and the player is still within range
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
                    me.valkeea.fishyaddons.util.PlaySound.hotspotAlarm();
                }
            }
        }
    }

    /**
     * Checks for new hotspots every second
     */
    public static void tick() {
        tickCounter++;
        if (visited.size() > 3) {
            visited.clear();
        }
        if (tickCounter % 20 == 0) {
            checkClosest();
        }
    }
    
    private static void checkClosest() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null || !track) {  return; }
        
        ArmorStandEntity target = findClosestVisible(client);
        if (target != null && isViewing(target)) {
            Text label = target.getCustomName() != null
                ? target.getCustomName()
                : Text.literal("");
            String labelString = label.getString();
            String hotspotKey = createKey(target.getBlockPos(), labelString);

            if (!visited.contains(hotspotKey)) {
                announce(target, labelString, label);
                visited.add(hotspotKey);
            }
        }
    }
    
    private static ArmorStandEntity findClosestVisible(MinecraftClient client) {
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
    
    private static String createKey(BlockPos pos, String labelText) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ() + ":" + labelText;
    }

    /**
     * Finds all hotspot armor stands within the specified radius
     * @param radius The search radius
     * @param forHiding If true, finds all hotspot-related stands; if false, only specific hotspot types
     * @return List of hotspot armor stands within radius
     */
    private static List<ArmorStandEntity> findHotspotArmorStands(double radius, boolean forHiding) {
        MinecraftClient client = MinecraftClient.getInstance();
        List<ArmorStandEntity> hotspots = new ArrayList<>();
        
        if (client.player == null || client.world == null) return hotspots;

        for (Entity entity : client.world.getEntitiesByClass(
                ArmorStandEntity.class,
                client.player.getBoundingBox().expand(radius),
                e -> true)) {

            if (entity instanceof ArmorStandEntity armorStand) {
                String labelText = extractLabel(armorStand);
                boolean isValid = isValidLabel(labelText);

                boolean isRelevantHotspot = isValid &&
                (forHiding ? isHotspotForHiding(labelText) : typesHas(labelText));

                if (isRelevantHotspot) {
                    hotspots.add(armorStand);
                }
            }
        }
        
        return hotspots;
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

    private static boolean isHotspotForHiding(String labelText) {
        return isHotspotLabel(labelText) || typesHas(labelText);
    }

    private static boolean isHotspotLabel(String labelText) {
        return labelText.contains("HOTSPOT");
    }

    private static boolean typesHas(String labelText) {
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

    private static boolean isViewing(ArmorStandEntity armorStand) {
        var client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return false;
        if (!client.player.canSee(armorStand)) return false;
        
        Vec3d playerPos = client.player.getEyePos();
        Vec3d armorStandPos = armorStand.getPos();
        Vec3d lookDirection = client.player.getRotationVector();
        Vec3d toArmorStand = armorStandPos.subtract(playerPos).normalize();
        
        double dotProduct = lookDirection.dotProduct(toArmorStand);
        return dotProduct > 0.5;
    }

    private static boolean nameMatches(ArmorStandEntity armorStand, String labelText) {
        String name = extractLabel(armorStand);
        return name.equals(labelText);
    }
    
    private static String extractLabel(ArmorStandEntity armorStand) {
        if (armorStand.getCustomName() == null) { return ""; }
            return armorStand.getCustomName().getString();
    }

    private static boolean isValidLabel(String labelText) {
        return labelText != null && !labelText.isEmpty();
    }

    private static void announce(ArmorStandEntity hotspot, String labelText, Text label) {
        String coords = String.format("x: %.0f, y: %.0f, z: %.0f", hotspot.getX(), hotspot.getY(), hotspot.getZ());
        
        if (announce) {
            MinecraftClient.getInstance().player.networkHandler.sendChatMessage(
                "/pc " + coords + " " + labelText);
        } else {

            MutableText shareButton = Text.literal(" [")
            .styled(style -> style.withColor(0xFF808080))
            .append((Text.literal("Chat"))
            .styled(style -> style.withClickEvent(
                new net.minecraft.text.ClickEvent.RunCommand("/pc " + coords + " " + labelText)
            ).withColor(FishyMode.getCmdColor())))
            .append(Text.literal("]").styled(style -> style.withColor(0xFF808080)));

            FishyNotis.alert(label.copy()
            .append(Text.literal(
                " " + String.format("x: %.0f y: %.0f z: %.0f", hotspot.getX(), hotspot.getY(), hotspot.getZ()))
                .styled(style -> style.withColor(0xFF808080))).append(shareButton));

            ActiveBeacons.setBeacon(hotspot.getBlockPos(), 0xFF1E90FF, labelText);
        }
    }    
}
