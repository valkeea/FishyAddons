package me.valkeea.fishyaddons.feature.waypoints;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import me.valkeea.fishyaddons.tool.FishyMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

@SuppressWarnings("squid:S1104")
public class ChainConfig {
    private ChainConfig() {}
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File WAYPOINTS_FILE = new File(MinecraftClient.getInstance().runDirectory, "config/fishyaddons/waypoints.json");
    private static final File BACKUP_DIR = new File(MinecraftClient.getInstance().runDirectory, "config/fishyaddons/backup");
    private static final File BACKUP_FILE = new File(BACKUP_DIR, "waypoints.json");
    // Keyed by preset name
    private static final Map<String, WaypointChain> PRESET_CHAINS = new HashMap<>();
    // Keyed by "area_chainName"
    private static final Map<String, Set<BlockPos>> PRESET_VISITED_WAYPOINTS = new HashMap<>();
    private static final Map<String, Integer> CHAIN_COLORS = new HashMap<>();
    private static final Map<String, Boolean> CHAIN_VISIBILITY = new HashMap<>();
    static final List<WaypointChain> USER_CHAINS = new ArrayList<>();

    private static boolean initialized = false;
    private static boolean recreatedConfig = false;
    private static boolean restoredConfig = false;
    
    public static void init() {
        if (initialized) return;

        initialized = true;
        BACKUP_DIR.mkdirs();
        load();
    }
    
    public static boolean isRecreated() { return recreatedConfig; }
    public static boolean isRestored() { return restoredConfig; }
    
    public static void resetFlags() {
        recreatedConfig = false;
        restoredConfig = false;
    }
    
    public static void loadPresetChains() {
        loadAllPresetLocations();
    } 

    protected static WaypointChain getPresetChain(String presetKey, String area) {
        var target = PRESET_CHAINS.get(presetKey);

        if (target != null && target.area.equals(area)) {
            return target;
        }

        return null;
    }

    // --- Preset Completion Management ---

    protected static boolean wasVisited(BlockPos pos, String name) {
        Set<BlockPos> visited = PRESET_VISITED_WAYPOINTS.get(name);
        return visited != null && visited.contains(pos);
    }

    protected static void resetStatusAt(BlockPos pos, String name) {
        Set<BlockPos> visited = PRESET_VISITED_WAYPOINTS.get(name);
        if (visited != null) {
            visited.remove(pos);
            save();
        }
    }

    protected static void markVisitedAt(BlockPos pos, String name) {
        PRESET_VISITED_WAYPOINTS.computeIfAbsent(name, k -> new HashSet<>()).add(pos);
        save();
    }

    public static void clearPresetFor(String name) {
        var target = PRESET_CHAINS.get(name);
        if (target != null) {
            String key = target.area + "_" + name;
            if (PRESET_VISITED_WAYPOINTS.containsKey(key)) {
                PRESET_VISITED_WAYPOINTS.remove(key);
                save();
            }
        }
    }

    // --- User Chain Management ---
    protected static Map<String, Integer> getChainColors() {
        return new HashMap<>(CHAIN_COLORS);
    }

    protected static Map<String, Boolean> getChainVisibility() {
        return new HashMap<>(CHAIN_VISIBILITY);
    }

    public static List<WaypointChain> getUserChains() {
        return new ArrayList<>(USER_CHAINS);
    }

    protected static void saveUserProgress() {
        save();
    }

    public static void setChainColor(String chainName, String area, int color) {
        String chainKey = area + "_" + chainName;
        CHAIN_COLORS.put(chainKey, color);
        save();
    }

    public static int getChainColor(String chainName, String area) {
        String chainKey = area + "_" + chainName;
        return CHAIN_COLORS.getOrDefault(chainKey, FishyMode.getThemeColor());
    }

    protected static boolean hasCustomColor(String chainName, String area) {
        String chainKey = area + "_" + chainName;
        return CHAIN_COLORS.containsKey(chainKey);
    }

    protected static void setChainVisible(String chainName, String area, boolean visible) {
        String chainKey = area + "_" + chainName;
        CHAIN_VISIBILITY.put(chainKey, visible);
        save();
    }

    public static boolean isChainVisible(String chainName, String area) {
        String chainKey = area + "_" + chainName;
        return CHAIN_VISIBILITY.getOrDefault(chainKey, true);
    }

    public static void toggleChainVisibility(String chainName, String area) {
        boolean currentVisibility = isChainVisible(chainName, area);
        setChainVisible(chainName, area, !currentVisibility);
    }

    public static void renameUserChain(WaypointChain chain, String newName) {

        String oldName = chain.name();
        String oldChainKey = chain.area + "_" + oldName;
        String newChainKey = chain.area + "_" + newName;

        chain.setName(newName);

        for (Waypoint waypoint : chain.waypoints) {
            waypoint.setLabel(waypoint.label().replace(oldName, newName));
        }

        Integer color = CHAIN_COLORS.remove(oldChainKey);
        if (color != null) {
            CHAIN_COLORS.put(newChainKey, color);
        }

        Boolean visible = CHAIN_VISIBILITY.remove(oldChainKey);
        if (visible != null) {
            CHAIN_VISIBILITY.put(newChainKey, visible);
        }

        save();
    }

    // --- Config I/O ---
    
    protected static void save() {
        try {
            WAYPOINTS_FILE.getParentFile().mkdirs();

            try (FileWriter writer = new FileWriter(WAYPOINTS_FILE)) {
                WaypointData data = new WaypointData();
                
                data.userChains = new ArrayList<>();
                for (WaypointChain chain : USER_CHAINS) {
                    UserChainData chainData = new UserChainData();
                    chainData.area = chain.area;
                    chainData.name = chain.name();
                    chainData.waypoints = new ArrayList<>();
                    for (Waypoint waypoint : chain.waypoints) {
                        UserWaypointData waypointData = new UserWaypointData();
                        waypointData.x = waypoint.position.getX();
                        waypointData.y = waypoint.position.getY();
                        waypointData.z = waypoint.position.getZ();
                        waypointData.label = waypoint.label();
                        waypointData.visited = waypoint.visited();
                        chainData.waypoints.add(waypointData);
                    }
                    data.userChains.add(chainData);
                }
                
                data.presetCompletions = new HashMap<>();
                for (Map.Entry<String, Set<BlockPos>> entry : PRESET_VISITED_WAYPOINTS.entrySet()) {
                    List<WaypointPosition> positions = new ArrayList<>();
                    for (BlockPos pos : entry.getValue()) {
                        positions.add(new WaypointPosition(pos.getX(), pos.getY(), pos.getZ()));
                    }
                    data.presetCompletions.put(entry.getKey(), positions);
                }
                
                data.chainColors = new HashMap<>(CHAIN_COLORS);
                data.chainVisibility = new HashMap<>(CHAIN_VISIBILITY);
                
                GSON.toJson(data, writer);
            }
            
            saveBackup();

        } catch (IOException e) {
            System.err.println("[ChainConfig] Failed to save waypoint data: " + e.getMessage());
        }
    }
    
    private static void load() {
        if (!WAYPOINTS_FILE.exists()) {
            System.err.println("[ChainConfig] Config file does not exist. Checking for backup...");
            loadOrRestore();
            return;
        }
        
        try (FileReader reader = new FileReader(WAYPOINTS_FILE)) {
            WaypointData data = GSON.fromJson(reader, WaypointData.class);
            if (data == null) {
                System.err.println("[ChainConfig] Invalid config detected. Attempting restore...");
                loadOrRestore();
                return;
            }
            
            loadUserChains(data);
            loadWaypointState(data);
            loadPresetProgress(data);

        } catch (JsonSyntaxException | JsonIOException e) {
            System.err.println("[ChainConfig] Malformed JSON: " + e.getMessage());
            loadOrRestore();
        } catch (IOException e) {
            System.err.println("[ChainConfig] Failed to load waypoint data: " + e.getMessage());
            loadOrRestore();
        }
    }
    
    private static void loadOrRestore() {
        if (BACKUP_FILE.exists()) {
            System.err.println("[ChainConfig] Restoring from backup...");
            try {
                Files.copy(BACKUP_FILE.toPath(), WAYPOINTS_FILE.toPath(), StandardCopyOption.REPLACE_EXISTING);
                load();
                restoredConfig = true;
                return;
            } catch (IOException e) {
                System.err.println("[ChainConfig] Backup restore failed: " + e.getMessage());
            }
        }

        System.err.println("[ChainConfig] No backup found. Creating default config...");
        save();
        recreatedConfig = true;
    }
    
    public static void saveBackup() {
        try {
            if (WAYPOINTS_FILE.exists()) {
                Files.copy(WAYPOINTS_FILE.toPath(), BACKUP_FILE.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.err.println("[ChainConfig] Failed to create backup: " + e.getMessage());
        }
    }

    private static void loadUserChains(WaypointData data) {
        USER_CHAINS.clear();
        if (data.userChains != null) {
            for (UserChainData chainData : data.userChains) {
                List<Waypoint> waypoints = new ArrayList<>();
                if (chainData.waypoints != null) {
                    for (UserWaypointData waypointData : chainData.waypoints) {
                        waypoints.add(new Waypoint(
                            new BlockPos(waypointData.x, waypointData.y, waypointData.z),
                            waypointData.label,
                            waypointData.visited
                        ));
                    }
                }
                USER_CHAINS.add(new WaypointChain(chainData.area, chainData.name, waypoints, ChainType.USER_DEFINED));
            }
        }    
    }

    private static void loadWaypointState(WaypointData data) {
        CHAIN_COLORS.clear();
        if (data.chainColors != null) {
            for (Map.Entry<String, Object> entry : data.chainColors.entrySet()) {
                if (entry.getValue() instanceof Number n) {
                    CHAIN_COLORS.put(entry.getKey(), n.intValue());
                }
            }
        }
        
        CHAIN_VISIBILITY.clear();
        if (data.chainVisibility != null) {
            for (Map.Entry<String, Object> entry : data.chainVisibility.entrySet()) {
                if (entry.getValue() instanceof Boolean b) {
                    CHAIN_VISIBILITY.put(entry.getKey(), b);
                }
            }
        }
    }

    private static void loadPresetProgress(WaypointData data) {
        PRESET_VISITED_WAYPOINTS.clear();
        if (data.presetCompletions != null) {
            for (Map.Entry<String, List<WaypointPosition>> entry : data.presetCompletions.entrySet()) {
                Set<BlockPos> visited = new HashSet<>();
                for (WaypointPosition posData : entry.getValue()) {
                    visited.add(new BlockPos(posData.x, posData.y, posData.z));
                }
                PRESET_VISITED_WAYPOINTS.put(entry.getKey(), visited);
            }
        }
    }

    // --- Preset Chains ---

    private static void loadAllPresetLocations() {
        try {
            var client = MinecraftClient.getInstance();
            if (client == null || client.getResourceManager() == null) {
                return;
            }
            
            InputStream stream = client.getResourceManager()
                .getResource(Identifier.of("fishyaddons", "data/preset_locations.json"))
                .orElseThrow(() -> new RuntimeException("preset_locations.json not found"))
                .getInputStream();
            
            InputStreamReader reader = new InputStreamReader(stream);
            PresetDataRoot rootData;

            try (java.util.Scanner scanner = new java.util.Scanner(reader)) {
                scanner.useDelimiter("\\Z");
                String jsonContent = scanner.next();
                rootData = GSON.fromJson(jsonContent, PresetDataRoot.class);
            }

            reader.close();
            
            if (rootData != null && rootData.presets != null) {
                for (Map.Entry<String, AreaPresets> entry : rootData.presets.entrySet()) {
                    String area = entry.getKey();
                    AreaPresets areaPresets = entry.getValue();
                    
                    if (areaPresets != null && areaPresets.chains != null) {
                        for (Map.Entry<String, PresetChainData> chainEntry : areaPresets.chains.entrySet()) {
                            String chainName = chainEntry.getKey();
                            PresetChainData chainData = chainEntry.getValue();
                            
                            if (chainData != null && chainData.locations != null) {
                                List<Waypoint> waypoints = new ArrayList<>();
                                for (int i = 0; i < chainData.locations.size(); i++) {
                                    PresetLocation loc = chainData.locations.get(i);
                                    String label = chainData.labelTemplate != null ? 
                                        chainData.labelTemplate.replace("{index}", String.valueOf(i + 1)) :
                                        chainName + " " + (i + 1);
                                    
                                    waypoints.add(new Waypoint(
                                        new BlockPos(loc.x, loc.y, loc.z),
                                        label,
                                        false
                                    ));
                                }
                                
                                String presetKey = chainData.key != null ? chainData.key : ("preset_" + area + "_" + chainName);
                                String displayName = chainData.displayName != null ? chainData.displayName : chainName;
                                
                                PRESET_CHAINS.put(presetKey, new WaypointChain(
                                    area,
                                    displayName,
                                    waypoints,
                                    ChainType.MOD_PRESET
                                ));
                            }
                        }
                    }
                }
            }
            
        } catch (IOException | JsonSyntaxException e) {
            System.err.println("[ChainConfig] Failed to load preset locations: " + e.getMessage());
        }
    }

    private static class PresetDataRoot {
        protected Map<String, AreaPresets> presets;
    }
    
    private static class AreaPresets {
        protected Map<String, PresetChainData> chains;
    }
    
    private static class PresetChainData {
        protected List<PresetLocation> locations;
        protected String displayName;
        protected String labelTemplate;
        protected String key;
    }
    
    private static class PresetLocation {
        protected int x;
        protected int y;
        protected int z;
    }
    
    private static class WaypointData {
        public List<UserChainData> userChains;
        public Map<String, List<WaypointPosition>> presetCompletions;
        public Map<String, Object> chainColors;
        public Map<String, Object> chainVisibility;
    }
    
    private static class WaypointPosition {
        public int x;
        public int y;
        public int z;

        public WaypointPosition(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
    
    private static class UserChainData {
        public String area;
        public String name;
        public List<UserWaypointData> waypoints;
    }
    
    private static class UserWaypointData {
        public int x;
        public int y;
        public int z;
        public String label;
        public boolean visited;
    }
}
