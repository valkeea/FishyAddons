package me.valkeea.fishyaddons.tracker.fishing;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import me.valkeea.fishyaddons.config.FilterConfig.MessageContext;
import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.RuleFactory;
import me.valkeea.fishyaddons.config.StatConfig;
import me.valkeea.fishyaddons.processor.MessageAnalysis;
import me.valkeea.fishyaddons.processor.SharedMessageDetector;
import me.valkeea.fishyaddons.tracker.ActivityMonitor;
import me.valkeea.fishyaddons.tracker.SkillTracker;
import me.valkeea.fishyaddons.util.AreaUtils;
import me.valkeea.fishyaddons.util.FishyNotis;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public class ScStats {
    private static final double POSITION_CHECK_THRESHOLD = 5.0;

    private static final String CI = "crimson_isles";
    private static final String CH = "crystal_hollows";    
    private static final String GALATEA = "galatea";
    private static final String BAYOU = "bayou";    
    private static final String JERRY = "jerry";
    private static final String DEN = "den";
    private static final String HUB = "hub";
    private static final String PARK = "park";
    private static final String NA = "invalid";
    private static final String CRIMSON_PREFIX = "crimson_";     
    private static final String SINCE_PREFIX = "since_";
    private static final String JAWBUS_VIAL_KEY = "jawbus_since_vial";

    private final Map<String, Integer> creatureCounters = new ConcurrentHashMap<>();

    private final List<String> validAreas = List.of(
        CI,
        GALATEA,
        BAYOU,
        JERRY,
        CH,
        DEN,
        HUB,
        PARK
    );

    private final List<String> hotspotAreas = List.of(
        CI,
        BAYOU,
        DEN,
        HUB
    );

    private int jawbusSinceVial = 0;
    private boolean loaded = false;  

    private static Vec3d lastCheckedPosition = null;    
    private static boolean enabled = false;
    private static boolean countDh = false;
    private static boolean announce = false;
    private static boolean isPlhlegPool = false;
    private static boolean isHotspot = false;

    private static String area = "";
    private static ScStats instance = null;

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
            instance.load();
            ScData.refresh();
        }
    }

    // --- Area / Context Management ---
    public static void setArea(String newArea) {
        if (!getInstance().validAreas.contains(newArea)) {
            area = NA;
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

    public static String getArea() { return area; }
    public static boolean isHspt() { return isHotspot; }
    public static boolean isPool() { return isPlhlegPool; }

    public static void resetHspt() {
        if (isHotspot) {
            isHotspot = false;
        }
    }
    
    private static void checkPoolStatus() {
        if (AreaUtils.isCrimson()) {
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

    public void checkForVial(String s) {
        if (s.startsWith("rare drop! radioactive vial")) {
            sendTookJawbusForVial();            
            jawbusSinceVial = 0;
            StatConfig.setSince(JAWBUS_VIAL_KEY, 0);
        }
    }

    public boolean handleMatch(String s) {
        MessageAnalysis analysis = SharedMessageDetector.analyzeMessage(s);
        if (analysis.hasFilterMatches()) {

            var firstMatch = analysis.getFilterMatches().get(0);
            var ruleName = firstMatch.getRuleName();
            if (ruleName.startsWith("sc_")) {
                String creatureId = ruleName.replace("sc_", "");
                
                ActivityMonitor.getInstance().recordActivity(ActivityMonitor.Currently.FISHING);

                boolean isDoubleHook = false;
                if (MessageContext.hasTriggerWithin(RuleFactory.getDhTriggers(), 50)) {
                    isDoubleHook = true;
                }

                checkSpecialConditions(creatureId);

                updateCounters(creatureId, isDoubleHook);
                return true;
            }
        }
        return false;
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

    private static void onCatch(String creatureId, int count) {
        ScData.onCatch(creatureId, count);
    }    

    // --- Increment and get counter values ---

    private void updateCounters(String creatureId, boolean isDoubleHook) {
        if (!loaded) {
            loadAllCounters();
            loaded = true;
        }

        int increment = isDoubleHook && countDh ? 2 : 1;
        
        if (AreaUtils.isCrimson()) {
            checkPoolStatus();
        }
        
        String currentArea = getCurrentAreaKey();
        processCatch(Sc.genAreaKey(creatureId), currentArea, increment, isDoubleHook);
    }

    private void processCatch(String creatureId, String currentArea, int increment, boolean wasDh) {
        if (Sc.isTracked(creatureId) && Sc.canSpawnIn(creatureId, currentArea)) {
            
            int counterValue = getCounterFor(creatureId);
            String displayName = Sc.displayName(creatureId);
            
            sendTookXScFor(displayName, counterValue, wasDh);
            onCatch(creatureId, counterValue);
            setCounterFor(creatureId, 0);
            
            String finalCreatureId = creatureId;
            CompletableFuture.runAsync(() -> {
                try {
                    StatConfig.setSince(SINCE_PREFIX + finalCreatureId, 0);
                } catch (Exception e) {
                    System.err.println("Failed to save counter reset for " + finalCreatureId + ": " + e.getMessage());
                }
            });
            
            if (creatureId.contains(Sc.JAWBUS)) {
                sendJawbusSinceVial();
                jawbusSinceVial += increment;
                
                CompletableFuture.runAsync(() -> {
                    try {
                        StatConfig.setSince(JAWBUS_VIAL_KEY, jawbusSinceVial);
                    } catch (Exception e) {
                        System.err.println("Failed to save jawbus vial counter: " + e.getMessage());
                    }
                });
            }
        }
        SkillTracker.getInstance().onCatch(wasDh);       
        incrementCounters(creatureId, currentArea, increment);
    }
    
    private void incrementCounters(String caughtCreatureId, String currentArea, int increment) {
        List<String> creaturesToIncrement = ScRegistry.getInstance()
            .getCreaturesToIncrement(caughtCreatureId, currentArea);
            
        for (String creatureId : creaturesToIncrement) {
            increment(creatureId, increment);
        }
    }
    
    private int getCounterFor(String creatureId) {
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

        ScData.getInstance().loadCatchPercentages();

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
                StatConfig.setSince(SINCE_PREFIX + entry.getKey(), entry.getValue());
            }
            
            StatConfig.setSince(JAWBUS_VIAL_KEY, jawbusSinceVial);
            
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

    public String getCurrentAreaKey() {
        if (area.isEmpty()) {
            return NA;
        }
        
        if (CI.equals(area)) {
            if (isPlhlegPool) {
                return "crimson_plhleg";
            } else if (isHotspot) {
                return "crimson_hotspot";
            }
        }
        
        return area;
    }

    // --- User Notifications ---
    
    private static void sendTookJawbusForVial() {
        if (!announce) return;
        var message = String.format("§7It took §b%d §cJawbus §7 to drop §dRadioactive Vial!", getInstance().jawbusSinceVial);
        FishyNotis.send(Text.literal(message));
    }

    private static void sendJawbusSinceVial() {
        if (!announce) return;
        var message = String.format("§cJawbus §8since vial: §b%d", getInstance().jawbusSinceVial);
        FishyNotis.send(Text.literal(message));
    }

    private static void sendTookXScFor(String creature, int count, boolean wasDh) {
        if (!announce) return;
        var message = String.format("Took §d%d§7 Sc for %s", count, creature);
        message += wasDh ? " §8(DH)§7!" : "§7!";
        FishyNotis.send(Text.literal(message));
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
        var isCi = currentArea.startsWith(CRIMSON_PREFIX) || CI.equals(currentArea);

        if (!validAreas.contains(currentArea) && !isCi && !isHotspot) {
            sb.append("§bN/A §c(Invalid Area)");
            FishyNotis.send(Text.literal(sb.toString()));
            return;
        }

        sb.append("§7").append(currentArea.substring(0, 1).toUpperCase())
          .append(currentArea.substring(1).replace("_", " "));

        if (isCi) {
            sb = buildCiStats(sb);
        } else {
            buildAreaStats(sb, currentArea);
        }
    
        FishyNotis.send(Text.literal(sb.toString()));
    }
    
    private StringBuilder buildAreaStats(StringBuilder sb, String currentArea) {
        switch (currentArea) {
            case JERRY:
                sb.append("§7, §6Yeti§7: §b").append(getCounterFor(Sc.YETI));
                sb.append("§7, §dReindrake§7: §b").append(getCounterFor(Sc.DRAKE));
                break;
            case GALATEA:
                sb.append("§7, §5Ent§7: §b").append(getCounterFor(Sc.ENT));
                sb.append("§7, §6Loch Emperor§7: §b").append(getCounterFor(Sc.EMP));
                break;
            case BAYOU:
                sb.append("§7, §dTitanoboa§7: §b").append(getCounterFor(Sc.TITANOBOA));
                break;
            case CH:
                sb.append("§7, §6Abyssal Miner§7: §b").append(getCounterFor(Sc.CH_MINER));
                break;
            case PARK:
                sb.append("§7, §5Night Squid§7: §b").append(getCounterFor(Sc.NIGHT_SQUID));
                break;    
            default:
                // No area-specific creatures
                break;
        }

        if (isHotspot) {
            sb.append("§7, §dWiki Tiki§7: §b").append(getCounterFor(Sc.TIKI));
        }        
        
        if (!GALATEA.equals(currentArea)) {
            sb.append("§7, §5Water Hydra§7: §b").append(getCounterFor(Sc.WATER_HYDRA));
        }

        if (ActivityMonitor.getInstance().isActive(ActivityMonitor.Currently.SPOOKY)) {
            sb.append("§7, §5Grim Reaper§7: §b").append(getCounterFor(Sc.GRIM_REAPER));
        }

        if (ActivityMonitor.getInstance().isActive(ActivityMonitor.Currently.SHARK)) {
            sb.append("§7, §6Great White Shark§7: §b").append(getCounterFor(Sc.GW));
        }

        return sb;
    }

    private StringBuilder buildCiStats(StringBuilder sb) {
        StringBuilder ciSb = new StringBuilder(sb);
        ciSb.append("§8 - §cRagnarok: §b").append(getCounterFor(Sc.RAGNAROK));
        ciSb.append("§7, §cPlhlegblast: §b").append(getCounterFor(Sc.PLHLEG));
        ciSb.append("§7, §cThunder: §b").append(getCounterFor(Sc.genAreaKey(Sc.THUNDER)));
        ciSb.append("§7, §cJawbus: §b").append(getCounterFor(Sc.genAreaKey(Sc.JAWBUS)));
        ciSb.append("§7 (§cJawbus §7since §dVial: §b").append(jawbusSinceVial).append("§7)");
        if (isHotspot || isPlhlegPool) {
            ciSb.append("§8 - §7Currently in:");
        }
        if (isHotspot) ciSb.append(" §8[§3Hotspot§8]");
        if (isPlhlegPool) ciSb.append(" §8[§3Pool§8]");
        return ciSb;
    }
}