package me.valkeea.fishyaddons.tracker.collection;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import me.valkeea.fishyaddons.hud.ui.UIFeedback;

/**
 * Tracks baseline collection level, session gains, and pending high-tier drops.
 */
public class CollectionData {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File DATA_FILE = new File("config/fishyaddons/data/collectionprogress.json");
    
    private static final Map<String, Long> baselines = new HashMap<>();         // Name -> current known collection level
    private static final Map<String, Long> sessionGains = new HashMap<>();      // Name -> session gain
    private static final Map<String, Long> lastUpdated = new HashMap<>();       // Name -> last update timestamp
    private static final Map<String, Boolean> baselineStale = new HashMap<>();  // Name -> baseline stale flag

    private static final long BASELINE_DIFF_ABS_THRESHOLD = 10_000L;
    private static final double BASELINE_DIFF_PERCENT_THRESHOLD = 0.01;
    private static final int MAX_PENDING_DROPS = 15;

    // Unverified potential drop name -> pending quantity
    private static final Map<String, Integer> pendingDrops = new HashMap<>();

    // Seen drops that are verified to not be collections
    private static final Set<String> verifiedFalse = new HashSet<>();

    // Known collections
    private static final Set<String> sbCollections = new HashSet<>();
    
    // Word index for fast collection matching (word -> collections containing that word)
    private static final Map<String, Set<String>> collectionWordIndex = new HashMap<>();
    
    // Name -> basedrop (learnt by recipe analysis)
    private static final Map<String, String> baseDropConversions = new HashMap<>();

    // --- Drop Validation ---

    /**
     * These cover some common pitfalls but potential drop matching likely needs future calibrating !
     */
    static final Set<String> falseMatches = Set.of(
        "jungle", "heart", "nether", "magma", "feather", "shard", "flesh", "potato",
        "mushrooms", "dust", "halfeaten mushroom", "bone", "spider", "never-melt ice"
    );

    static final Set<String> specialCrafts = Set.of(
        "enchanted cookie", "bale", "slime"
    );

    private CollectionData() {}

    public static void init() {
        loadSbCollections();
        load();
    }


    // --- Type Validation ---

    private static void loadSbCollections() {
        try {
            InputStream stream = CollectionData.class.getResourceAsStream(
                "/assets/fishyaddons/data/collections.json"
            );
            
            if (stream == null) {
                System.err.println("[CollectionTracker] collections.json not found in resources!");
                return;
            }
            
            var reader = new InputStreamReader(stream);
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            @Nullable Map<String, Object> data = GSON.fromJson(reader, type);
            
            reader.close();
            
            if (data != null && data.containsKey("collections")) {

                @SuppressWarnings("unchecked")
                var collections = (Map<String, java.util.List<String>>) data.get("collections");
                
                sbCollections.clear();
                collectionWordIndex.clear();
                
                for (var categoryItems : collections.values()) {
                    normalizeAndIndex(categoryItems);
                }
            }
        } catch (Exception e) {
            System.err.println("[CollectionTracker] Failed to load collections.json: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void normalizeAndIndex(java.util.List<String> items) {
        for (String item : items) {
            String normalized = normalize(item);
            sbCollections.add(normalized);
            
            var words = normalized.split(" ");
            for (String word : words) {
                if (!word.isEmpty()) {
                    collectionWordIndex.computeIfAbsent(word, k -> new HashSet<>()).add(normalized);
                }
            }
        }
    }

    /** Check if an item is a known collection item */
    protected static boolean knownBaseDrop(String itemName) {
        return sbCollections.contains(normalize(itemName));
    }

    // --- Basedrop Mapping and Pending Drop Handling ---
    
    /**
     * Get the mapped collection for a crafted item (special cases and learnt mappings)
     * @param itemName The crafted item name
     * @return The collection name it maps to, or null if no mapping exists
     */
    protected static String getMappedCollection(String itemName) {
        itemName = normalize(itemName);
        
        String learnt = baseDropConversions.get(itemName);
        if (learnt != null) return learnt;
        
        return null;
    }
    
    /**
     * Map a crafted item to its base collection
     * @param craftedItem The crafted item name
     * @param collection The collection it belongs to
     */
    protected static void learnCraftMapping(String craftedItem, String collection) {
        final String craftedItemNormalized = normalize(craftedItem);
        final String collectionNormalized = normalize(collection);
        
        baseDropConversions.computeIfAbsent(craftedItemNormalized, k -> {
            save();
            return collectionNormalized;
        });
    }

    /**
     * Check if an item name contains collection keywords
     */
    protected static boolean isPotentialDrop(String itemName) {
        String normalized = normalize(itemName);
        
        if (baseDropConversions.containsKey(normalized) || specialCrafts.contains(normalized)) {
            return true;
        }

        if (falseMatches.stream().anyMatch(f -> normalized.contains(f)
            && !normalized.startsWith("enchanted"))) {
            verifiedFalse.add(normalized);    
            return false;
        }

        String stripped = normalized.replace("enchanted ", "");
        String[] words = stripped.split(" ");
        
        for (String word : words) {
            if (!word.isEmpty() && collectionWordIndex.containsKey(word)) {
                return true;
            }
        }
        
        verifiedFalse.add(normalized);
        return false;
    }

    /**
     * Add a pending drop for an unverified high-tier item.
     * These are processed later on recipe discovery.
     */
    protected static void addPendingDrop(String itemName, int amount) {
        itemName = normalize(itemName);
        int current = pendingDrops.getOrDefault(itemName, 0);
        pendingDrops.put(itemName, current + amount);

        while (pendingDrops.size() > MAX_PENDING_DROPS) {
            String oldestKey = pendingDrops.keySet().iterator().next();
            pendingDrops.remove(oldestKey);
        }
    }

    /**
     * Process pending drops when a recipe is discovered.
     * Converts pending drops to actual gains using the newly discovered conversion.
     */
    protected static void processPendingDrops(String enchantedName) {
        enchantedName = normalize(enchantedName);
        
        Integer pending = pendingDrops.remove(enchantedName);
        if (pending == null || pending == 0) {
            return;
        }
    
        int baseAmount = CraftingRecipes.convertToBase(enchantedName, pending);
        String baseItemName = CraftingRecipes.getBaseItem(enchantedName);
        
        if (baseItemName != null && baseAmount > 0) {
            addGain(baseItemName, baseAmount);
        }
    }

    /** Get pending drops count for an item */
    public static int getPendingDrops(String itemName) {
        return pendingDrops.getOrDefault(normalize(itemName), 0);
    }

    /** Get all pending drops */
    public static Map<String, Integer> getAllPendingDrops() {
        return new HashMap<>(pendingDrops);
    }    

    // --- Data handling ---

    /**
     * Update collection progress from a successful scan and
     * correct any tracking errors while preserving session continuity.
     */
    protected static void updateProgress(String itemName, long scannedTotal) {

        itemName = normalize(itemName);

        long oldBaseline = baselines.getOrDefault(itemName, 0L);
        long currentGain = sessionGains.getOrDefault(itemName, 0L);
        boolean wasStale = isBaselineStale(itemName);
        
        long calculated = oldBaseline + currentGain;
        long difference = scannedTotal - calculated;

        if (oldBaseline == 0) { // First encounter: set baseline
            long newBaseline = Math.max(scannedTotal - currentGain, 0L);
            baselines.put(itemName, newBaseline);
            
        // Baseline is stale and scan is far off: keep current session gains            
        } else if (wasStale && isLargeBaselineMismatch(oldBaseline, scannedTotal, difference)) {
            long newBaseline = Math.max(scannedTotal - currentGain, 0L);
            baselines.put(itemName, newBaseline);
            inform("Updated stale data for §b" + itemName);
            
        } else if (difference != 0) { // Reasonable discrepancy: adjust gains

            long adjustedGain = currentGain + difference;
            if (currentGain == 0) adjustedGain = 0;
            
            baselines.put(itemName, scannedTotal - adjustedGain);
            sessionGains.put(itemName, adjustedGain);
            
            if (adjustedGain != currentGain) {
                inform(
                    "Session gains for " + itemName + "\n adjusted by §b"
                    + String.format("%+,d", difference)
                );
            }

            refreshDisplays();     
        }
        
        baselineStale.put(itemName, false);
        lastUpdated.put(itemName, System.currentTimeMillis());
    }

    /**
     * Validate and add to session gain.
     */
    protected static void addGain(String itemName, int amount) {

        itemName = normalize(itemName);
        // Convert to base amount first so mapping doesn't skip conversion
        if (CraftingRecipes.known(itemName)) {
            int baseAmount = CraftingRecipes.convertToBase(itemName, amount);
            String baseItemName = CraftingRecipes.getBaseItem(itemName);
            
            if (baseItemName != null && baseAmount > 0) {
                itemName = baseItemName;
                amount = baseAmount;
            }
        }
        
        // Apply learned/special-case mappings after conversion
        String mappedCollection = getMappedCollection(itemName);
        if (mappedCollection != null) itemName = mappedCollection;
        if (!knownBaseDrop(itemName)) return; // Not a collection drop
        
        long current = sessionGains.getOrDefault(itemName, 0L);
        sessionGains.put(itemName, current + amount);
        lastUpdated.put(itemName, System.currentTimeMillis());

        refreshDisplays();
    }

    /** Reset session and update collection totals */
    protected static void resetSession() {
        updateTotal();
        sessionGains.clear();
        lastUpdated.clear();
        pendingDrops.clear();
        save();
        refreshDisplays();
    }

    protected static void refreshDisplays() {
        ActiveDisplay.getInstance().invalidateAll();
        GoalManager.getInstance().clearAll();
    }

    protected static void markAllBaselinesStale() {
        for (String item : baselines.keySet()) {
            baselineStale.put(item, true);
        }
    }

    // --- Data Access ---

    /** Get collection gain rate per hour for an item */
    public static double getGainRatePerHour(String itemName) {
        itemName = normalize(itemName);
        
        long sessionGain = sessionGains.getOrDefault(itemName, 0L);
        if (sessionGain == 0) {
            return 0.0;
        }
        
        long elapsedMs = CollectionTracker.getTimeElapsedMs();
        if (elapsedMs <= 0) {
            return 0.0;
        }
        
        double elapsedHours = elapsedMs / 3600000.0;
        return sessionGain / elapsedHours;
    }

    /** Get session duration in milliseconds */
    public static long getSessionDuration() {
        return CollectionTracker.getTimeElapsedMs();
    }

    /** Get current estimated collection (baseline + session gain) */
    public static long getCurrentCollection(String itemName) {
        long baseline = baselines.getOrDefault(itemName, 0L);
        long gain = sessionGains.getOrDefault(itemName, 0L);
        return baseline + gain;
    }

    /** Get session gain */
    public static long getSessionGain(String itemName) {
        return sessionGains.getOrDefault(itemName, 0L);
    }

    /** Get last update timestamp for this item */
    public static long getLastUpdated(String itemName) {
        return lastUpdated.getOrDefault(itemName, 0L);
    }

    /** Check if we have baseline data for an item */
    public static boolean hasBaseline(String itemName) {
        return baselines.containsKey(normalize(itemName));
    }

    /** Check if there are any pending drops at all */
    public static boolean hasAnyPending() {
        return !pendingDrops.isEmpty();
    }

    /**
     * Return all pending drop names, excluding those mapped to hidden collections
     */
    public static java.util.List<String> getActivePendingRecipes() {
        return pendingDrops.keySet().stream()
                .filter(name -> {
                    String mapped = getMappedCollection(name);
                    return mapped == null || !VisibilityManager.getInstance().isHidden(mapped);
                })
                .toList();
    }

    /**
     * Get all tracked items, sorted by:
     * 1. Actively tracked items (with session gains) first, sorted alphabetically
     * 2. Then items with no session gains, sorted alphabetically
     */
    public static Map<String, Long> getAllTracked() {
        Map<String, Long> result = new HashMap<>();
        
        for (String item : baselines.keySet()) {
            result.put(item, getCurrentCollection(item));
        }
        for (String item : sessionGains.keySet()) {
            result.computeIfAbsent(item, k -> getCurrentCollection(item));
        }
        
        return result.entrySet()
                     .stream()
                     .sorted((e1, e2) -> {
                         long gain1 = sessionGains.getOrDefault(e1.getKey(), 0L);
                         long gain2 = sessionGains.getOrDefault(e2.getKey(), 0L);
                         
                         if (gain1 > 0 && gain2 == 0) {
                             return -1; // e1 active, e2 not active
                         } else if (gain1 == 0 && gain2 > 0) {
                             return 1; // e2 active, e1 not active
                         } else {
                             return e1.getKey().compareTo(e2.getKey());
                         }
                     })
                     .collect(Collectors.toMap(
                         Map.Entry::getKey,
                         Map.Entry::getValue,
                         (e1, e2) -> e1,
                         LinkedHashMap::new
                     ));
    }

    /**
     * Get only actively tracked items, sorted by gain amount descending.
     */
    public static Map<String, Long> getActiveTracked() {
        Map<String, Long> result = new HashMap<>();
        
        for (String item : sessionGains.keySet()) {
            long gain = sessionGains.getOrDefault(item, 0L);
            if (gain > 0) {
                result.put(item, getCurrentCollection(item));
            }
        }

        return result.entrySet()
                     .stream()
                     .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                     .collect(Collectors.toMap(
                         Map.Entry::getKey,
                         Map.Entry::getValue,
                         (e1, e2) -> e1,
                         LinkedHashMap::new
                     ));
    }

    protected static String normalize(String name) {
        if (name == null) return "";

        var n = name.toLowerCase();
        if (n.contains("mushroom")) {
            n = normalizeMushroom(n);
        } else if (n.contains("gemstone")) {
            n = normalizeGemstone(n);
        }
        
        return n.replaceAll("\\s+", " ")
                .replaceAll("[^a-z0-9 ]", "") 
                .trim();
    }

    private static String normalizeMushroom(String name) {
        return name.replaceAll("(?i)red|brown", "").replaceAll("\\s+", " ");
    }

    private static String normalizeGemstone(String name) {
        var matcher = Pattern.compile("(?i)(rough|flawed|fine)\\s+([a-z]+)\\s+gemstone").matcher(name);
        if (matcher.find()) {
            var type = matcher.group(1) + " gemstone";
            return type.replaceFirst("rough", "").trim();
        } else return name;
    }

    /** Update baselines with session gains on session reset and shutdown */
    protected static void updateTotal() {
        for (String item : sessionGains.keySet()) {
            long gain = sessionGains.getOrDefault(item, 0L);
            if (gain > 0) {
                long baseline = baselines.getOrDefault(item, 0L);
                baselines.put(item, baseline + gain);
            }
        }
    }

    // --- Persistence ---

    protected static void save() {
        try {
            DATA_FILE.getParentFile().mkdirs();
            
            SaveData data = new SaveData();
            data.baselines = new HashMap<>(baselines);
            data.sessionGains = new HashMap<>(sessionGains);
            data.lastUpdated = new HashMap<>(lastUpdated);
            data.pendingDrops = new HashMap<>(pendingDrops);
            data.baseDropConversions = new HashMap<>(baseDropConversions);
            data.baselineStale = new HashMap<>(baselineStale);
            
            try (FileWriter writer = new FileWriter(DATA_FILE)) {
                GSON.toJson(data, writer);
            }

        } catch (Exception e) {
            System.err.println("[CollectionTracker] Failed to save data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    protected static void load() {
        if (!DATA_FILE.exists()) return;
        
        try (FileReader reader = new FileReader(DATA_FILE)) {
            @Nullable SaveData data = GSON.fromJson(reader, SaveData.class);
            if (data != null) {
                baselines.clear();
                sessionGains.clear();
                lastUpdated.clear();
                pendingDrops.clear();
                baseDropConversions.clear();
                baselineStale.clear();
                
                if (data.baselines != null) {
                    baselines.putAll(data.baselines);
                }
                if (data.sessionGains != null) {
                    sessionGains.putAll(data.sessionGains);
                }
                if (data.lastUpdated != null) {
                    lastUpdated.putAll(data.lastUpdated);
                }
                if (data.pendingDrops != null) {
                    pendingDrops.putAll(data.pendingDrops);
                }
                if (data.baseDropConversions != null) {
                    baseDropConversions.putAll(data.baseDropConversions);
                }
                if (data.baselineStale != null) {
                    baselineStale.putAll(data.baselineStale);
                } else {
                    for (String item : baselines.keySet()) {
                        baselineStale.put(item, true);
                    }
                }

            }
        } catch (Exception e) {
            System.err.println("[CollectionTracker] Failed to load data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    protected static void clear() {
        baselines.clear();
        sessionGains.clear();
        lastUpdated.clear();
        pendingDrops.clear();
        baseDropConversions.clear();
        baselineStale.clear();
    }

    private static boolean isBaselineStale(String itemName) {
        return baselineStale.getOrDefault(itemName, true);
    }

    private static boolean isLargeBaselineMismatch(long oldBaseline, long newCollectionAmount, long difference) {
        long reference = Math.max(oldBaseline, newCollectionAmount);
        long percentThreshold = Math.round(reference * BASELINE_DIFF_PERCENT_THRESHOLD);
        long threshold = Math.max(BASELINE_DIFF_ABS_THRESHOLD, percentThreshold);
        return Math.abs(difference) > threshold;
    }

    private static void inform(String msg) {
        UIFeedback.getInstance().set(msg, 400, null, null, null);
    }

    private static class SaveData {
        Map<String, Long> baselines;
        Map<String, Long> sessionGains;
        Map<String, Long> lastUpdated;
        Map<String, Integer> pendingDrops;
        Map<String, String> baseDropConversions;
        Map<String, Boolean> baselineStale;
    }
}
