package me.valkeea.fishyaddons.feature.waypoints;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas;
import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas.Island;
import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.render.Beacon;
import me.valkeea.fishyaddons.tool.FishyMode;
import me.valkeea.fishyaddons.util.FishyNotis;
import me.valkeea.fishyaddons.util.text.Color;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Renders a collection of ordered waypoints depending on the detected area.
 * Waypoints get colored based on whether a variable action was performed at that location.
 */
public class WaypointChains {
    private WaypointChains() {}

    // Keyed by "area_chainName"
    private static final Map<String, Long> CHAIN_START_TIMES = new HashMap<>();
    private static final Map<String, List<Long>> RUN_TIMES = new HashMap<>();

    private static boolean enabled = false;
    private static boolean announce = true;
    private static boolean presetsLoaded = false;
    private static int compDistance = 3;
    private static List<String> enabledPresets = new ArrayList<>();
    
    private static final Map<String, List<WaypointChain>> CACHED_PRESET_CHAINS = new HashMap<>();
    private static final Map<String, List<WaypointChain>> CACHED_USER_CHAINS = new HashMap<>();
    private static String lastArea = null;

    static List<String> presets = List.of(
        Key.WAYPOINT_CHAINS_SHOW_RELICS
    );
    
    public static void init() {
        refresh();
        
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            if (!enabled) return;
            
            Island currentArea = SkyblockAreas.getIsland();
            if (currentArea == null) return;
            
            List<WaypointChain> presetChains = getCachedPresetChains(currentArea.key());
            for (WaypointChain presetChain : presetChains) {
                renderChain(context, presetChain, true);
            }
            
            List<WaypointChain> userChains = getCachedUserChains(currentArea.key());
            for (WaypointChain userChain : userChains) {
                renderChain(context, userChain, false);
            }
        });
    }

    public static void onConnect() {
        clearAllCaches();
        
        if (!presetsLoaded && MinecraftClient.getInstance().getResourceManager() != null) {
            try {
                ChainConfig.loadPresetChains();
                presetsLoaded = true;
            } catch (Exception e) {
                System.err.println("Failed to load waypoint chain presets: " + e.getMessage());
            }
        }
    }
    
    public static void refresh() {
        ChainConfig.init();
        enabled = FishyConfig.getState(Key.WAYPOINT_CHAINS_ENABLED, false);
        compDistance = FishyConfig.getInt(Key.WAYPOINT_CHAINS_COMPLETION_DISTANCE, 3);
        announce = FishyConfig.getState(Key.WAYPOINT_CHAINS_INFO, true);

        List<String> newEnabledPresets = new ArrayList<>();
        for (String presetKey : presets) {
            boolean isEnabled = FishyConfig.getState(presetKey, false);
            if (isEnabled) {
                newEnabledPresets.add(presetKey);
                if (!enabledPresets.contains(presetKey)) {
                    ChainConfig.loadPresetChains();
                }
            }
        }
        
        if (!enabledPresets.equals(newEnabledPresets)) {
            enabledPresets = newEnabledPresets;
            clearPresetCache();
        }
    }
    
    private static void clearPresetCache() {
        CACHED_PRESET_CHAINS.clear();
        CACHED_USER_CHAINS.clear();
        lastArea = null;
    }
    
    public static void clearUserChainCache() {
        CACHED_USER_CHAINS.clear();
    }
    
    public static void clearAllCaches() {
        CACHED_PRESET_CHAINS.clear();
        CACHED_USER_CHAINS.clear();
        lastArea = null;
    }
    
    private static List<WaypointChain> getCachedPresetChains(String area) {
        if (area.equals(lastArea) && CACHED_PRESET_CHAINS.containsKey(area)) {
            return CACHED_PRESET_CHAINS.get(area);
        }
        
        List<WaypointChain> chains = new ArrayList<>();
        for (String presetKey : enabledPresets) {
            WaypointChain presetChain = ChainConfig.getPresetChain(presetKey, area);
            if (presetChain != null) {
                chains.add(presetChain);
            }
        }
        
        CACHED_PRESET_CHAINS.put(area, chains);
        lastArea = area;
        return chains;
    }
    
    private static List<WaypointChain> getCachedUserChains(String area) {
        if (CACHED_USER_CHAINS.containsKey(area)) {
            return CACHED_USER_CHAINS.get(area);
        }
        
        List<WaypointChain> chains = new ArrayList<>();
        for (WaypointChain userChain : ChainConfig.getUserChains()) {
            if (userChain != null && userChain.area.equals(area) && ChainConfig.isChainVisible(userChain.name(), userChain.area)) {
                chains.add(userChain);
            }
        }
        
        CACHED_USER_CHAINS.put(area, chains);
        return chains;
    }
    
    // --- Core rendering logic ---

    private static void renderChain(WorldRenderContext context, WaypointChain chain, boolean isPreset) {
        var client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        Vec3d playerPos = client.player.getPos();
        
        if (!isPreset && chain.type == ChainType.USER_DEFINED) {
            checkForCompletion(chain);
            startChainTimingIfNeeded(chain);
        }
        
        int nextWaypointIndex = findNextWaypointIndex(chain, isPreset);
        
        for (int i = 0; i < chain.waypoints.size(); i++) {
            renderWaypoint(context, chain, i, playerPos, isPreset, nextWaypointIndex);
        }
    }
    
    private static void startChainTimingIfNeeded(WaypointChain chain) {
        String chainKey = chain.area + "_" + chain.name();
        if (CHAIN_START_TIMES.containsKey(chainKey)) return;
        
        boolean hasUnvisitedWaypoints = chain.waypoints.stream().anyMatch(w ->
            !w.visited());

        if (hasUnvisitedWaypoints) {
            CHAIN_START_TIMES.put(chainKey, System.currentTimeMillis());
        }
    }
    
    private static int findNextWaypointIndex(WaypointChain chain, boolean isPreset) {
        if (isPreset) return -1;
        
        for (int i = 0; i < chain.waypoints.size(); i++) {
            var waypoint = chain.waypoints.get(i);
            boolean wasVisited = waypoint.visited();
            if (!wasVisited) {
                return i;
            }
        }
        return -1;
    }
    
    private static void renderWaypoint(WorldRenderContext context, WaypointChain chain, int index, 
                                     Vec3d playerPos, boolean isPreset, int nextWaypointIndex) {
        var waypoint = chain.waypoints.get(index);
        var pos = waypoint.position;

        if (isPreset && ChainConfig.wasVisited(pos, chain.name())) return;

        boolean wasVisited = getAndUpdateCompletion(waypoint, pos, playerPos);
        int color = determineColor(chain, index, wasVisited, isPreset, nextWaypointIndex);
        double distance = calcDistance(playerPos, pos);
        String label = formatLabel(waypoint.label(), distance, isPreset, index, waypoint, nextWaypointIndex);

        var beaconData = new ChainBeaconData(pos, color, label, wasVisited);
        Beacon.renderBeacon(context, beaconData);
    }

    private static boolean getAndUpdateCompletion(Waypoint waypoint, BlockPos pos, Vec3d playerPos) {
        boolean wasVisited = waypoint.visited();

        if (!wasVisited) {
            double delta = calcDistance(playerPos, pos);
            if (delta <= compDistance) {
                waypoint.setVisited(true);
                wasVisited = true;
            }
        }
        
        return wasVisited;
    }
    
    private static int determineColor(WaypointChain chain, int index, boolean wasVisited, 
                                            boolean isPreset, int nextWaypointIndex) {
        int baseColor = isPreset ? FishyMode.getThemeColor() : ChainConfig.getChainColor(chain.name(), chain.area);

        if (wasVisited) return Color.desaturateAndDarken(baseColor, 0.6f);

        if (isPreset || index == nextWaypointIndex + 1) return baseColor;
        
        if (index == nextWaypointIndex) {
            return Color.brighten(baseColor, 0.3f);
        }

        return Color.dim(baseColor, 0.8f);
    }

    private static double calcDistance(Vec3d playerPos, BlockPos pos) {
        return playerPos.distanceTo(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
    }

    private static String formatLabel(String baseLabel, double distance, boolean isPreset, 
                                            int index, Waypoint waypoint, int nextWaypointIndex) {
        String label = baseLabel;
        String distanceFormat;

        if (distance >= 250 || waypoint.visited()) {
            distanceFormat = "§8";
        } else if (distance <= 100) {
            distanceFormat = distance <= 30 ? "§a" : "§f";
        } else {
            distanceFormat = "§7";
        }
        
        if (!isPreset && index == nextWaypointIndex) label = "§l" + label;

        return label + " §8(" + distanceFormat + String.format("%.0f", distance) + "m§8)";
    }

    private static void checkForCompletion(WaypointChain chain) {

        boolean allCompleted = true;
        for (Waypoint waypoint : chain.waypoints) {
            if (!waypoint.visited()) {
                allCompleted = false;
                break;
            }
        }

        if (allCompleted && chain.waypoints.size() > 1) {

            String chainKey = chain.area + "_" + chain.name();
            var chainName = Text.literal(
                chain.name()).styled(style -> style.withColor(ChainConfig.getChainColor(chain.name(), chain.area)));

            if (CHAIN_START_TIMES.containsKey(chainKey)) {

                long compTime = System.currentTimeMillis() - CHAIN_START_TIMES.get(chainKey);
                String timeStr = formatTime(compTime);

                RUN_TIMES.computeIfAbsent(chainKey, k -> new ArrayList<>()).add(compTime);
                CHAIN_START_TIMES.remove(chainKey);
                resetProgress(chain);

                if (announce) {
                    FishyNotis.alert(Text.literal("§a✓ §8Chain ")
                    .append(chainName).append(Text.literal(" §8completed in §b" + timeStr + "§8!")));
                }
                
            } else {
                resetProgress(chain);
                if (announce) {
                    FishyNotis.alert(Text.literal("§a✓ §8Chain ")
                    .append(chainName).append(Text.literal(" §8completed!")));
                }
            }
        }
    }

    // --- Completion management ---

    private static void resetProgress(WaypointChain chain) {

        for (Waypoint waypoint : chain.waypoints) {
            if (chain.type == ChainType.USER_DEFINED) {
                waypoint.setVisited(false);

            } else ChainConfig.resetStatusAt(waypoint.position, chain.name());
        }
        ChainConfig.saveUserProgress();
    }  
    
    public static void onRelicFound() {

        var client = MinecraftClient.getInstance();
        if (client.player != null) {

            var currentArea = SkyblockAreas.getIsland();
            var chain = ChainConfig.getPresetChain(Key.WAYPOINT_CHAINS_SHOW_RELICS, currentArea.key());

            if (chain != null) {
                Vec3d playerPos = client.player.getPos();
                BlockPos nearest = findNearest(chain, playerPos);

                if (nearest != null) {
                    ChainConfig.markVisitedAt(nearest, chain.name());
                }
            }
        }
    }
    
    private static BlockPos findNearest(WaypointChain chain, Vec3d playerPos) {

        BlockPos nearest = null;
        double minDistance = Double.MAX_VALUE;
        boolean isPreset = chain.type != ChainType.USER_DEFINED;

        for (Waypoint wp : chain.waypoints) {

            boolean wasVisited = isPreset 
                ? ChainConfig.wasVisited(wp.position, chain.name()) 
                : wp.visited();

            if (wasVisited) continue;
            
            double distance = playerPos.distanceTo(new Vec3d(
                wp.position.getX() + 0.5,
                wp.position.getY() + 0.5,
                wp.position.getZ() + 0.5
            ));
            
            if (distance < minDistance && distance <= 8.0) {
                minDistance = distance;
                nearest = wp.position;
            }
        }
        
        return nearest;
    }

    public static String getAvgRunTime(String chainName, String area) {

        String chainKey = area + "_" + chainName;
        List<Long> times = RUN_TIMES.get(chainKey);
        if (times == null || times.isEmpty()) {
            return "N/A";
        }
        
        long total = 0;
        for (Long time : times) {
            total += time;
        }
        long average = total / times.size();
        return formatTime(average);
    }

    public static boolean isChainActive(String chainName, String area) {
        String chainKey = area + "_" + chainName;
        return CHAIN_START_TIMES.containsKey(chainKey);
    }

    public static long getChainStartTime(String chainName, String area) {
        String chainKey = area + "_" + chainName;
        return CHAIN_START_TIMES.getOrDefault(chainKey, -1L);
    }

    private static String formatTime(long timeMs) {
        long seconds = timeMs / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        
        if (minutes > 0) {
            return String.format(Locale.US, "%dm %02ds", minutes, seconds);
        } else {
            return String.format(Locale.US, "%.1fs", timeMs / 1000.0);
        }
    }      

    protected static class ChainBeaconData implements me.valkeea.fishyaddons.render.IBeaconData {
        private final BlockPos pos;
        private final int color;
        private final String label;
        private final boolean visited;
        private final long setTime;

        protected ChainBeaconData(BlockPos pos, int color, String label, boolean visited) {
            this.pos = pos;
            this.color = color;
            this.label = label;
            this.visited = visited;
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
        
        public boolean wasVisited() {
            return visited;
        }

        @Override
        public boolean noDepth() {
            return false;
        }

        @Override
        public boolean fillBlock() {
            return false;
        }
    }    
}
