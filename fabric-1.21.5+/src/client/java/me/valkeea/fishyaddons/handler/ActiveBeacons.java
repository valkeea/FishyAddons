package me.valkeea.fishyaddons.handler;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.render.Beacon;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class ActiveBeacons {
    private ActiveBeacons() {}
    private static final List<BeaconData> beacons = new ArrayList<>();
    private static final double HIDE_DISTANCE = 5.0;
    private static final long GRACE_PERIOD_MS = 5000; 

    private static BeaconData lastBeacon = null;
    private static boolean hideNear = false;
    private static long duration = 60000;

    public static void refresh() {
        duration = FishyConfig.getInt(Key.RENDER_COORD_MS, 60000) * 1000L;
        hideNear = FishyConfig.getState(Key.RENDER_COORD_HIDE_CLOSE, true);
    }

    public static void init() {
        refresh();
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (beacons.isEmpty()) return;

            long now = System.currentTimeMillis();
            beacons.removeIf(beacon -> now - beacon.setTime > duration);

            if (hideNear) {
                renderFar(context);
            } else {
                renderAll(context);
            }
        });
    }
    
    private static void renderFar(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        Vec3d playerPos = client.player.getPos();
        long now = System.currentTimeMillis();
        
        beacons.removeIf(beacon -> {
            Vec3d beaconCenter = new Vec3d(
                beacon.getPos().getX() + 0.5, 
                beacon.getPos().getY() + 0.5, 
                beacon.getPos().getZ() + 0.5
            );
            double distance = playerPos.distanceTo(beaconCenter);
            
            if (distance <= HIDE_DISTANCE) {
                long beaconAge = now - beacon.getSetTime();
                
                // Remove if self-spawned and past grace period
                if (beaconAge <= GRACE_PERIOD_MS) {
                    Beacon.renderBeacon(context, beacon);
                    return false;
                } else {
                    return true;
                }
            } else {
                Beacon.renderBeacon(context, beacon);
                return false;
            }
        });
    }
    
    private static void renderAll(WorldRenderContext context) {
        for (BeaconData beacon : beacons) {
            Beacon.renderBeacon(context, beacon);
        }
    }  

    public static void clearBeacons() {
        if (!beacons.isEmpty()) {
            beacons.clear();
        }
    }

    /**
     * Adds a beacon at the specified position with color and optional label.
     * The position is converted to a centered BlockPos and empty titles are handled at render.
     */
    public static void setBeacon(BlockPos pos, int colorARGB, String displayLabel) {
        beacons.add(new BeaconData(getActualPos(new Vec3d(pos)), colorARGB, displayLabel));
        lastBeacon = beacons.get(beacons.size() - 1);
    }     

    /**
     * Convert Vec3d to centered BlockPos with proper flooring for negative coordinates.
     */
    public static BlockPos getActualPos(Vec3d pos) {
        return new BlockPos(
            (int) (pos.x < 0 ? Math.floor(pos.x) - 1 : Math.floor(pos.x)),
            (int) (Math.floor(pos.y) - 1),
            (int) (pos.z < 0 ? Math.floor(pos.z) - 1 : Math.floor(pos.z))
        );
    }

    public static void redrawLast() {
        if (lastBeacon != null) {
            beacons.add(new BeaconData(lastBeacon.pos, lastBeacon.color, lastBeacon.label));
            lastBeacon = beacons.get(beacons.size() - 1);
        }
    }

    public static void redraw(BlockPos pos, String title) {
        BlockPos actualPos = getActualPos(new Vec3d(pos));
        
        beacons.removeIf(beacon -> beacon.getPos().equals(actualPos));
        
        int color = FishyConfig.getInt(Key.RENDER_COORD_COLOR, -5653771);
        String label = (title != null && !title.isEmpty()) ? title : "";
        setBeacon(pos, color, label);
    }

    public static void removeBeaconAt(BlockPos pos) {
        beacons.removeIf(beacon -> beacon.getPos().equals(getActualPos(new Vec3d(pos))));
    }

    /**
     * Data class representing a beacon with position, color, label, and timestamp.
     */
    public static class BeaconData {
        final BlockPos pos;
        final int color;
        final String label;
        final long setTime;

        BeaconData(BlockPos pos, int color, @Nullable String label) {
            this.pos = pos;
            this.color = color;
            this.label = label;
            this.setTime = System.currentTimeMillis();
        }

        public BlockPos getPos() {
            return pos;
        }
        public int getColor() {
            return color;
        }

        public String getLabel() {
            return label != null ? label : "";
        }

        public long getSetTime() {
            return setTime;
        }
    }
}