package me.valkeea.fishyaddons.tracker;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;


public class InventoryTracker {
    // Constants
    private static final int STACK_INCREASE_THRESHOLD = 32;
    
    // Track recently detected "Enchanted Book" drops from chat
    private static final Map<Long, EnchantedBookDrop> recentEnchantedBookDrops = new ConcurrentHashMap<>();
    private static final long DROP_CORRELATION_WINDOW = 5000; // 5 seconds to correlate drops
    
    // Max monitoring window in case quick disable is missed
    private static boolean monitoringEnabled = false;
    private static long monitoringStartTime = 0;
    private static final long MONITORING_WINDOW = 60000;
    private static boolean lsEnabled = false; 
    private static long lsStartTime = 0;
    private static final long LS_WINDOW = 10000;
    
    private static final Map<String, String> TRACKED_PLAYER_HEADS = new ConcurrentHashMap<>();
    private static final Map<String, String> TRACKED_GHAST_TEARS = new ConcurrentHashMap<>();
    
    /**
     * Data class to store enchanted book drop information
     */
    private static class EnchantedBookDrop {
        final int quantity;
        final String magicFind;
        
        EnchantedBookDrop(int quantity, String magicFind) {
            this.quantity = quantity;
            this.magicFind = magicFind;
        }
    }
    
    static {
        TRACKED_PLAYER_HEADS.put("emperor's skull", "DIVER_FRAGMENT");
        TRACKED_PLAYER_HEADS.put("magma lord fragment", "MAGMA_LORD_FRAGMENT");
    }

    static {
        TRACKED_GHAST_TEARS.put("great white shark tooth", "GREAT_WHITE_SHARK_TOOTH");
    }
    
    // Track recently detected special item drops from chat
    private static final Map<Long, String> recentTrackedItemDrops = new ConcurrentHashMap<>();
    
    // Track last known stack sizes for stackable skulls to detect increases
    private static final Map<String, Integer> lastKnownStackSizes = new ConcurrentHashMap<>();

    private InventoryTracker() {}
    
    /**
     * Called when an "Enchanted Book" is detected in chat
     */
    public static void onEnchantedBookDropDetected(int quantity, String magicFind) {

        long currentTime = System.currentTimeMillis();
        recentEnchantedBookDrops.put(currentTime, new EnchantedBookDrop(quantity, magicFind));
        // Clean up old entries
        recentEnchantedBookDrops.entrySet().removeIf(entry -> 
            (currentTime - entry.getKey()) > DROP_CORRELATION_WINDOW * 2);
        
    }
    
    public static void onItemAdded(ItemStack stack) {
        if (stack.isEmpty()) return;

        // Process enchanted books if we have recent drops to correlate
        if (!recentEnchantedBookDrops.isEmpty() && stack.getItem() == Items.ENCHANTED_BOOK) {
            handleEnchantedBookAdded(stack);
            return; // Exit early to prevent double processing
        }
        
        // Only process items if we're in a monitoring window (after entity death)
        if (!isMonitoringActive()) {
            return;
        }
        
        // Handle other tracked items during monitoring window
        if (stack.getItem() == Items.PLAYER_HEAD) {
            handlePlayerHeadAdded(stack);
        }

        if (stack.getItem() == Items.GHAST_TEAR) {
            handleGhastTearAdded(stack);
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
        for (Map.Entry<Long, EnchantedBookDrop> entry : recentEnchantedBookDrops.entrySet()) {
            long dropTime = entry.getKey();
            EnchantedBookDrop drop = entry.getValue();
            
            if (currentTime - dropTime <= 5000L && drop.quantity >= stack.getCount()) {
                // Found correlation - extract enchantment info from lore
                LoreComponent lore = stack.get(DataComponentTypes.LORE);
                String tooltipContent = lore != null ? lore.toString() : null;
                ItemTrackerData.addEnchantedBookDrop("enchanted book", stack.getCount(), tooltipContent, drop.magicFind);
                
                // Remove the processed drop from tracking
                recentEnchantedBookDrops.remove(dropTime);
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

    private static void handleGhastTearAdded(ItemStack stack) {
        String displayName = stack.getName().getString().toLowerCase().trim();
        String cleanDisplayName = displayName.replaceAll("§[0-9a-fk-or]", "").trim();
        String bazaarId = TRACKED_GHAST_TEARS.get(cleanDisplayName);
        
        if (bazaarId == null) {
            return; // Not a tracked ghast tear
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
        for (Map.Entry<Long, EnchantedBookDrop> entry : recentEnchantedBookDrops.entrySet()) {
            long dropTime = entry.getKey();
            EnchantedBookDrop drop = entry.getValue();
            if (currentTime - dropTime > DROP_CORRELATION_WINDOW) {
                ItemTrackerData.addDrop("enchanted book", drop.quantity);
            }
        }
        
        recentEnchantedBookDrops.entrySet().removeIf(entry -> 
            (currentTime - entry.getKey()) > DROP_CORRELATION_WINDOW * 3);
        
        recentTrackedItemDrops.entrySet().removeIf(entry -> 
            (currentTime - entry.getKey()) > DROP_CORRELATION_WINDOW * 3);

        if (lastKnownStackSizes.size() > 63) {
            lastKnownStackSizes.clear();
        }
    }
    
    /**
     * Called when a loot share message is detected
     */
    public static void onLsDetected() {
        lsEnabled = true;
        lsStartTime = System.currentTimeMillis();
    }
    
    /**
     * Called when any entity enters death animation - enables packet monitoring window
     */
    public static void onValuableEntityDamaged() {
        monitoringEnabled = true;
        monitoringStartTime = System.currentTimeMillis();
    }
    
    /**
     * Check if an entity is valuable based on its display name
     * Uses the same patterns as EntityTracker
     */
    private static boolean isEntityValuable(String displayName) {
        if (displayName == null) return false;
        String nameToCheck = displayName.toLowerCase();
        
        // Check for valuable sea creatures
        if (nameToCheck.matches(".*\\b(the loch emperor|lord jawbus|thunder)\\b.*")) {
            return true;
        }
        
        // Check for valuable player entities
        if (nameToCheck.matches(".*\\b(great white shark|tiger shark|ent)\\b.*")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Called when a valuable entity dies - brief monitoring window then quick disable
     */
    public static void onValuableEntityDeath() {
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
