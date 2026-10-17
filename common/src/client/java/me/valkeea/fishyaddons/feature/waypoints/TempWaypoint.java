package me.valkeea.fishyaddons.feature.waypoints;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jetbrains.annotations.Nullable;

import me.valkeea.fishyaddons.render.Beacon;
import me.valkeea.fishyaddons.vconfig.annotation.VCInit;
import me.valkeea.fishyaddons.vconfig.annotation.VCListener;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.api.IntKey;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

@VCModule
public class TempWaypoint {
    private TempWaypoint() {}
    private static final List<BeaconData> beacons = new CopyOnWriteArrayList<>();
    private static final double HIDE_DIST_SQ = Math.pow(8.0, 2);
    private static final long GRACE_PERIOD_MS = 3000; 

    private static volatile BeaconData lastBeacon = null;
    private static boolean hideClose = false;
    private static long duration = IntKey.RENDER_COORD_MS.getDefault();

    @VCListener(value = BooleanKey.RENDER_COORD_HIDE_CLOSE, ints = IntKey.RENDER_COORD_MS)
    public static void refresh() {
        duration = Config.get(IntKey.RENDER_COORD_MS) * 1000L;
        hideClose = Config.get(BooleanKey.RENDER_COORD_HIDE_CLOSE);
    }

    @VCInit
    public static void init() {
        refresh();
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            if (beacons.isEmpty()) return;

            long now = System.currentTimeMillis();
            beacons.removeIf(beacon -> now - beacon.setTime > beacon.duration);

            if (hideClose) {
                renderFar(context);
            } else {
                renderAll(context);
            }
        });
    }
    
    private static void renderFar(LevelRenderContext context) {
        var client = Minecraft.getInstance();
        if (client.player == null) return;

        Vec3 playerPos = client.player.position();
        long now = System.currentTimeMillis();

        for (BeaconData beacon : beacons) {
            double distanceSq = playerPos.distanceToSqr(Vec3.atCenterOf(beacon.getPos()));

            if (distanceSq <= HIDE_DIST_SQ && now - beacon.getSetTime() > GRACE_PERIOD_MS) {
                beacons.remove(beacon);
            }

            Beacon.renderBeacon(context, beacon);
        }
    }
    
    private static void renderAll(LevelRenderContext context) {
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
     * Adds a beacon at the specified position with color and optional label (dedup by position).
     * The position is converted to a centered BlockPos and empty titles are handled at render.
     */
    public static void setBeacon(BlockPos pos, int colorARGB, String displayLabel) {
        addBeacon(new BeaconData(getActualPos(new Vec3(pos)), colorARGB, displayLabel));
    }
    
    public static void setBeacon(BlockPos pos, int colorARGB, String displayLabel, long customDuration) {
        var beacon = new BeaconData(getActualPos(new Vec3(pos)), colorARGB, displayLabel);
        beacon.setDuration(customDuration);
        addBeacon(beacon);
    }

    private static void addBeacon(BeaconData beacon) {
        beacons.removeIf(existing -> existing.pos.equals(beacon.pos));
        beacons.add(beacon);
        lastBeacon = beacon;
    }

    /**
     * Convert Vec3d to centered BlockPos with proper flooring for negative coordinates.
     */
    public static BlockPos getActualPos(Vec3 pos) {
        return new BlockPos(
            (int) (pos.x < 0 ? Math.floor(pos.x) - 1 : Math.floor(pos.x)),
            (int) (Math.floor(pos.y) - 1),
            (int) (pos.z < 0 ? Math.floor(pos.z) - 1 : Math.floor(pos.z))
        );
    }

    public static void redrawLast() {
        BeaconData previous = lastBeacon;
        if (previous != null) {
            addBeacon(new BeaconData(previous.pos, previous.color, previous.label));
        }
    }

    public static void redraw(BlockPos pos, String title) {
        int color = Config.get(IntKey.RENDER_COORD_COLOR);
        String label = (title != null && !title.isEmpty()) ? title : "";
        setBeacon(pos, color, label);
    }

    public static void removeBeaconAt(BlockPos pos) {
        beacons.removeIf(beacon -> beacon.getPos().equals(getActualPos(new Vec3(pos))));
    }

    public static class BeaconData implements me.valkeea.fishyaddons.render.IBeaconData {
        final BlockPos pos;
        final int color;
        final String label;
        final long setTime;
        long duration = TempWaypoint.duration;

        BeaconData(BlockPos pos, int color, @Nullable String label) {
            this.pos = pos;
            this.color = color;
            this.label = label;
            this.setTime = System.currentTimeMillis();
        }

        private String cachedLabel = "";
        private long lastLabelUpdate = 0;
        private static final long LABEL_REFRESH_MS = 200;

        public BlockPos getPos() { return pos; }
        public long getSetTime() { return setTime; }
        public int getColor() { return color; }        

        public String getLabel() {
            long now = System.currentTimeMillis();
            if (now - lastLabelUpdate >= LABEL_REFRESH_MS) {
                cachedLabel = formatLabel(label);
                lastLabelUpdate = now;
            }
            return cachedLabel;
        }

        public void setDuration(long duration) {
            this.duration = duration;
        }

        @Override
        public boolean fillBlock() {
            return false;
        }

        private String formatLabel(String label) {
            
            var mc = Minecraft.getInstance();
            if (mc.player == null) return label != null ? label : "";

            var distance = mc.player.position().distanceTo(new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
            var distanceStr = String.format("%.1f", distance);

            return label != null && !label.isEmpty() ? label + " §8(§7" + distanceStr + "m§8)" : distanceStr + "m";
        }
    }
}
