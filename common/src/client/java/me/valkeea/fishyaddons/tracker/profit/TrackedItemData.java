package me.valkeea.fishyaddons.tracker.profit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jetbrains.annotations.Nullable;

import me.valkeea.fishyaddons.api.hypixel.PriceServiceManager;
import net.minecraft.text.Text;

public class TrackedItemData {
    private static final Map<String, Integer> itemCounts = new ConcurrentHashMap<>();
    private static final List<String> seenUUIDs = new CopyOnWriteArrayList<>();
    
    // Pending drops from chat, waiting for inventory confirmation
    private static final List<PendingDrop> pendingChatDrops = new CopyOnWriteArrayList<>();
    private static final long PENDING_DROP_WINDOW = 2000;
    
    private static class PendingDrop {
        final String itemName;
        final long timestamp;
        int quantity;

        PendingDrop(String itemName, int quantity) {
            this.itemName = itemName;
            this.quantity = quantity;
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > PENDING_DROP_WINDOW;
        }

        void updateQuantity(int newQuantity) {
            this.quantity = newQuantity;
        }
    }

    private static long sessionStartTime = System.currentTimeMillis();
    private static long lastActivityTime = System.currentTimeMillis();
    private static long totalPausedTime = 0;

    private static final long INACTIVITY_THRESHOLD = 3L * 60 * 1000;
    private static double sessionCoins = 0.0;
    
    /**
     * Register a drop detected from chat. This creates a pending drop, which
     * will be matched with inventory tracking to avoid duplicates.
     * 
     * @param itemName The normalized item name
     * @param quantity The quantity dropped
     * @param isBook Whether this is a book drop (noti styling)
     */
    public static void registerPendingDrop(String itemName, int quantity) {
        if (itemName == null || itemName.trim().isEmpty()) return;
        String normalizedName = normalizeItemName(itemName);
        pendingChatDrops.add(new PendingDrop(normalizedName, quantity));
        cleanupExpiredPendingDrops();
    }
    
    private static void cleanupExpiredPendingDrops() {
        pendingChatDrops.removeIf(PendingDrop::isExpired);
    }

    /** Overload for non-book chat or sack drops */
    public static void addDrop(String itemName, int quantity) {
        addDrop(itemName, quantity, null, null);
    }

    /** Overload for book chat drops */
    public static void addDrop(String itemName, int quantity, @Nullable Text originalMessage) {
        addDrop(itemName, quantity, originalMessage, null);
    }
    
    /**
     * Add a drop to tracking.
     * 
     * @param itemName The item name
     * @param quantity The quantity
     * @param originalMessage Original chat message (for book rarity detection)
     * @param uuid The UUID of the item from inventory (null for chat-only drops)
     * @return DropResult indicating if already counted and if notification should be sent
     */
    public static DropResult addDrop(String itemName, int quantity, @Nullable Text originalMessage, @Nullable String uuid) {
        if (itemName == null || itemName.trim().isEmpty()) return new DropResult(false, false);

        long currentTime = System.currentTimeMillis();
        long timeSinceLastActivity = currentTime - lastActivityTime;
        if (timeSinceLastActivity > INACTIVITY_THRESHOLD) {
            totalPausedTime += timeSinceLastActivity;
        }

        lastActivityTime = currentTime;

        String normalizedName;

        if (originalMessage != null) {
            boolean isUltimate = extractBookRarity(itemName, originalMessage);
            normalizedName = isUltimate ? "ultimate_" + itemName : itemName;
        } else {
            normalizedName = normalizeItemName(itemName);
        }
        
        if (uuid != null && seenUUIDs.contains(uuid)) {
            return new DropResult(true, false);
        }

        return potentialNewDrop(normalizedName, quantity, uuid);
    }

    private static DropResult potentialNewDrop(String normalizedName, int quantity, @Nullable String uuid) {

        PendingDrop matching = null;
        for (var pending : pendingChatDrops) {
            if (pending.itemName.equals(normalizedName) && !pending.isExpired()) {
                matching = pending;
                break;
            }
        }
        
        boolean wasPending = matching != null;
        
        if (wasPending) {
            if (quantity < matching.quantity) {
                matching.updateQuantity(matching.quantity - quantity);
            } else {
                pendingChatDrops.remove(matching);
            }
        }
        
        if (uuid != null) seenUUIDs.add(uuid);
        
        itemCounts.merge(normalizedName, quantity, (oldVal, newVal) -> oldVal + newVal);
        return new DropResult(false, !wasPending);
    }

    /**
     * @param itemName The raw item name
     * @return The price, or estimated value if not found
     */
    public static double getPrice(String itemName) {
        if (itemName == null || itemName.trim().isEmpty()) {
            return 0.0;
        }
        
        String normalizedName = normalizeItemName(itemName);
        
        double price = 0.0;
        var priceService = PriceServiceManager.getInstanceOrNull();
        if (priceService != null) {
            price = priceService.getPrice(normalizedName);
        }
        
        if (price <= 0) {
            price = getEstimatedValue(normalizedName);
        }
        
        return price;
    }    
    
    /**
     * Get prices for all tracked items.
     */
    public static Map<String, Double> getPrices() {
        
        var itemNames = getAllItems().keySet();
        Map<String, Double> results = new HashMap<>();
        
        if (itemNames == null || itemNames.isEmpty()) {
            return results;
        }
        
        List<String> normalizedNames = new ArrayList<>();
        for (String itemName : itemNames) {
            normalizedNames.add(normalizeItemName(itemName));
        }
        
        var priceService = PriceServiceManager.getInstanceOrNull();
        if (priceService != null) {

            var fetchedPrices = priceService.getPrices(normalizedNames);
            for (var entry : fetchedPrices.entrySet()) {
                double price = entry.getValue();
                if (price <= 0) price = getEstimatedValue(entry.getKey());
                if (price > 0) results.put(entry.getKey(), price);
            }
        }
        
        return results;
    }

    private static double getEstimatedValue(String itemName) {
        if (itemName.contains("shard")) return 1000.0;

        // Npc prices
        switch (itemName.toLowerCase()) {
            case "enchanted book":
                return 1.0;
            case "ender chestplate", "ender leggings", "ender boots", "ender helmet":
                return 10000.0;
            case "crown of greed":
                return 1000000.0;
            default:
                return 2.0;
        }
    }

    public static String normalizeItemName(String itemName) {
        return itemName.trim()
                .replaceAll("§[0-9a-fk-or]", "")
                .replaceAll("\\s+", " ")
                .toLowerCase();
    }      
    
    /**
     * Result of adding a drop
     */
    public static class DropResult {
        public final boolean alreadyCounted;
        public final boolean shouldNotify;
        
        public DropResult(boolean alreadyCounted, boolean shouldNotify) {
            this.alreadyCounted = alreadyCounted;
            this.shouldNotify = shouldNotify;
        }
    }

    public static boolean extractBookRarity(String itemName, Text styled) {
        for (Text sibling : styled.getSiblings()) {
            String siblingText = sibling.getString().toLowerCase().trim();
            if (siblingText.contains(itemName.toLowerCase().trim())) {
                String style = sibling.getStyle().toString();
                if (style.contains("light_purple") || style.contains("magenta")) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isItemTracked(String itemName) {
        if (itemName == null || itemName.trim().isEmpty()) return false;
        String normalizedName = normalizeItemName(itemName);
        return itemCounts.containsKey(normalizedName);
    }
    
    public static void addCoins(double coinAmount) {
        sessionCoins += coinAmount;
    }
    
    public static double getSessionCoins() {
        return sessionCoins;
    }
    
    public static Map<String, Integer> getAllItems() {
        return new HashMap<>(itemCounts);
    }
    
    public static void clearAll() {
        itemCounts.clear();
        sessionCoins = 0.0;
        sessionStartTime = System.currentTimeMillis();
        lastActivityTime = System.currentTimeMillis();
        totalPausedTime = 0;
        pendingChatDrops.clear();
        seenUUIDs.clear();
    }
    
    public static long getSessionStartTime() {
        return sessionStartTime;
    }

    public static long getTotalDurationMinutes() {

        long currentTime = System.currentTimeMillis();
        long totalElapsed = currentTime - sessionStartTime;
        long currentPausedTime = totalPausedTime;
        long timeSinceLastActivity = currentTime - lastActivityTime;

        if (timeSinceLastActivity > INACTIVITY_THRESHOLD) {
            currentPausedTime += timeSinceLastActivity;
        }
        
        long activeTime = totalElapsed - currentPausedTime;
        return Math.max(0, activeTime) / (60 * 1000);
    }
    
    public static boolean isCurrentlyPaused() {
        long timeSinceLastActivity = System.currentTimeMillis() - lastActivityTime;
        return timeSinceLastActivity > INACTIVITY_THRESHOLD;
    }
    
    public static long getInactiveMinutes() {
        if (!isCurrentlyPaused()) return 0;
        long timeSinceLastActivity = System.currentTimeMillis() - lastActivityTime;
        return timeSinceLastActivity / (60 * 1000);
    }
    
    public static boolean hasData() {
        return !itemCounts.isEmpty();
    }
    
    /** Get the total number of unique items tracked */
    public static int getTotalItemCount() {
        return itemCounts.values().stream().mapToInt(Integer::intValue).sum();
    }

    /** Get the quantity of a specific item */
    public static int getItemCount(String itemName) {
        return itemCounts.getOrDefault(normalizeItemName(itemName), 0);
    }
    
    /**
     * Get the total session value including coins and all tracked items.
     */
    public static double getTotalSessionValue() {
        double totalValue = sessionCoins;
        
        for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
            String itemName = entry.getKey();
            int quantity = entry.getValue();
            double unitPrice = getPrice(itemName);
            totalValue += unitPrice * quantity;
        }
        return totalValue;
    }
    
    // --- State accessors for profile management ---
    public static long getLastActivityTime() {
        return lastActivityTime;
    }

    public static long getTotalPausedTime() {
        return totalPausedTime;
    }

    public static void setAllItems(Map<String, Integer> items) {
        itemCounts.clear();
        if (items != null) {
            itemCounts.putAll(items);
        }
    }

    public static void setSessionStartTime(long time) {
        sessionStartTime = time;
    }

    public static void setLastActivityTime(long time) {
        lastActivityTime = time;
    }

    public static void setTotalPausedTime(long time) {
        totalPausedTime = time;
    }

    private TrackedItemData() {
        throw new UnsupportedOperationException("Utility class");
    }
}
