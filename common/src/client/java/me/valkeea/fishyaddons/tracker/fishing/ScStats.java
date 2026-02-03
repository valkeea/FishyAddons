package me.valkeea.fishyaddons.tracker.fishing;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas;
import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas.Island;
import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.StatConfig;
import me.valkeea.fishyaddons.event.impl.EnvironmentChangeEvent;
import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.event.impl.ScCatchEvent;
import me.valkeea.fishyaddons.tool.RunDelayed;
import me.valkeea.fishyaddons.tracker.ActivityMonitor;
import me.valkeea.fishyaddons.util.FishyNotis;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public class ScStats {
    private static final double POSITION_CHECK_THRESHOLD = 5.0;
    private static final String CRIMSON_PREFIX = "crimson_";     
    private static final String SINCE_PREFIX = "since_";
    private static final String JAWBUS_VIAL_KEY = "jawbus_since_vial";

    private final Map<String, Integer> creatureCounters = new ConcurrentHashMap<>();

    private final List<Island> validAreas = List.of(
        Island.CI,
        Island.GAL,
        Island.BAYOU,
        Island.JERRY,
        Island.CH,
        Island.DEN,
        Island.HUB,
        Island.PARK
    );

    private final List<Island> hotspotAreas = List.of(
        Island.CI,
        Island.BAYOU,
        Island.DEN,
        Island.HUB
    );

    private int jawbusSinceVial = 0;
    private boolean loaded = false;  

    private static Vec3d lastCheckedPosition = null;    
    private static boolean enabled = false;
    private static boolean countDh = false;
    private static boolean announce = false;
    private static boolean isPlhlegPool = false;
    private static boolean isHotspot = false;
    private static boolean registered = false;

    private static Island area = Island.NA;
    private static ScStats instance = null;
    private static final Object COUNTER_LOCK = new Object();

    private ScStats() {}

    public static ScStats getInstance() {
        if (instance == null) {
            instance = new ScStats();
        }
        return instance;
    }
    
    public static void init() {
        enabled = FishyConfig.getState(me.valkeea.fishyaddons.config.Key.TRACK_SCS, true);
        countDh = FishyConfig.getState(me.valkeea.fishyaddons.config.Key.TRACK_SCS_WITH_DH, false);
        announce = FishyConfig.getState(me.valkeea.fishyaddons.config.Key.SC_SINCE, false);

        if (enabled) {
            ScStats instance = getInstance();
            
            if (!registered) {
                FaEvents.SEA_CREATURE_CATCH.register(instance::handleMatch);
                FaEvents.ENVIRONMENT_CHANGE.register(ScStats::setArea);
                registered = true;
            }
            
            instance.load();
            ScData.refresh();
        }
    }

    // --- Area / Context Management ---
    public static void setArea(EnvironmentChangeEvent event) {
        Island newArea = event.newIsland();
        if (!getInstance().validAreas.contains(newArea)) {
            area = Island.NA;
        } else {
            area = newArea;
        }
    }

    /**
     * Set sub-area context (hotspot or plhlegblast pool) based on nearby armor stand
     * @param hspt The detected hotspot armor stand, or null if none found
     */
    public static void setSubArea(@Nullable ArmorStandEntity hspt) {
        if (getInstance().hotspotAreas.contains(area) && enabled && MinecraftClient.getInstance().player != null) {
            if (hspt == null) {
                resetHspt();
                return;
            }

            var playerPos = MinecraftClient.getInstance().player.getPos();
            var hsptPos = hspt.getPos();
            var horizontalDistance = Math.sqrt(Math.pow(playerPos.x - hsptPos.x, 2) + Math.pow(playerPos.z - hsptPos.z, 2));

            if (horizontalDistance <= 13.0 && !isHotspot) {
                isHotspot = true;
            }
        } else {
            resetHspt();
        }
    }

    public static Island getArea() { return area; }
    public static boolean isHspt() { return isHotspot; }
    public static boolean isPool() { return isPlhlegPool; }

    public static void resetHspt() {
        if (isHotspot) {
            isHotspot = false;
        }
    }
    
    private static void checkPoolStatus() {
        if (SkyblockAreas.isCrimson()) {
            boolean currentPoolStatus = updatePoolStatus();
            if (currentPoolStatus != isPlhlegPool) {
                isPlhlegPool = currentPoolStatus;
            }
        }
    }    
    
    private static boolean updatePoolStatus() {
        var client = MinecraftClient.getInstance();

        if (client.player == null) {
            return false;
        }
        
        var currentPos = client.player.getPos();
        if (lastCheckedPosition != null) {

            var distanceMoved = currentPos.distanceTo(lastCheckedPosition);
            if (distanceMoved < POSITION_CHECK_THRESHOLD) {
                return isPlhlegPool;
            }
        }
        
        lastCheckedPosition = currentPos;
        return isPool3d(currentPos);
    }
    
    private static boolean isPool3d(Vec3d pos) {
        
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;
        
        boolean inPoolX = x >= -400 && x <= -355;
        boolean inPoolY = y >= 66 && y <= 84;
        boolean inPoolZ = z >= -725 && z <= -680;

        return inPoolX && inPoolY && inPoolZ;
    }

    // --- Chat Handling ---

    public boolean checkForVial(String s) {
        if (s.startsWith("rare drop! radioactive vial")) {
            sendTookJawbusForVial();
            
            synchronized (COUNTER_LOCK) {
                jawbusSinceVial = 0;
            }
            
            save(JAWBUS_VIAL_KEY, 0);
            return true;
        }
        return false;
    }

    public void handleMatch(ScCatchEvent event) {

        if (!enabled) return;

        String id = event.seaCreatureId;
        if (id != null) {
            checkSpecialConditions(id);
            updateCounters(id, event.wasDh);
        }
    }

    private static void checkSpecialConditions(String name) {
        if (name.equals("scarecrow") || name.equals("grim_reaper") ||
            name.equals("nightmare") || name.equals("phantom_fisher")) {
            ActivityMonitor.getInstance().recordActivity(ActivityMonitor.Currently.SPOOKY);
        }
        
        if (name.contains("shark")) {
            ActivityMonitor.getInstance().recordActivity(ActivityMonitor.Currently.SHARK);
        }   
    }

    private static void updateDataOnCatch(String creatureId, int count) {
        ScData.onCatch(creatureId, count);
    }    

    // --- Increment and get counter values ---

    private void updateCounters(String creatureId, boolean isDh) {
        if (!loaded) {
            loadAllCounters();
            loaded = true;
        }

        int increment = isDh && countDh ? 2 : 1;
        
        if (SkyblockAreas.isCrimson()) {
            checkPoolStatus();
        }

        Island currentArea = getCurrentAreaKey();
        
        StatConfig.beginBatch();
        try {
            processCatch(Sc.genAreaKey(creatureId), currentArea, increment, isDh);
        } finally {
            StatConfig.endBatch();
        }
    }

    private void processCatch(String creatureId, Island currentArea, int increment, boolean wasDh) {
        if (Sc.isTracked(creatureId) && Sc.canSpawnIn(creatureId, currentArea)) {
            
            int counterValue;
            String displayName = Sc.displayName(creatureId);
            
            synchronized (COUNTER_LOCK) {
                counterValue = getCounterFor(creatureId);
                setCounterFor(creatureId, 0);
            }
            
            sendTookXScFor(displayName, counterValue, wasDh);
            updateDataOnCatch(creatureId, counterValue);
            
            String finalCreatureId = creatureId;
            save(SINCE_PREFIX + finalCreatureId, 0);
            
            if (creatureId.contains(Sc.JAWBUS)) {
                sendJawbusSinceVial();
                
                synchronized (COUNTER_LOCK) {
                    jawbusSinceVial += increment;
                }
                save(JAWBUS_VIAL_KEY, jawbusSinceVial);
            }
        }

        incrementCounters(creatureId, currentArea, increment);
    }

    private void incrementCounters(String caughtCreatureId, Island currentArea, int increment) {
        List<String> creaturesToIncrement = ScRegistry.getInstance()
            .getCreaturesToIncrement(caughtCreatureId, currentArea);
            
        for (String creatureId : creaturesToIncrement) {
            increment(creatureId, increment);
        }
    }
    
    protected int getCounterFor(String creatureId) {
        return creatureCounters.getOrDefault(creatureId, 0);
    }
    
    private void setCounterFor(String creatureId, int value) {
        creatureCounters.put(creatureId, value);
    }
    
    private void increment(String creatureId, int amount) {
        creatureCounters.put(creatureId, getCounterFor(creatureId) + amount);
    }

    // --- Load and Save ---

    private void loadAllCounters() {

        Sc.getTrackedCreatures().forEach(creatureId -> 
            creatureCounters.put(creatureId, StatConfig.getSince(SINCE_PREFIX + creatureId))
        );

        ScData.getInstance().loadCatchRates();

        jawbusSinceVial = StatConfig.getSince(JAWBUS_VIAL_KEY);
    }

    public void load() {
        if (enabled && !loaded) {
            loadAllCounters();
        }
    }    

    public void save() {
        if (!enabled || !loaded) return;
        
        boolean success = true;
        
        try {
            for (Map.Entry<String, Integer> entry : creatureCounters.entrySet()) {
                save(SINCE_PREFIX + entry.getKey(), entry.getValue());
            }
            
            save(JAWBUS_VIAL_KEY, jawbusSinceVial);
            
        } catch (Exception e) {
            System.err.println("ScStats: Failed to save data: " + e.getMessage());
            success = false;
        }
        
        if (success) {
            creatureCounters.clear();
            jawbusSinceVial = 0;
            loaded = false;
        } else {
            System.err.println("ScStats: Keeping data in memory due to save failure");
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    private static void save(String key, int value) {
        StatConfig.setSince(key, value);
    }

    public Island getCurrentAreaKey() {

        if (Island.CI.equals(area)) {
            if (isPlhlegPool) {
                return Island.PLHLEGBLAST;
            } else if (isHotspot) {
                return Island.CI_HOTSPOT;
            }
        }
        
        return area;
    }

    // --- User Notifications ---
    
    private static void sendTookJawbusForVial() {
        if (!announce) return;
        var message = String.format("§7It took §b%d §cJawbus §7 to drop §dRadioactive Vial!", getInstance().jawbusSinceVial);
        announceAfter(message);
    }

    private static void sendJawbusSinceVial() {
        if (!announce) return;
        var message = String.format("§cJawbus §8since vial: §b%d", getInstance().jawbusSinceVial);
        announceAfter(message);
    }

    private static void sendTookXScFor(String creature, int count, boolean wasDh) {
        if (!announce) return;
        var message = String.format("Took §d%d§7 Sc for %s", count, creature);
        message += wasDh ? " §8(DH)§7!" : "§7!";
        announceAfter(message);
    }

    private static void announceAfter(String msg) {
        final String m = msg;
        RunDelayed.run(() -> FishyNotis.send(Text.literal(m)), 100, null);
    }

    public void sendStats() {
        if (!enabled) return;
        if (!loaded) {
            loadAllCounters();
            loaded = true;
        }

        var sb = new StringBuilder();
        sb.append("§3Sc since in ");
        
        var currentArea = getCurrentAreaKey();
        var isCi = currentArea.key().startsWith(CRIMSON_PREFIX);

        if (!validAreas.contains(currentArea) && !isCi && !isHotspot) {
            sb.append("§bN/A §c(Invalid Area)");
            FishyNotis.send(Text.literal(sb.toString()));
            return;
        }

        String areaKey = currentArea.key();
        sb.append("§7").append(areaKey.substring(0, 1).toUpperCase())
          .append(areaKey.substring(1).replace("_", " "));

        if (isCi) {
            sb = buildCiStats(sb);
        } else {
            buildAreaStats(sb, currentArea);
        }
    
        FishyNotis.send(Text.literal(sb.toString()));
    }
    
    private StringBuilder buildAreaStats(StringBuilder sb, Island currentArea) {
        List<String> spawnableCreatures = ScRegistry.getInstance().getCreaturesForArea(currentArea);
        
        for (String creatureId : spawnableCreatures) {
            String displayName = ScRegistry.getInstance().getDisplayName(creatureId);
            int count = getCounterFor(creatureId);
            sb.append("§7, ").append(displayName).append("§7: §b").append(count);
        }

        return sb;
    }

    private StringBuilder buildCiStats(StringBuilder sb) {

        var ciSb = new StringBuilder(sb);

        ciSb.append("§8 - §cRagnarok: §b").append(getCounterFor(Sc.RAGNAROK));
        ciSb.append("§7, §cPlhlegblast: §b").append(getCounterFor(Sc.PLHLEG));
        ciSb.append("§7, §cThunder: §b").append(getCounterFor(Sc.genAreaKey(Sc.THUNDER)));
        ciSb.append("§7, §cJawbus: §b").append(getCounterFor(Sc.genAreaKey(Sc.JAWBUS)));
        ciSb.append("§7 (§cJawbus §7since §dVial: §b").append(jawbusSinceVial).append("§7)");

        if (isHotspot || isPlhlegPool) ciSb.append("§8 - §7Currently in:");
        if (isHotspot) ciSb.append(" §8[§3Hotspot§8]");
        if (isPlhlegPool) ciSb.append(" §8[§3Pool§8]");

        return ciSb;
    }
}
