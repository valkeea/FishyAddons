package me.valkeea.fishyaddons.api.skyblock;

import java.util.Arrays;

import org.jetbrains.annotations.Nullable;

import me.valkeea.fishyaddons.api.skyblock.SafariData.SafariZone;
import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas.Island;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class IslandRegion {
    private IslandRegion() {}

    private static final double POS_CHECK_THRESHOLD = 5.0;

    // --- Safari zone geometry ---

    private static final double SAFARI_CENTER_X = -49.5;
    private static final double SAFARI_CENTER_Z = 0.5;

    // Borders sorted by angle (north-up map), paired with the
    // zone immediately clockwise of that border (derived from approx boundary coords)
    private static final double[] SAFARI_BORDER_ANGLES;
    private static final SafariZone[] SAFARI_ZONE_AFTER_BORDER;

    static {
        record Border(double x, double z, SafariZone zoneClockwise) {}
        var borders = new Border[] {
            new Border(-55.7, 95.29, SafariZone.CAVERN),  // forest | cavern
            new Border(-137.0, 3.65, SafariZone.ICY),     // cavern | icy
            new Border(-43.0, -90.0, SafariZone.HAUNTED), // icy | haunted
            new Border(-33.6, -1.6, SafariZone.FOREST),   // haunted | forest
        };

        Arrays.sort(borders, (a, b) -> Double.compare(safariZoneAngle(a.x(), a.z()), safariZoneAngle(b.x(), b.z())));

        SAFARI_BORDER_ANGLES = new double[borders.length];
        SAFARI_ZONE_AFTER_BORDER = new SafariZone[borders.length];

        for (int i = 0; i < borders.length; i++) {
            SAFARI_BORDER_ANGLES[i] = safariZoneAngle(borders[i].x(), borders[i].z());
            SAFARI_ZONE_AFTER_BORDER[i] = borders[i].zoneClockwise();
        }
    }

    private static double safariZoneAngle(double x, double z) {
        return Math.atan2(z - SAFARI_CENTER_Z, x - SAFARI_CENTER_X);
    }

    /**
     * Safari zone for arbitrary coordinates
     */
    public static SafariZone safariZoneAt(double x, double z) {
        double angle = safariZoneAngle(x, z);

        var zone = SAFARI_ZONE_AFTER_BORDER[SAFARI_ZONE_AFTER_BORDER.length - 1];
        for (int i = 0; i < SAFARI_BORDER_ANGLES.length; i++) {
            if (angle < SAFARI_BORDER_ANGLES[i]) break;
            zone = SAFARI_ZONE_AFTER_BORDER[i];
        }
        return zone;
    }

    // --- Plhlegblast pool geometry ---

    /**
     * Whether the given position is within the Plhlegblast pool bounds
     */
    public static boolean isCrimsonPool(Vec3 pos) {
        var inPoolX = pos.x >= -400 && pos.x <= -355;
        var inPoolY = pos.y >= 66 && pos.y <= 84;
        var inPoolZ = pos.z >= -725 && pos.z <= -680;
        return inPoolX && inPoolY && inPoolZ;
    }

    // --- Player-position zone tracking, routed by island ---

    private static Island lastZoneIsland = null;
    private static Vec3 lastCheckedPos = null;
    private static SafariZone playerSafariZone = null;
    private static boolean playerInCrimsonPool = false;

    /**
     * Recalculate the player's current zone every tick if position has changed, routed by island
     */
    public static void tick() {
        var island = SkyblockAreas.getIsland();
        if (!island.hasRegions()) {
            lastZoneIsland = null;
            lastCheckedPos = null;
            return;
        }

        var player = Minecraft.getInstance().player;
        if (player == null) return;

        var pos = player.position();
        boolean islandChanged = island != lastZoneIsland;
        if (!islandChanged && lastCheckedPos != null && pos.distanceTo(lastCheckedPos) < POS_CHECK_THRESHOLD) {
            return;
        }

        lastZoneIsland = island;
        lastCheckedPos = pos;

        if (island == Island.SAFARI) {
            playerSafariZone = safariZoneAt(pos.x, pos.z);
        } else {
            playerInCrimsonPool = isCrimsonPool(pos);
        }
    }

    /**
     * The player's current Safari zone, or null if not on Safari / not yet determined.
     */
    public static @Nullable SafariZone getPlayerSafariZone() {
        if (playerSafariZone == null && SkyblockAreas.isSafari()) tick();
        return playerSafariZone;
    }

    /**
     * Whether the player is currently within Plhlegblast pool.
     */
    public static boolean isPlayerInCrimsonPool() {
        return playerInCrimsonPool;
    }
}
