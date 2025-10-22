package me.valkeea.fishyaddons.tracker.fishing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import me.valkeea.fishyaddons.config.StatConfig;
import me.valkeea.fishyaddons.tracker.ActivityMonitor;

/**
 * Registry for sc spawn requirements.
 */
public class ScRegistry {
    private static ScRegistry instance = null;
    private final Map<String, CreatureData> creatures = new LinkedHashMap<>();
    private boolean initialized = false;
    private String lastArea = "";
    private List<String> checked = new ArrayList<>();
    
    private String lastConditionsHash = "";
    private Map<String, Boolean> conditionCache = new HashMap<>();
    
    // Area constants
    private static final String GALATEA = "galatea";
    private static final String CRIMSON_HOTSPOT = "crimson_hotspot";
    private static final String CRIMSON_PLHLEG = "crimson_plhleg";
    private static final String CRIMSON_ISLES = "crimson_isles";
    private static final String JERRY = "jerry";
    private static final String BAYOU = "bayou";
    private static final String CRYSTAL_HOLLOWS = "crystal_hollows";
    private static final String PARK = "park";
    private static final String ANY = "any";

    private ScRegistry() {}

    public static ScRegistry getInstance() {
        if (instance == null) {
            instance = new ScRegistry();
        }
        return instance;
    }
    
    private void ensureInit() {
        if (!initialized) {
            initializeCreatures();
            initialized = true;
        }
    }
    
    /**
     * Represents a sea creature with its spawn requirements and metadata
     */
    public static class CreatureData {
        private final String id;
        private final String displayName;
        private final Set<String> validAreas;
        private final List<SpawnRequirement> requirements;
        private final boolean isTracked;
        
        public CreatureData(String id, String displayName, Set<String> validAreas, 
                           List<SpawnRequirement> requirements, boolean isTracked) {
            this.id = id;
            this.displayName = displayName;
            this.validAreas = Set.copyOf(validAreas);
            this.requirements = List.copyOf(requirements);
            this.isTracked = isTracked;
        }
        
        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public Set<String> getValidAreas() { return validAreas; }
        public List<SpawnRequirement> getRequirements() { return requirements; }
        public boolean isTracked() { return isTracked; }
        
        /**
         * Check if this creature can spawn/be tracked in the given area with current conditions
         */
        public boolean canSpawnIn(String area) {
            if (!validAreas.contains(area) && !validAreas.contains("any")) { //
                return false;
            }
            
            return requirements.stream().allMatch(SpawnRequirement::isMet);
        }
        
        /**
         * Check if this creature should be incremented when another creature is caught
         */
        public boolean shouldIncrementWhen(String caughtCreatureId, String area) {
            return canSpawnIn(area) && 
                   !id.equals(caughtCreatureId) &&
                   !StatConfig.isIgnoredSc(id);
        }
    }
    
    private void initializeCreatures() {

        SpawnRequirement always = new SpawnRequirement("always", () -> true);

        SpawnRequirement inHotspot = new SpawnRequirement("hotspot", 
            () -> {
                try { return ScStats.isHspt(); } catch (Exception e) { return false; }
            });

        SpawnRequirement inPool = new SpawnRequirement("pool", 
            () -> {
                try { return ScStats.isPool(); } catch (Exception e) { return false; }
            });

        SpawnRequirement notPool = new SpawnRequirement("not_pool", 
            () -> {
                try { return !ScStats.isPool(); } catch (Exception e) { return true; }
            });

        SpawnRequirement isSpooky = new SpawnRequirement("spooky", 
            () -> {
                try { 
                    return ActivityMonitor.getInstance().isActive(ActivityMonitor.Currently.SPOOKY); 
                } catch (Exception e) { return false; }
            });

        SpawnRequirement isShark = new SpawnRequirement("shark", 
            () -> {
                try { 
                    return ActivityMonitor.getInstance().isActive(ActivityMonitor.Currently.SHARK); 
                } catch (Exception e) { return false; }
            });            

        SpawnRequirement notGalatea = new SpawnRequirement("not_galatea", 
            () -> {
                try { return !GALATEA.equals(ScStats.getArea()); } catch (Exception e) { return true; }
            });
            
        SpawnRequirement water = new SpawnRequirement("water", 
            () -> {
                try { return !ScStats.getArea().contains("crimson"); } catch (Exception e) { return true; }
            });

        // Crimson Isles
        registerCreature(Sc.THUNDER, "§dThunder", 
            Set.of(CRIMSON_ISLES), List.of(always), true);
            
        registerCreature(Sc.HSPT_THUNDER, "§dThunder §8(§3Hotspot§8)", 
            Set.of(CRIMSON_HOTSPOT), List.of(always, notPool), true);
            
        registerCreature(Sc.POOL_THUNDER, "§dThunder §8(§3Pool§8)", 
            Set.of(CRIMSON_PLHLEG), List.of(always), true);
            
        registerCreature(Sc.JAWBUS, "§cLord Jawbus", 
            Set.of(CRIMSON_ISLES), List.of(always), true);
            
        registerCreature(Sc.HSPT_JAWBUS, "§cLord Jawbus §8(§3Hotspot§8)", 
            Set.of(CRIMSON_HOTSPOT), List.of(always, notPool), true);
            
        registerCreature(Sc.POOL_JAWBUS, "§cLord Jawbus §8(§3Pool§8)", 
            Set.of(CRIMSON_PLHLEG), List.of(always), true);
            
        registerCreature(Sc.RAGNAROK, "§cRagnarok", 
            Set.of(CRIMSON_HOTSPOT, CRIMSON_PLHLEG), List.of(inHotspot), true);
            
        registerCreature(Sc.PLHLEG, "§dPlhlegblast", 
            Set.of(CRIMSON_PLHLEG), List.of(inPool), true);
        
        // Jerry Island
        registerCreature(Sc.YETI, "§6Yeti", 
            Set.of(JERRY), List.of(always), true);
            
        registerCreature(Sc.DRAKE, "§dReindrake", 
            Set.of(JERRY), List.of(always), true);
        
        // Galatea
        registerCreature(Sc.ENT, "§5Ent", 
            Set.of(GALATEA), List.of(always), true);
            
        registerCreature(Sc.EMP, "§6Loch Emperor", 
            Set.of(GALATEA), List.of(always), true);
        
        // Bayou, Ch, Park
        registerCreature(Sc.TITANOBOA, "§dTitanoboa", 
            Set.of(BAYOU), List.of(always), true);
            
        registerCreature(Sc.CH_MINER, "§6Abyssal Miner", 
            Set.of(CRYSTAL_HOLLOWS), List.of(always), true);
            
        registerCreature(Sc.NIGHT_SQUID, "§5Night Squid", 
            Set.of(PARK), List.of(always), true);
        
        // Special conditions
        registerCreature(Sc.WATER_HYDRA, "§6Water Hydra", 
            Set.of(ANY), List.of(notGalatea, water), true);
            
        registerCreature(Sc.TIKI, "§dWiki Tiki", 
            Set.of(ANY), List.of(inHotspot, water), true);
            
        registerCreature(Sc.GRIM_REAPER, "§5Grim Reaper", 
            Set.of(ANY), List.of(isSpooky, water), true);

        registerCreature(Sc.GW, "§6Great White Shark", 
            Set.of(ANY), List.of(isShark, water), true);    
    }
    
    private void registerCreature(String id, String displayName, Set<String> validAreas, 
                                 List<SpawnRequirement> requirements, boolean isTracked) {
        creatures.put(id, new CreatureData(id, displayName, validAreas, requirements, isTracked));
    }
    
    /**
     * Get all creatures that can spawn in the given area with current conditions
     */
    public List<String> getCreaturesForArea(String area) {
        ensureInit();
        
        String currentConditionsHash = generateConditionsHash(area);
        
        if (!area.equals(lastArea) || !currentConditionsHash.equals(lastConditionsHash)) {
            lastArea = area;
            lastConditionsHash = currentConditionsHash;
            
            checked = creatures.values().stream()
                .filter(creature -> creature.canSpawnIn(area))
                .filter(creature -> !StatConfig.isIgnoredSc(creature.getId()))
                .map(CreatureData::getId)
                .toList();
        }
        return checked;
    }
    
    /**
     * Generate a hash of current conditions that affect spawn requirements
     */
    private String generateConditionsHash(String area) {
        try {
            StringBuilder hash = new StringBuilder();
            hash.append(area).append("|");
            hash.append(ScStats.isHspt()).append("|");
            hash.append(ScStats.isPool()).append("|");
            hash.append(ActivityMonitor.getInstance().isActive(ActivityMonitor.Currently.SPOOKY)).append("|");
            hash.append(ActivityMonitor.getInstance().isActive(ActivityMonitor.Currently.SHARK)).append("|");
            hash.append(ScStats.getArea());
            return hash.toString();
        } catch (Exception e) {
            return String.valueOf(System.currentTimeMillis());
        }
    }
    
    /**
     * Get all creatures that should be incremented when the given creature is caught in the given area
     */
    public List<String> getCreaturesToIncrement(String caughtCreatureId, String area) {
        ensureInit();
        return creatures.values().stream()
            .filter(creature -> creature.shouldIncrementWhen(caughtCreatureId, area))
            .map(CreatureData::getId)
            .toList();
    }
    
    public boolean isTracked(String creatureId) {
        ensureInit();
        CreatureData creature = creatures.get(creatureId);
        return creature != null && creature.isTracked();
    }

    public boolean canSpawnIn(String creatureId, String area) {
        CreatureData creature = creatures.get(creatureId);
        return creature != null && creature.canSpawnIn(area);
    }    
    
    public String getDisplayName(String creatureId) {
        ensureInit();
        CreatureData creature = creatures.get(creatureId);
        return creature != null ? creature.getDisplayName() : "§7" + creatureId.replace("_", " ");
    }
    
    public List<String> getTrackedCreatures() {
        ensureInit();
        return creatures.values().stream()
            .filter(CreatureData::isTracked)
            .map(CreatureData::getId)
            .toList();
    }
    
    public CreatureData getCreature(String id) {
        return creatures.get(id);
    }
    
    public Collection<CreatureData> getAllCreatures() {
        return creatures.values();
    }
    
    public void clearCache() {
        lastArea = "";
        lastConditionsHash = "";
        checked.clear();
        conditionCache.clear();
    }
}