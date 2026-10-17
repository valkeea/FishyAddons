package me.valkeea.fishyaddons.feature.waypoints;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import me.valkeea.fishyaddons.render.Beacon;
import me.valkeea.fishyaddons.vconfig.annotation.VCInit;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

@VCModule
public class NpcWaypoints {
    private NpcWaypoints() {}

    // Keyed by NPC name
    private static final Map<String, NpcBeacon> beacons = new ConcurrentHashMap<>();

    private static final double MAX_RENDER_DIST_SQ = Math.pow(200.0, 2);
    private static final double HIDE_DIST_SQ = Math.pow(4.0, 2);
    private static final long SWEEP_INTERVAL_MS = 1000;
    private static long lastSweep = 0;

    @VCInit
    public static void init() {
        LevelRenderEvents.COLLECT_SUBMITS.register(ctx -> {
            if (beacons.isEmpty()) return;

            long now = System.currentTimeMillis();
            if (now - lastSweep > SWEEP_INTERVAL_MS) {
                lastSweep = now;
                beacons.values().removeIf(beacon -> now - beacon.setTime > beacon.duration);
            }

            var mc = Minecraft.getInstance();
            if (mc.player == null) return;
            Vec3 playerPos = mc.player.position();

            for (NpcBeacon b : beacons.values()) {

                double distanceSq = playerPos.distanceToSqr(Vec3.atCenterOf(b.getPos()));
                if (distanceSq <= HIDE_DIST_SQ) beacons.remove(b.getName());
                if (distanceSq > MAX_RENDER_DIST_SQ) continue;
                Beacon.renderBeacon(ctx, b);
            }
        });
    }

    public static void clearBeacons() {
        beacons.clear();
    }

    public static void setBeacon(BlockPos pos, int colorARGB, String name, long duration) {
        beacons.put(name, new NpcBeacon(pos, colorARGB, name, duration));
    }

    private static class NpcBeacon implements me.valkeea.fishyaddons.render.IBeaconData {
        private static final long REFRESH_MS = 250;

        private final BlockPos pos;
        private final int color;
        private final String name;
        private final long setTime;
        private final long duration;

        private String cachedLabel;
        private long lastUpdate = 0;

        NpcBeacon(BlockPos pos, int color, String name, long duration) {
            this.pos = pos;
            this.color = color;
            this.name = name;
            this.duration = duration;
            this.setTime = System.currentTimeMillis();
            this.cachedLabel = name;
        }

        public String getName() { return name; }
        public BlockPos getPos() { return pos; }
        public int getColor() { return color; }
        public long getSetTime() { return setTime; }

        public String getLabel() {
            long now = System.currentTimeMillis();
            if (now - lastUpdate >= REFRESH_MS) {
                var mc = Minecraft.getInstance();
                if (mc.player != null) {
                    double distance = mc.player.position().distanceTo(Vec3.atCenterOf(pos));
                    cachedLabel = "§f" + name + " §8(§7" + String.format("%.1f", distance) + "m§8)";
                }
                lastUpdate = now;
            }
            return cachedLabel;
        }

        @Override
        public boolean renderBeam() {
            return false;
        }
    }
}
