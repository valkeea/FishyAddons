package me.valkeea.fishyaddons.tracker;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;


public class InventoryTracker {
    // Constants
    private static final int STACK_INCREASE_THRESHOLD = 64; // Increased threshold - drops can be large but purchases are usually full stacks
    
    // Track recently detected "Enchanted Book" drops from chat
    private static final Map<Long, Integer> recentEnchantedBookDrops = new ConcurrentHashMap<>();
    private static final long DROP_CORRELATION_WINDOW = 5000; // 5 seconds to correlate drops
    
    // Max monitoring window in case quick disable is missed
    private static boolean monitoringEnabled = false;
    private static long monitoringStartTime = 0;
    private static final long MONITORING_WINDOW = 60000;
    private static boolean lsEnabled = false; 
    private static long lsStartTime = 0;
    private static final long LS_WINDOW = 10000;
    
    // Track specific items we want to monitor (besides enchanted books)
    private static final Map<String, String> TRACKED_PLAYER_HEADS = new ConcurrentHashMap<>();
    
    static {
        // Initialize tracked player head items (display name -> bazaar ID)
        TRACKED_PLAYER_HEADS.put("emperor's skull", "DIVER_FRAGMENT");
        TRACKED_PLAYER_HEADS.put("great white shark tooth", "GREAT_WHITE_SHARK_TOOTH");
        // Add more player head items here as needed
    }
    
    // Track recently detected special item drops from chat
    private static final Map<Long, String> recentTrackedItemDrops = new ConcurrentHashMap<>();
    
    // Track last known stack sizes for stackable skulls to detect increases
    private static final Map<String, Integer> lastKnownStackSizes = new ConcurrentHashMap<>();

    private InventoryTracker() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Called when an "Enchanted Book" is detected in chat - we'll correlate this with inventory additions
     */
    public static void onEnchantedBookDropDetected(int quantity) {
        long currentTime = System.currentTimeMillis();
        recentEnchantedBookDrops.put(currentTime, quantity);
        
        // Clean up old entries
        recentEnchantedBookDrops.entrySet().removeIf(entry -> 
            (currentTime - entry.getKey()) > DROP_CORRELATION_WINDOW * 2);
        
    }
    
    public static void onItemAdded(ItemStack stack) {
        if (stack.isEmpty()) return;

        if (!recentEnchantedBookDrops.isEmpty()) {
            handleEnchantedBookAdded(stack);
        }
        
        // Only process items if we're in a monitoring window (after entity death)
        if (!isMonitoringActive()) {
            return;
        }
        
        
        if (stack.getItem() == Items.ENCHANTED_BOOK) {
            handleEnchantedBookAdded(stack);
        } else if (stack.getItem() == Items.PLAYER_HEAD) {
            handlePlayerHeadAdded(stack);
        }
    }
    
    private static boolean isMonitoringActive() {
        if (!monitoringEnabled) return false;
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - monitoringStartTime > MONITORING_WINDOW) {
            monitoringEnabled = false;
            return false;
        }
        return true;
    }

    public static boolean isLsMonitoringActive() {
        if (!lsEnabled) return false;
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lsStartTime > LS_WINDOW) {
            lsEnabled = false;
            return false;
        }
        return true;
    }
    
    private static void handleEnchantedBookAdded(ItemStack stack) {
        long currentTime = System.currentTimeMillis();
        
        // Check if this correlates with a recent chat drop
        for (Map.Entry<Long, Integer> entry : recentEnchantedBookDrops.entrySet()) {
            long dropTime = entry.getKey();
            int dropQuantity = entry.getValue();
            
            if (currentTime - dropTime <= 5000L && dropQuantity >= stack.getCount()) {
                // Found correlation - scan inventory to find the actual book name
                recentEnchantedBookDrops.remove(dropTime);
                ItemTrackerData.removeGenericEnchantedBook(stack.getCount());
                return;
            }
        }
    }
    
    private static void handlePlayerHeadAdded(ItemStack stack) {
        String displayName = stack.getName().getString().toLowerCase().trim();
        String cleanDisplayName = displayName.replaceAll("§[0-9a-fk-or]", "").trim();
        String bazaarId = TRACKED_PLAYER_HEADS.get(cleanDisplayName);
        
        if (bazaarId == null) {
            return; // Not a tracked skull
        }
        
        int currentStackSize = stack.getCount();
        int previousStackSize = lastKnownStackSizes.getOrDefault(cleanDisplayName, 0);
        
        if (currentStackSize > previousStackSize) {
            int rawIncrease = currentStackSize - previousStackSize;
            
            // Failsafe: If increase is more than threshold, likely user bought/moved items
            // Count as only 1 to avoid false positives from purchases/trades
            int newItems = (rawIncrease > STACK_INCREASE_THRESHOLD) ? 1 : rawIncrease;
            
            lastKnownStackSizes.put(cleanDisplayName, currentStackSize);
            
            ItemTrackerData.addDrop(cleanDisplayName, newItems);
        } else {
            // Stack size didn't increase, just update our tracking
            lastKnownStackSizes.put(cleanDisplayName, currentStackSize);
        }
    }
    
    /**
     * Cleanup method to be called periodically to reset correlation state
     */
    public static void cleanup() {
        long currentTime = System.currentTimeMillis();
        
        // Clean up old enchanted book drop entries
        recentEnchantedBookDrops.entrySet().removeIf(entry -> 
            (currentTime - entry.getKey()) > DROP_CORRELATION_WINDOW * 3);
        
        // Clean up old tracked item drop entries
        recentTrackedItemDrops.entrySet().removeIf(entry -> 
            (currentTime - entry.getKey()) > DROP_CORRELATION_WINDOW * 3);
        
        // Clean up stack size tracking if it gets too large
        if (lastKnownStackSizes.size() > 63) {
            lastKnownStackSizes.clear();
        }
    }
    
    /**
     * Called when a loot share message is detected - enable monitoring
     */
    public static void onLsDetected() {
        lsEnabled = true;
        lsStartTime = System.currentTimeMillis();
    }
    
    /**
     * Called when any entity enters death animation - enables packet monitoring window
     * This version doesn't filter by entity type since death animation is already a good indicator
     */
    public static void onValuableEntityDamaged() {
        
        // Enable monitoring window immediately when death animation is detected
        monitoringEnabled = true;
        monitoringStartTime = System.currentTimeMillis();
        
        // Don't clear stack tracking here since entity isn't fully dead yet
    }
    
    /**
     * Called when a valuable entity takes damage - enables packet monitoring window
     * Only enable monitoring if the entity is actually valuable
     */
    public static void onValuableEntityDamaged(String displayName) {
        if (displayName == null) return;
        
        // Check if this entity is actually valuable using the same patterns as EntityTracker
        if (!isEntityValuable(displayName)) {
            return; // Not a valuable entity, don't enable monitoring
        }
        
        // Enable monitoring window immediately when damage is detected
        monitoringEnabled = true;
        monitoringStartTime = System.currentTimeMillis();
        
        // Don't clear stack tracking here since entity isn't dead yet
    }
    
    /**
     * Check if an entity is valuable based on its display name
     * Uses the same patterns as EntityTracker
     */
    private static boolean isEntityValuable(String displayName) {
        if (displayName == null) return false;
        
        // Check for valuable mob patterns (same as EntityTracker)
        String nameToCheck = displayName.toLowerCase();
        
        // Check for valuable sea creatures
        if (nameToCheck.matches(".*\\b(sea walker|guardian defender|sea witch|squid|loch emperor|the rider of the deep)\\b.*")) {
            return true;
        }
        
        // Check for valuable player entities (sharks)
        if (nameToCheck.matches(".*\\b(great white shark|tiger shark)\\b.*")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Called when a valuable entity dies - brief monitoring window then quick disable
     * This replaces complex inventory scanning with simple packet detection
     */
    public static void onValuableEntityDeath(String entityName) {
        
        // Enable monitoring window for immediate drops
        monitoringEnabled = true;
        monitoringStartTime = System.currentTimeMillis();
        
        // Schedule a quick disable after 1 second (drops should be immediate)
        Thread.startVirtualThread(() -> {
            try {
                Thread.sleep(1000); // 1 second delay
                if (System.currentTimeMillis() - monitoringStartTime >= 1000) {
                    monitoringEnabled = false;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
}
