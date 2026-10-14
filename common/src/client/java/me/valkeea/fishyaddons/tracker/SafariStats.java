package me.valkeea.fishyaddons.tracker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import me.valkeea.fishyaddons.api.skyblock.SafariData.Critter;
import me.valkeea.fishyaddons.api.skyblock.SafariData.SafariZone;
import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas;
import me.valkeea.fishyaddons.event.impl.EnvironmentChangeEvent;
import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.vconfig.annotation.VCInit;
import me.valkeea.fishyaddons.vconfig.annotation.VCListener;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.api.IntKey;
import me.valkeea.fishyaddons.vconfig.ui.layout.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

@VCModule
public class SafariStats {
    private static List<Component> styledData = new ArrayList<>();    
   
    private static int mode = 0;
    private static int ticks = 0;    
    private static int uniqueCaptured = 0;

    private static long lastInstanceTime = -1; 
    private static SafariZone playerZone = null;
    private static boolean enabled = false;     

    private static final String CRITTER_NAMES = Arrays.stream(Critter.values()).map(c -> Pattern.quote(c.toString())).collect(Collectors.joining("|"));
    private static final Pattern CRITTER_PATTERN = Pattern.compile(
        "\\b(?:(?<amount>\\d+)x\\b|an?\\b) (?<critter>" + CRITTER_NAMES + ")\\b shard",
        Pattern.CASE_INSENSITIVE
    );
    
    @VCInit
    public static void onInstanceChange() {
        FaEvents.ENVIRONMENT_CHANGE.register(SafariStats::clearData);
    }

    @VCListener(BooleanKey.TRACK_CRITTERS)
    public static void onCritterToggle(boolean enabled) {
        SafariStats.enabled = enabled;
    }

    @VCListener(BooleanKey.SAFARI_OWN_ZONE_ONLY)
    public static void onSafariOwnZoneOnlyToggle() {
        refreshData();
    }    

    @VCListener(ints = IntKey.CRITTER_MODE)
    public static void onCritterModeChange(int newMode) {
        SafariStats.mode = newMode;
        refreshData();
    }

    public static void tick() {
        if (!enabled || lastInstanceTime == -1) return;

        ticks++;
        if (ticks % 40 == 0) {
            updateZone();
            ticks = 0;
        }
    }

    private static void clearData(EnvironmentChangeEvent e) {
        if (!enabled) return;

        if (e.newIsland == SkyblockAreas.Island.SAFARI) {
            lastInstanceTime = System.nanoTime();
        } else if (uniqueCaptured > 0) {
            resetSafariData();
        }
    }

    public static void refreshData() {
        styledData.clear();
    }

    private static void resetSafariData() {
        for (var c : Critter.values()) c.resetStats();
        uniqueCaptured = 0;
        styledData.clear();
    }    

    public static boolean handleChat(String clean) {
        if (!enabled || clean.isEmpty() || !(clean.startsWith("capture!") || clean.startsWith("loot share!"))) return false;
        
        var m = CRITTER_PATTERN.matcher(clean);
        if (!m.find()) return false;

        var critter = getCritter(m.group("critter"));
        if (critter == null) return false;

        var amtGroup = m.group("amount");
        int shardAmount = amtGroup != null ? Integer.parseInt(amtGroup) : 1;

        recordCapture(critter, shardAmount);
        return true;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    // Retrieves a Critter enum instance from a raw string, or null if it doesn't exist.
    private static @Nullable Critter getCritter(String raw) {
        try {
            return Critter.valueOf(raw.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException _) {
            return null;
        }
    }

    /**
     * Constructs and returns the styled list of critters with their counts based on the current mode.
     */
    public static List<Component> getData() {
        if (styledData != null && !styledData.isEmpty()) {
            return styledData;
        }

        var data = switch (mode) {
            case 1 -> collectRawCounts();
            case 2 -> collectShardCounts();
            default -> collectCompletions();
        };

        data.add(Component.literal("§7(" + uniqueCaptured + "/" + Critter.values().length + ") unique critters"));
        styledData = new ArrayList<>(data);

        return data;
    }

    private static final String DIVIDER = "§r:§7 ";

    private static List<Component> collectCompletions() {
        List<Component> comp = new ArrayList<>();
        SafariZone lastZone = null;

        for (var c : Critter.values()) {

            var zone = c.getZone();
            if (!isActiveZone(zone)) continue;
            if (lastZone != zone) {
                lastZone = zone;
                comp.add(Component.literal(lastZone.name())
                .withColor(Colors.BRIGHT_GRAY));
            }
            
            comp.add(c.getDisplayName().copy().append(Component.literal(DIVIDER + (c.getCaptureCount() > 0 ? "§a✓" : "§c✗"))));
        }

        return comp;
    }

    private static List<Component> collectRawCounts() {
        List<Component> rawCounts = new ArrayList<>();
        SafariZone seenZone = null;

        for (var c : Critter.values()) {

            var count = c.getCaptureCount();
            if (count > 0) {
                
                var zone = c.getZone();
                if (!isActiveZone(zone)) continue;
                if (seenZone != zone) {
                    seenZone = zone;
                    rawCounts.add(Component.literal(seenZone.name())
                    .withColor(Colors.BRIGHT_GRAY));
                }

                rawCounts.add(c.getDisplayName().copy().append(Component.literal(DIVIDER + count)));
            }
        }

        return rawCounts;
    }  

    private static List<Component> collectShardCounts() {
        List<Component> shardAmounts = new ArrayList<>();
        SafariZone seenZone = null;

        for (var c : Critter.values()) {

            var count = c.getShardCount();
            if (count > 0) {

                var zone = c.getZone();
                if (!isActiveZone(zone)) continue;
                if (seenZone != zone) {
                    seenZone = zone;
                    shardAmounts.add(Component.literal(seenZone.name())
                    .withColor(Colors.BRIGHT_GRAY));
                }
                
                shardAmounts.add(c.getDisplayName().copy().append(Component.literal(DIVIDER + count)));
            }
        }

        return shardAmounts;
    }

    /**
     * Retrieves the full stats for all critters including capture counts and shard amounts, organized by zone.
     */
    public static List<Component> getFullStats() {
        List<Component> fullStats = new ArrayList<>();
        SafariZone seenZone = null;

        for (var c : Critter.values()) {

            var zone = c.getZone();
            if (seenZone != zone) {
                seenZone = zone;
                fullStats.add(Component.literal(seenZone.name())
                .withColor(Colors.BRIGHT_GRAY));
            }

            fullStats.add(c.getDisplayName().copy().append(Component.literal(
                DIVIDER + c.getCaptureCount() + " §8(§7" + c.getShardCount() + " shards §8)"))
            );
        }

        return fullStats;
    }

    private static void recordCapture(Critter c, int shardAmount) {
        if (c.getCaptureCount() == 0) uniqueCaptured++;

        c.incrementCaptureCount();
        c.addShardCount(shardAmount);

        styledData.clear();
        updateZone();
    }

    public static Component timerComponent() {

        long elapsed = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - lastInstanceTime);
        long min = elapsed / 60;
        long s = elapsed % 60;

        return Component.literal(String.format("%02d:%02d", min, s))
            .withColor(Colors.BRIGHT_GRAY);
    }

    // --- SafariZone Calculation --- 

    private static final double CENTER_X = -49.5;
    private static final double CENTER_Z = 0.5;

    // Borders sorted by angle (north-up map), paired with the
    // zone immediately clockwise of that border (derived from approx boundary coords)
    private static final double[] ZONE_BORDER_ANGLES;
    private static final SafariZone[] ZONE_AFTER_BORDER;

    static {
        record Border(double x, double z, SafariZone zoneClockwise) {}
        var borders = new Border[] {
            new Border(-55.7, 95.29, SafariZone.CAVERN),  // forest | cavern
            new Border(-137.0, 3.65, SafariZone.ICY),     // cavern | icy
            new Border(-43.0, -90.0, SafariZone.HAUNTED), // icy | haunted
            new Border(-33.6, -1.6, SafariZone.FOREST),   // haunted | forest
        };

        Arrays.sort(borders, (a, b) -> Double.compare(zoneAngle(a.x(), a.z()), zoneAngle(b.x(), b.z())));

        ZONE_BORDER_ANGLES = new double[borders.length];
        ZONE_AFTER_BORDER = new SafariZone[borders.length];

        for (int i = 0; i < borders.length; i++) {
            ZONE_BORDER_ANGLES[i] = zoneAngle(borders[i].x(), borders[i].z());
            ZONE_AFTER_BORDER[i] = borders[i].zoneClockwise();
        }
    }

    private static double zoneAngle(double x, double z) {
        return Math.atan2(z - CENTER_Z, x - CENTER_X);
    }

    /**
     * Determine zone based on the player's angle relative to the center.
     */
    private static void updateZone() {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;

        double angle = zoneAngle(mc.player.getX(), mc.player.getZ());

        var last = playerZone;
        playerZone = ZONE_AFTER_BORDER[ZONE_AFTER_BORDER.length - 1];
        for (int i = 0; i < ZONE_BORDER_ANGLES.length; i++) {
            if (angle < ZONE_BORDER_ANGLES[i]) break;
            playerZone = ZONE_AFTER_BORDER[i];
        }

        if (last != playerZone) refreshData();
    }

    private static boolean isActiveZone(SafariZone zone) {
        if (!Config.get(BooleanKey.SAFARI_OWN_ZONE_ONLY)) return true;
        else return zone == getCurrentZone();
    }

    private static SafariZone getCurrentZone() {
        if (playerZone == null) updateZone();
        return playerZone;
    }

    private SafariStats() {}
}
