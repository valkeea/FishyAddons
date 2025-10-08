package me.valkeea.fishyaddons.tracker;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import me.valkeea.fishyaddons.api.HypixelPriceClient;
import me.valkeea.fishyaddons.cache.ApiCache;
import net.minecraft.text.Text;

public class ItemTrackerData {
    private static final Map<String, Integer> itemCounts = new ConcurrentHashMap<>();
    private static final Map<String, Double> itemValues = new ConcurrentHashMap<>();

    private static final String ENCHANTED_BOOK = "enchanted book";
    private static final String REGEX = "[^a-zA-Z0-9]";

    private static long sessionStartTime = System.currentTimeMillis();
    private static long lastActivityTime = System.currentTimeMillis();
    private static long totalPausedTime = 0;

    private static final long INACTIVITY_THRESHOLD = 3L * 60 * 1000;
    private static double sessionCoins = 0.0;
    private static HypixelPriceClient priceClient = null;

    public static void init() {
        if (priceClient == null) {
            priceClient = new HypixelPriceClient();
        }
    }

    public static void shutdown() {
        if (priceClient != null) {
            priceClient.shutdown();
            priceClient = null;
        }
    }

    public static void addDrop(String itemName, int quantity) {
        addDrop(itemName, quantity, null);
    }

    public static void addDrop(String itemName, int quantity, @Nullable Text originalMessage) {
        if (itemName == null || itemName.trim().isEmpty()) return;

        // Check if pause threshold has been exceeded
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

        itemCounts.merge(normalizedName, quantity, Integer::sum);

        if (priceClient != null && !itemValues.containsKey(normalizedName)) {
            new Thread(() -> {
                try {
                    double price = priceClient.getBestPrice(normalizedName);
                    if (price > 0) {
                        itemValues.put(normalizedName, price);
                    }
                } catch (Exception e) {
                    System.err.println("Error fetching price for " + normalizedName + ": " + e.getMessage());
                }
            }, "AutoPriceLookup-" + normalizedName.replaceAll(REGEX, "")).start();
        }
    }

    public static boolean extractBookRarity(String itemName, Text originalMessage) {
        if (originalMessage == null || itemName == null || itemName.trim().isEmpty()) {
            return false;
        }

        for (Text sibling : originalMessage.getSiblings()) {
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
        itemValues.clear();
        sessionCoins = 0.0;
        sessionStartTime = System.currentTimeMillis();
        lastActivityTime = System.currentTimeMillis();
        totalPausedTime = 0;
        ApiCache.cleanupExpiredEntries();
    }
    
    public static long getSessionStartTime() {
        return sessionStartTime;
    }

    public static long getTotalDurationMinutes() {
        long currentTime = System.currentTimeMillis();
        long totalElapsed = currentTime - sessionStartTime;
        long currentPausedTime = totalPausedTime;
        // add the current inactive time to paused time for calculation
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
        if (!isCurrentlyPaused()) {
            return 0;
        }
        long timeSinceLastActivity = System.currentTimeMillis() - lastActivityTime;
        return timeSinceLastActivity / (60 * 1000);
    }
    
    private static String normalizeItemName(String itemName) {
        return itemName.trim()
                .replaceAll("§[0-9a-fk-or]", "")
                .replaceAll("\\s+", " ")
                .toLowerCase();
    }
    
    public static boolean hasData() {
        return !itemCounts.isEmpty();
    }
    
    // Get the total number of unique items tracked
    public static int getTotalItemCount() {
        return itemCounts.values().stream().mapToInt(Integer::intValue).sum();
    }

    // Get the count of a specific item
    public static int getItemCount(String itemName) {
        return itemCounts.getOrDefault(normalizeItemName(itemName), 0);
    }    
    
    public static double getTotalSessionValue() {
        double totalValue = sessionCoins;
        
        for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
            String itemName = entry.getKey();
            int quantity = entry.getValue();
            double unitPrice = 0;
    
            Double cachedValue = itemValues.get(itemName);
            if (cachedValue != null) {
                unitPrice = cachedValue;
            } else if (priceClient != null) {
                // Only check price client if no cached value
                if (priceClient.hasBazaarData(itemName)) {
                    unitPrice = priceClient.getBazaarBuyPrice(itemName);
                    itemValues.put(itemName, unitPrice);
                } else if (priceClient.hasAuctionData(itemName)) {
                    unitPrice = priceClient.getCachedAuctionPrice(itemName);
                    itemValues.put(itemName, unitPrice);
                }
            }
            
            // If still no price found, use estimated value and cache it
            if (unitPrice == 0) {
                unitPrice = getEstimatedValue(itemName);
                itemValues.put(itemName, unitPrice);
            }
            
            totalValue += unitPrice * quantity;
        }
        return totalValue;
    }
    
    public static double getCachedItemValue(String itemName) {
        String normalizedName = normalizeItemName(itemName);
        Double cachedValue = itemValues.get(normalizedName);
        if (cachedValue != null) {
            return cachedValue;
        }
        
        // Check price client if not known
        if (priceClient != null) {
            if (priceClient.hasBazaarData(normalizedName)) {
                double price = priceClient.getBazaarBuyPrice(normalizedName);
                if (price > 0) {
                    itemValues.put(normalizedName, price);
                    return price;
                }
            } else if (priceClient.hasAuctionData(normalizedName)) {
                double price = priceClient.getCachedAuctionPrice(normalizedName);
                if (price > 0) {
                    itemValues.put(normalizedName, price);
                    return price;
                }
            }
        }
        // If no cached or API value, use estimated value
        double estimatedValue = getEstimatedValue(normalizedName);
        if (estimatedValue > 0) {
            itemValues.put(normalizedName, estimatedValue);
        }
        
        return estimatedValue;
    }
    
    // Get the estimated value of an item, used when no bazaar or auction data is available
    public static double getItemValue(String itemName) {
        String normalizedName = normalizeItemName(itemName);
        
        // Check if we have a cached value
        Double cachedValue = itemValues.get(normalizedName);
        if (cachedValue != null) {
            return cachedValue;
        }
        
        // Try to fetch from API if client is initialized
        if (priceClient != null) {
            double apiValue = priceClient.getBestPrice(itemName);
            if (apiValue > 0) {
                itemValues.put(normalizedName, apiValue);
                return apiValue;
            }
        }
        
        // Fallback to estimated values for items not on bazaar
        double estimatedValue = getEstimatedValue(normalizedName);
        itemValues.put(normalizedName, estimatedValue);
        return estimatedValue;
    }
    
    private static double getEstimatedValue(String itemName) {
        // Fallback shard value
        if (itemName.contains("shard")) {
            return 1000.0;
        }

        // Npc prices/ah stack estimates
        switch (itemName.toLowerCase()) {
            case ENCHANTED_BOOK:
                return 1.0;
            case "ender chestplate", "ender leggings", "ender boots", "ender helmet":
                return 10000.0;
            case "lushlilac":
                return 15000.0;
            case "crown of greed":
                return 1000000.0;
            default:
                return 2.0;
        }
    }
    
    public static boolean hasApiPriceData(String itemName) {
        return priceClient != null && priceClient.hasPriceData(itemName);
    }
    
    public static String getPriceSource(String itemName) {
        if (priceClient != null) {
            HypixelPriceClient.PriceSource source = priceClient.getPriceSource(itemName);
            return source.name();
        }
        return "NONE";
    }
    
    public static long getLastApiUpdateTime() {
        return priceClient != null ? priceClient.getLastBazaarUpdate() : 0;
    }
    
    public static long getLastBazaarUpdateTime() {
        return priceClient != null ? priceClient.getLastBazaarUpdate() : 0;
    }
    
    public static long getLastAuctionUpdateTime() {
        return priceClient != null ? priceClient.getLastAuctionUpdate() : 0;
    }
    
    /**
     * Force refresh of all prices (bazaar and auctions)
     */
    public static void refreshPrices() {
        if (priceClient != null) {
            priceClient.refreshAllAsync();
        }
    }
    
    /**
     * Force refresh of bazaar prices only
     */
    public static void refreshBazaarPrices() {
        if (priceClient != null) {
            priceClient.refreshBazaarAsync();
        }
    }

    // Returns the price client instance for direct access (used by commands)
    public static HypixelPriceClient getPriceClient() {
        return priceClient;
    }
    
    /**
     * Get the estimated value of a specific item asynchronously
     * @param itemName The item name
     * @param callback Callback to receive the result (value, source)
     */
    public static void getItemValueAsync(String itemName, ItemValueCallback callback) {
        String normalizedName = normalizeItemName(itemName);
        
        // Check if we have a cached value first
        Double cachedValue = itemValues.get(normalizedName);
        if (cachedValue != null) {
            String source = priceClient != null ? priceClient.getPriceSource(itemName).name() : "CACHED";
            callback.onResult(cachedValue, source);
            return;
        }
        
        // Check if we can get instant bazaar data
        if (priceClient != null && priceClient.hasBazaarData(itemName)) {
            double bazaarPrice = priceClient.getBazaarBuyPrice(itemName);
            if (bazaarPrice > 0) {
                itemValues.put(normalizedName, bazaarPrice);
                callback.onResult(bazaarPrice, "BAZAAR");
                return;
            }
        }
        
        if (priceClient != null && priceClient.hasAuctionData(itemName)) {
            double auctionPrice = priceClient.getLowestBinPrice(itemName);
            if (auctionPrice > 0) {
                itemValues.put(normalizedName, auctionPrice);
                callback.onResult(auctionPrice, "AUCTION");
                return;
            }
        }
        
        // If no cached data, do async lookup
        new Thread(() -> {
            try {
                double value = getItemValue(itemName);
                String source = priceClient != null ? priceClient.getPriceSource(itemName).name() : "ESTIMATED";
                callback.onResult(value, source);
            } catch (Exception e) {
                double estimatedValue = getEstimatedValue(normalizedName);
                callback.onResult(estimatedValue, "ESTIMATED");
            }
        }, "AsyncValueLookup-" + itemName.replaceAll(REGEX, "")).start();
    }
    
    /**
     * Callback interface for async item value lookup
     */
    public interface ItemValueCallback {
        void onResult(double value, String source);
    }
    
    public static void updateAllAsync() {
        if (priceClient == null) {
            return;
        }

        Map<String, Integer> currentItems = new HashMap<>(itemCounts);
        
        new Thread(() -> {
            for (String normalizedItemName : currentItems.keySet()) {
                try {
                    if (!itemValues.containsKey(normalizedItemName)) {
                        getItemValue(normalizedItemName);
                        Thread.sleep(100);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Background price update interrupted");
                    break;
                } catch (Exception e) {
                    System.err.println("Error updating price for " + normalizedItemName + ": " + e.getMessage());
                }
            }
        }, "BackgroundPriceUpdate").start();
    }
    
    // Force refresh from the hud buttons
    public static void forceRefreshAuctionCache() {
        if (priceClient == null) return;
        
        new Thread(() -> {
            try {
                priceClient.clearAuctionCache();
                Map<String, Integer> items = new HashMap<>(itemCounts);
                int refreshCount = 0;
                
                for (String itemName : items.keySet()) {
                    if (!refreshAuctionItem(itemName)) {
                        break;
                    } else {
                        refreshCount++;
                    }
                }
                
                me.valkeea.fishyaddons.util.FishyNotis.alert(Text.literal(
                "§7Auction cache refresh completed. Updated " + refreshCount + " items."));
            } catch (Exception e) {
                System.err.println("Error during force auction refresh: " + e.getMessage());
            }
        }, "ForceAuctionRefresh").start();
    }
    
    private static boolean refreshAuctionItem(String itemName) {
        try {
            // Rebuild
            double price = priceClient.getLowestBinPrice(itemName);
            if (price > 0) {
                // price updated, nothing else needed here
            }

            Thread.sleep(200);

            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            System.err.println("Error refreshing " + itemName + ": " + e.getMessage());
            return true;
        }
    }
    
    /**
     * Clear the cached item values to force recalculation with new price type
     */
    public static void clearValueCache() {
        itemValues.clear();
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

    private ItemTrackerData() {
        throw new UnsupportedOperationException("Utility class");
    }
}