package me.valkeea.fishyaddons.tracker;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class InventoryTracker {
    // Constants
    private static final int STACK_INCREASE_THRESHOLD = 32;
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
    private static final Map<String, String> TRACKED_AXES = new ConcurrentHashMap<>();

    private static final String CLEAN_REGEX = "§[0-9a-fk-or]";
    
    static {
        TRACKED_PLAYER_HEADS.put("emperor's skull", "DIVER_FRAGMENT");
        TRACKED_PLAYER_HEADS.put("magma lord fragment", "MAGMA_LORD_FRAGMENT");
        TRACKED_PLAYER_HEADS.put("soul fragment", "SOUL_FRAGMENT");
        TRACKED_PLAYER_HEADS.put("foraging exp boost", "Foraging Exp Boost");
        TRACKED_PLAYER_HEADS.put("minos relic", "Minos Relic");
        TRACKED_PLAYER_HEADS.put("dwarf turtle shelmet", "Dwarf Turtle Shelmet");
        TRACKED_PLAYER_HEADS.put("antique remedies", "Antique Remedies");
        TRACKED_PLAYER_HEADS.put("crown of greed", "Crown of Greed");
    }

    static {
        TRACKED_GHAST_TEARS.put("great white shark tooth", "GREAT_WHITE_SHARK_TOOTH");
    }
    
    // Track recently detected special item drops from chat
    private static final Map<Long, String> recentTrackedItemDrops = new ConcurrentHashMap<>();
    
    // Track last known stack sizes for stackable skulls to detect increases
    private static final Map<String, Integer> lastKnownStackSizes = new ConcurrentHashMap<>();

    private InventoryTracker() {}
    
    public static void onItemAdded(ItemStack stack) {
        if (stack.isEmpty()) return;
        
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

        if (stack.getItem() == Items.IRON_AXE) {
            handleAxeAdded(stack);
        }
    }
    
    private static boolean isMonitoringActive() {
        if (!monitoringEnabled) return false;
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - monitoringStartTime > MONITORING_WINDOW) {
            monitoringEnabled = false;
            monitoringStartTime = 0;
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
    
    private static void handlePlayerHeadAdded(ItemStack stack) {
        String displayName = stack.getName().getString().toLowerCase().trim();
        String cleanDisplayName = displayName.replaceAll(CLEAN_REGEX, "").trim();
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
            TrackerUtils.trackerNoti(cleanDisplayName);
            ItemTrackerData.addDrop(cleanDisplayName, newItems);
        } else {
            // Stack size didn't increase, just update our tracking
            lastKnownStackSizes.put(cleanDisplayName, currentStackSize);
        }
    }

    private static void handleGhastTearAdded(ItemStack stack) {
        String displayName = stack.getName().getString().toLowerCase().trim();
        String cleanDisplayName = displayName.replaceAll(CLEAN_REGEX, "").trim();
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
            TrackerUtils.trackerNoti(cleanDisplayName);
            ItemTrackerData.addDrop(cleanDisplayName, newItems);
        } else {
            // Stack size didn't increase, just update our tracking
            lastKnownStackSizes.put(cleanDisplayName, currentStackSize);
        }
    }

    private static void handleAxeAdded(ItemStack stack) {
        String displayName = stack.getName().getString().toLowerCase().trim();
        String cleanDisplayName = displayName.replaceAll(CLEAN_REGEX, "").trim();
        String auctionId = TRACKED_AXES.get(cleanDisplayName);
        if (auctionId == null) {
            return;
        }
        TrackerUtils.trackerNoti(cleanDisplayName);
        ItemTrackerData.addDrop(cleanDisplayName, 1);
    }

    /**
     * Cleanup method to be called periodically to reset correlation state
     */
    public static void cleanup() {
        long currentTime = System.currentTimeMillis();

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
        if (!monitoringEnabled) {
            monitoringEnabled = true;
        }
        monitoringStartTime = System.currentTimeMillis();
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
                Thread.sleep(200);
                if (System.currentTimeMillis() - monitoringStartTime >= 200) {
                    monitoringEnabled = false;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
}
