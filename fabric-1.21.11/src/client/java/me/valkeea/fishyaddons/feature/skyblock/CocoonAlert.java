package me.valkeea.fishyaddons.feature.skyblock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import me.valkeea.fishyaddons.api.skyblock.GameChat;
import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas.Island;
import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.hud.elements.simple.TitleDisplay;
import me.valkeea.fishyaddons.tool.RunDelayed;
import me.valkeea.fishyaddons.tracker.profit.ValuableMobs;
import me.valkeea.fishyaddons.util.FishyNotis;
import me.valkeea.fishyaddons.util.text.ChatButton;
import me.valkeea.fishyaddons.util.text.StringUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public class CocoonAlert {
    private CocoonAlert() {}

    private static long lastCleanupTime = 0;
    private static final Map<Double, List<SpawnedStand>> recentStandsByX = new HashMap<>();
    private static final Map<Vec3d, Long> processedStands = new HashMap<>();
    private static final Map<Vec3d, PendingAlert> pendingAlerts = new HashMap<>();
    
    private static final UUID TARGET_UUID = UUID.fromString("d88c6ff7-1185-3e93-bffd-fce06348b05f");
    
    private record PendingAlert(Vec3d location, long scheduledTime) {}

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
    
    private record SpawnedStand(ArmorStandEntity stand, long spawnTime) {}

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
        
        var armorStand = (ArmorStandEntity) entity;
        var player = MinecraftClient.getInstance().player;
        
        if (!isValid(armorStand, player)) return;
        
        List<ArmorStandEntity> recentCluster = addToClusterAndGet(armorStand, currentTime);
        if (recentCluster.isEmpty()) return;
        
        Vec3d center = calcCenter(recentCluster);

        if (center == null) return;
        if (wasAlerted(center, currentTime)) return;
        
        processedStands.put(center, currentTime);
        
        if (shouldAlert(center, armorStand, player)) {
            scheduleDelayedAlert(center);
        }
    }
    
    private static boolean shouldProcess(Entity entity) {
        if (!enabled || !validGameMode || !validIsland) return false;
        if (!(entity instanceof ArmorStandEntity stand)) return false;
        var name = stand.getCustomName();
        return name == null || name.getString().isEmpty();
    }
    
    private static boolean isValid(ArmorStandEntity stand, ClientPlayerEntity player) {
        if (player == null) return false;
        return player.distanceTo(stand) <= 20.0;
    }
    
    /**
     * Adds the armor stand to a cluster based on X-coordinate and returns
     * the current cluster if it meets size and profile criteria
     */
    private static List<ArmorStandEntity> addToClusterAndGet(ArmorStandEntity stand, long currentTime) {
        var pos = stand.getEntityPos();
        double xRounded = Math.round(pos.x / X_ROUNDING) * X_ROUNDING;
        
        recentStandsByX.computeIfAbsent(xRounded, k -> new ArrayList<>())
            .add(new SpawnedStand(stand, currentTime));
        
        var standsAtX = recentStandsByX.get(xRounded);
        if (standsAtX == null) return List.of();
        
        List<ArmorStandEntity> recentCluster = standsAtX.stream()
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
    private static Vec3d calcCenter(List<ArmorStandEntity> recentCluster) {
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        double sumX = 0;
        double sumY = 0;
        double sumZ = 0;
        
        for (var stand : recentCluster) {
            var p = stand.getEntityPos();
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
        
        return new Vec3d(sumX / recentCluster.size(), sumY / recentCluster.size(), sumZ / recentCluster.size());
    }
    
    private static boolean wasAlerted(Vec3d center, long currentTime) {
        return processedStands.entrySet().stream()
            .anyMatch(e -> center.distanceTo(e.getKey()) < ALERT_DISTANCE_THRESHOLD && 
                          currentTime - e.getValue() < ALERT_COOLDOWN);
    }
    
    private static boolean shouldAlert(Vec3d center, ArmorStandEntity armorStand, ClientPlayerEntity player) {
        double distance = player.distanceTo(armorStand);
        boolean isClose = distance <= CLOSE_DISTANCE;
        boolean inView = !isClose && isInFieldOfView(center, FOV_DEGREES);
        return isClose || inView;
    }

    private static boolean isInFieldOfView(Vec3d position, double fovDegrees) {
        var mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return false;
        
        var camera = mc.gameRenderer.getCamera();
        var cameraPos = camera.getCameraPos();
        var cameraDirection = getCameraDirection(camera);
        Vec3d toPosition = position.subtract(cameraPos).normalize();
        double dot = cameraDirection.dotProduct(toPosition);
        double fovCos = Math.cos(Math.toRadians(fovDegrees));
        
        return dot > fovCos;
    }

    private static Vec3d getCameraDirection(net.minecraft.client.render.Camera camera) {
        float yaw = camera.getYaw();
        float pitch = camera.getPitch();
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        
        return new Vec3d(
            -Math.sin(yawRad) * Math.cos(pitchRad),
            -Math.sin(pitchRad),
            Math.cos(yawRad) * Math.cos(pitchRad)
        ).normalize();
    }
    
    private static boolean hasTargetProfile(ArmorStandEntity armorStand) {

        try {
            var headSlot = armorStand.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD);
            if (headSlot == null || headSlot.isEmpty()) return false;
            if (!headSlot.isOf(Items.PLAYER_HEAD)) return false;
            
            var profile = headSlot.getOrDefault(DataComponentTypes.PROFILE, null);
            if (profile == null) return false;
            
            var gameProfile = profile.getGameProfile();
            if (gameProfile == null) return false;

            return TARGET_UUID.equals(gameProfile.id());
            
        } catch (Exception e) {
            return false;
        }
    }

    private static void scheduleDelayedAlert(Vec3d location) {

        long scheduleTime = System.currentTimeMillis();
        pendingAlerts.put(location, new PendingAlert(location, scheduleTime));

        RunDelayed.run(() -> {
            try {
                processDelayedAlert(location, scheduleTime);
            } catch (Exception e) {
                // Ignore
            }
        }, ALERT_DELAY_MS, null);
    }
    
    private static void processDelayedAlert(Vec3d loc, long originalScheduleTime) {

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
        
        var client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        String mobName = ValuableMobs.checkRecentDeath(loc);
        Text msg;

        if (mobName != null) {
            var displayed = StringUtils.capitalize(mobName);
            msg = Text.literal("§5" + displayed + " §ccocooned!")
            .append(createBtn(loc, displayed));
        } else msg = Text.literal("§cMob cocooned!");


        FishyNotis.send(msg);
        
        if (FishyConfig.getState(Key.ALERT_COCOON, false)) {
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
        if (MinecraftClient.getInstance().inGameHud != null) {
            TitleDisplay.setTitle("COCOON", 0xFF8B0000);
            me.valkeea.fishyaddons.tool.PlaySound.cocoonAlarm();            
        }
    }

    private static Text createBtn(Vec3d loc, String mobName) {
        return ChatButton.create( 
            GameChat.channelPrefix() + " " + String.format("x: %d, y: %d, z: %d %s cocooned!",
            (int)loc.x, (int)loc.y, (int)loc.z, mobName),
            "Share"
        );
    }

    public static void refresh() {
        enabled = FishyConfig.getState(Key.TRACK_COCOON, false);
    }
}
