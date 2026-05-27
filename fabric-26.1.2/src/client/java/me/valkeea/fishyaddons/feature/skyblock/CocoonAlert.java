package me.valkeea.fishyaddons.feature.skyblock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import me.valkeea.fishyaddons.api.skyblock.GameChat;
import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas.Island;
import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.hud.elements.simple.TitleDisplay;
import me.valkeea.fishyaddons.tool.RunDelayed;
import me.valkeea.fishyaddons.tracker.profit.ValuableMobs;
import me.valkeea.fishyaddons.util.FishyNotis;
import me.valkeea.fishyaddons.util.text.ChatButton;
import me.valkeea.fishyaddons.util.text.StringUtils;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class CocoonAlert {
    private CocoonAlert() {}

    private static long lastCleanupTime = 0;
    private static final Map<Double, List<SpawnedStand>> recentStandsByX = new HashMap<>();
    private static final Map<Vec3, Long> processedStands = new HashMap<>();
    private static final Map<Vec3, PendingAlert> pendingAlerts = new HashMap<>();
    
    private static final UUID TARGET_UUID = UUID.fromString("d88c6ff7-1185-3e93-bffd-fce06348b05f");
    
    private record PendingAlert(Vec3 location, long scheduledTime) {}

    private static final int MIN_CLUSTER_SIZE = 3;    
    private static final double FOV_DEGREES = 75.0;
    private static final double MAX_Y_RANGE = 3.0;
    private static final double MAX_Z_RANGE = 2.0;
    private static final double X_ROUNDING = 0.5;
    private static final double CLOSE_DISTANCE = 10.0;    
    private static final double ALERT_DISTANCE_THRESHOLD = 3.0;    
    private static final double NEARBY_CLUSTER_RADIUS = 5.0;    
    private static final long CLUSTER_TIME_WINDOW = 500;
    private static final long ALERT_COOLDOWN = 2000;
    private static final long ALERT_DELAY_MS = 150;
    
    private record SpawnedStand(ArmorStand stand, long spawnTime) {}

    private static boolean enabled = false;
    private static boolean validIsland = false;
    private static boolean validGameMode = false;    

    public static void init() {
        refresh();
        FaEvents.ENVIRONMENT_CHANGE.register(event -> {
            validIsland = event.newIsland != Island.RIFT;
            validGameMode = event.isInSkyblock;
        });
    }

    /** 
     * Detects spawns first using 3D clustering 
     */
    public static void onEntityAdded(Entity entity) {

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCleanupTime > 5000) {
            cleanup();
            lastCleanupTime = currentTime;
        }

        if (!shouldProcess(entity)) return;
        
        var armorStand = (ArmorStand) entity;
        var player = Minecraft.getInstance().player;
        
        if (!isValid(armorStand, player)) return;
        
        List<ArmorStand> recentCluster = addToClusterAndGet(armorStand, currentTime);
        if (recentCluster.isEmpty()) return;
        
        Vec3 center = calcCenter(recentCluster);

        if (center == null) return;
        if (wasAlerted(center, currentTime)) return;
        
        processedStands.put(center, currentTime);
        
        if (shouldAlert(center, armorStand, player)) {
            scheduleDelayedAlert(center);
        }
    }
    
    private static boolean shouldProcess(Entity entity) {
        if (!enabled || !validGameMode || !validIsland) return false;
        if (!(entity instanceof ArmorStand stand)) return false;
        var name = stand.getCustomName();
        return name == null || name.getString().isEmpty();
    }
    
    private static boolean isValid(ArmorStand stand, LocalPlayer player) {
        if (player == null) return false;
        return player.distanceTo(stand) <= 20.0;
    }
    
    /**
     * Adds the armor stand to a cluster based on X-coordinate and returns
     * the current cluster if it meets size and profile criteria
     */
    private static List<ArmorStand> addToClusterAndGet(ArmorStand stand, long currentTime) {
        var pos = stand.position();
        double xRounded = Math.round(pos.x / X_ROUNDING) * X_ROUNDING;
        
        recentStandsByX.computeIfAbsent(xRounded, k -> new ArrayList<>())
            .add(new SpawnedStand(stand, currentTime));
        
        var standsAtX = recentStandsByX.get(xRounded);
        if (standsAtX == null) return List.of();
        
        List<ArmorStand> recentCluster = standsAtX.stream()
            .filter(s -> currentTime - s.spawnTime() <= CLUSTER_TIME_WINDOW)
            .map(s -> s.stand())
            .toList();
        
        if (recentCluster.size() < MIN_CLUSTER_SIZE) return List.of();
        
        boolean hasTargetProfile = recentCluster.stream().anyMatch(CocoonAlert::hasTargetProfile);
        return hasTargetProfile ? recentCluster : List.of();
    }
    
    /**
     * Calculates the center of the cluster and checks Y and Z spread
     */
    private static Vec3 calcCenter(List<ArmorStand> recentCluster) {
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        double sumX = 0;
        double sumY = 0;
        double sumZ = 0;
        
        for (var stand : recentCluster) {
            var p = stand.position();
            double y = p.y;
            double z = p.z;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
            if (z < minZ) minZ = z;
            if (z > maxZ) maxZ = z;
            sumX += p.x;
            sumY += p.y;
            sumZ += p.z;
        }
        
        double yRange = maxY - minY;
        double zRange = maxZ - minZ;
        if (yRange > MAX_Y_RANGE || zRange > MAX_Z_RANGE) return null;
        
        return new Vec3(sumX / recentCluster.size(), sumY / recentCluster.size(), sumZ / recentCluster.size());
    }
    
    private static boolean wasAlerted(Vec3 center, long currentTime) {
        return processedStands.entrySet().stream()
            .anyMatch(e -> center.distanceTo(e.getKey()) < ALERT_DISTANCE_THRESHOLD && 
                          currentTime - e.getValue() < ALERT_COOLDOWN);
    }
    
    private static boolean shouldAlert(Vec3 center, ArmorStand armorStand, LocalPlayer player) {
        double distance = player.distanceTo(armorStand);
        boolean isClose = distance <= CLOSE_DISTANCE;
        boolean inView = !isClose && isInFieldOfView(center, FOV_DEGREES);
        return isClose || inView;
    }

    private static boolean isInFieldOfView(Vec3 position, double fovDegrees) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return false;
        
        var camera = mc.gameRenderer.getMainCamera();
        var cameraPos = camera.position();
        var cameraDirection = getCameraDirection(camera);
        Vec3 toPosition = position.subtract(cameraPos).normalize();
        double dot = cameraDirection.dot(toPosition);
        double fovCos = Math.cos(Math.toRadians(fovDegrees));
        
        return dot > fovCos;
    }

    private static Vec3 getCameraDirection(net.minecraft.client.Camera camera) {
        float yaw = camera.yRot();
        float pitch = camera.xRot();
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        
        return new Vec3(
            -Math.sin(yawRad) * Math.cos(pitchRad),
            -Math.sin(pitchRad),
            Math.cos(yawRad) * Math.cos(pitchRad)
        ).normalize();
    }
    
    private static boolean hasTargetProfile(ArmorStand armorStand) {

        try {
            var headSlot = armorStand.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
            if (headSlot.isEmpty()) return false;
            if (!headSlot.is(Items.PLAYER_HEAD)) return false;
            
            var profile = headSlot.get(DataComponents.PROFILE);
            if (profile == null) return false;
            
            var gameProfile = profile.partialProfile();
            return TARGET_UUID.equals(gameProfile.id());
            
        } catch (Exception _) {
            return false;
        }
    }

    private static void scheduleDelayedAlert(Vec3 location) {

        long scheduleTime = System.currentTimeMillis();
        pendingAlerts.put(location, new PendingAlert(location, scheduleTime));

        RunDelayed.run(() -> {
            try {
                processDelayedAlert(location, scheduleTime);
            } catch (Exception _) {
                // Ignore
            }
        }, ALERT_DELAY_MS, null);
    }
    
    private static void processDelayedAlert(Vec3 loc, long originalScheduleTime) {

        var pending = pendingAlerts.get(loc);
        if (pending == null || pending.scheduledTime() != originalScheduleTime) return;
        
        long nearbyCount = pendingAlerts.entrySet().stream()
            .filter(e -> !e.getKey().equals(loc))
            .filter(e -> e.getKey().distanceTo(loc) < NEARBY_CLUSTER_RADIUS)
            .count();
        
        if (nearbyCount > 0) {
            pendingAlerts.remove(loc);
            return;
        }
        
        pendingAlerts.remove(loc);
        
        var client = Minecraft.getInstance();
        if (client.player == null) return;
        
        String mobName = ValuableMobs.checkRecentDeath(loc);
        Component msg;

        if (mobName != null) {
            var displayed = StringUtils.capitalize(mobName);
            msg = Component.literal("§5" + displayed + " §ccocooned!")
            .append(createBtn(loc, displayed));
        } else msg = Component.literal("§cMob cocooned!");


        FishyNotis.send(msg);
        
        if (Config.get(BooleanKey.ALERT_COCOON)) {
            alert();
        }
    }    

    private static void cleanup() {
        long now = System.currentTimeMillis();

        recentStandsByX.values().forEach(list -> 
            list.removeIf(s -> now - s.spawnTime() > 2000)
        );
        
        recentStandsByX.entrySet().removeIf(e -> e.getValue().isEmpty());
        processedStands.entrySet().removeIf(e -> now - e.getValue() > 10000);
        pendingAlerts.entrySet().removeIf(e -> now - e.getValue().scheduledTime() > 1000);
    }

    private static void alert() {
        if (Minecraft.getInstance().gui != null) {
            TitleDisplay.setTitle("COCOON", 0xFF8B0000);
            me.valkeea.fishyaddons.tool.PlaySound.cocoonAlarm();            
        }
    }

    private static Component createBtn(Vec3 loc, String mobName) {
        return ChatButton.create( 
            GameChat.channelPrefix() + " " + String.format("x: %d, y: %d, z: %d %s cocooned!",
            (int)loc.x, (int)loc.y, (int)loc.z, mobName),
            "Share"
        );
    }

    public static void refresh() {
        enabled = Config.get(BooleanKey.TRACK_COCOON);
    }
}
