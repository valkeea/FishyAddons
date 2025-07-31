package me.valkeea.fishyaddons.tracker;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.valkeea.fishyaddons.api.HypixelPriceClient;
import me.valkeea.fishyaddons.cache.ApiCache;
import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;


// Session-persistent storage
public class ItemTrackerData {
    private static final Map<String, Integer> itemCounts = new ConcurrentHashMap<>();
    private static final Map<String, Double> itemValues = new ConcurrentHashMap<>();
    private static final String BOOK_DROP_MESSAGE = "BOOK DROP!";
    private static long sessionStartTime = System.currentTimeMillis();
    private static double sessionCoins = 0.0;
    private static HypixelPriceClient priceClient = null;
    private ItemTrackerData() {}
    
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
    
    public static void addDrop(String itemName, int quantity, String tooltipContent) {
        if (itemName == null || itemName.trim().isEmpty()) return;
        String normalizedName = normalizeItemName(itemName);
        itemCounts.merge(normalizedName, quantity, Integer::sum);
        
        // Async price lookup for new items to populate cache
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
            }, "AutoPriceLookup-" + normalizedName.replaceAll("[^a-zA-Z0-9]", "")).start();
        }
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
        ApiCache.cleanupExpiredEntries();
    }
    
    public static long getSessionStartTime() {
        return sessionStartTime;
    }
    
    public static long getSessionDurationMinutes() {
        return (System.currentTimeMillis() - sessionStartTime) / (60 * 1000);
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
        
        return totalValue + sessionCoins;
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
        // Common shard values (rough estimates - can be updated)
        if (itemName.contains("shard")) {
            return 1000.0;
        }

        // Npc prices/ah stack estimates
        switch (itemName.toLowerCase()) {
            case "enchanted book":
                return 5.0;
            case "ender chestplate", "ender leggings", "ender boots", "ender helmet":
                return 10000.0;
            case "lushlilac":
                return 15000.0;
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
        return priceClient != null ? Math.max(priceClient.getLastBazaarUpdate(), priceClient.getLastAuctionUpdate()) : 0;
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
    
    /**
     * Get the price client instance for direct access (used by commands)
     * @return The HypixelPriceClient instance, or null if not initialized
     */
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
        
        // Check if we have cached auction data
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
                double value = getItemValue(itemName); // This will do the async search
                String source = priceClient != null ? priceClient.getPriceSource(itemName).name() : "ESTIMATED";
                callback.onResult(value, source);
            } catch (Exception e) {
                double estimatedValue = getEstimatedValue(normalizedName);
                callback.onResult(estimatedValue, "ESTIMATED");
            }
        }, "AsyncValueLookup-" + itemName.replaceAll("[^a-zA-Z0-9]", "")).start();
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
        // to-do -- 371 and 411 check later if getItemvalue --> getCachedItemValue works
        Map<String, Integer> currentItems = new HashMap<>(itemCounts);
        
        new Thread(() -> {
            System.out.println("Background price update started for " + currentItems.size() + " items");
            
            for (String normalizedItemName : currentItems.keySet()) {
                try {
                    if (!itemValues.containsKey(normalizedItemName)) {
                        double value = getItemValue(normalizedItemName);
                        System.out.println("Updated price for " + normalizedItemName + ": " + value);
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
            
            System.out.println("Background price update completed");
        }, "BackgroundPriceUpdate").start();
    }
    
    /**
     * Path to the JSON file where tracker data is saved
     */
    private static final String TRACKER_FILE_PATH = "config/fishyaddons/tracker/profittracker.json";
    
    public static void saveToJson() {
        try {
            Path filePath = Paths.get(TRACKER_FILE_PATH);
            Files.createDirectories(filePath.getParent());
            
            TrackerData data = new TrackerData();
            data.itemCounts = new HashMap<>(itemCounts);
            data.sessionStartTime = sessionStartTime;
            data.savedAt = System.currentTimeMillis();
            
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(data);
            
            try (FileWriter writer = new FileWriter(filePath.toFile())) {
                writer.write(json);
            }
        } catch (Exception e) {
            System.err.println("Error saving tracker data: " + e.getMessage());
        }
    }
    
    public static boolean loadFromJson() {
        try {
            Path filePath = Paths.get(TRACKER_FILE_PATH);
            if (!Files.exists(filePath)) {
                return false;
            }
            
            String json = Files.readString(filePath);
            Gson gson = new Gson();
            TrackerData data = gson.fromJson(json, TrackerData.class);
            
            if (data != null && data.itemCounts != null) {
                itemCounts.clear();
                itemCounts.putAll(data.itemCounts);
                if (data.sessionStartTime > 0) {
                    sessionStartTime = data.sessionStartTime;
                }
                return true;
            }
        } catch (Exception e) {
            System.err.println("Error loading tracker data: " + e.getMessage());
        }
        return false;
    }
    
    public static boolean deleteJsonFile() {
        try {
            Path filePath = Paths.get(TRACKER_FILE_PATH);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                return true;
            }
        } catch (Exception e) {
            System.err.println("Error deleting tracker data file: " + e.getMessage());
        }
        return false;
    }
    
    public static boolean hasJsonFile() {
        return Files.exists(Paths.get(TRACKER_FILE_PATH));
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
                    try {
                        // Rebuild
                        double price = priceClient.getLowestBinPrice(itemName);
                        if (price > 0) {
                            refreshCount++;
                        }
        
                        Thread.sleep(200);

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        System.err.println("Error refreshing " + itemName + ": " + e.getMessage());
                    }
                }
                
                me.valkeea.fishyaddons.util.FishyNotis.send(
                "Auction cache refresh completed. Updated " + refreshCount + " items.");
            } catch (Exception e) {
                System.err.println("Error during force auction refresh: " + e.getMessage());
            }
        }, "ForceAuctionRefresh").start();
    }
    
    private static String formatCoins(double coins) {
        if (coins >= 1_000_000.0) {
            return String.format("%.1fm", coins / 1_000_000.0);
        } else if (coins >= 1_000.0) {
            return String.format("%.1fk", coins / 1_000.0);
        } else {
            return String.format("%.0f", coins);
        }
    }
    
    /**
     * Data class for JSON serialization
     */
    private static class TrackerData {
        Map<String, Integer> itemCounts;
        long sessionStartTime;
        long savedAt;
    }
    
    /**
     * Enhance item name extraction for better display, especially for enchanted books
     * This version is for when we want to extract the name without modifying the tracker
     */
    public static String enhanceItemName(String rawItemName, String tooltipContent) {
        System.out.println("Enhancing item name: " + rawItemName);
        if (rawItemName == null || rawItemName.trim().isEmpty()) {
            return rawItemName;
        }
        
        String cleanItemName = normalizeItemName(rawItemName);
        
        // Special handling for enchanted books
        if (cleanItemName.equals("enchanted book") && tooltipContent != null) {
            String enhancedName = extractEnchantedBookName(tooltipContent);
            if (enhancedName != null && !enhancedName.trim().isEmpty() && !enhancedName.equals("enchanted book")) {
                return enhancedName;
            }
        }
        
        return rawItemName;
    }
    
    /**
     * Add an enchanted book with automatic enhancement and proper tracking, including magic find for alerts
     */
    public static void addEnchantedBookDrop(String rawItemName, int quantity, String tooltipContent, String magicFind) {
        String enhancedName = enhanceItemName(rawItemName, tooltipContent);
        
        if (enhancedName != null && !enhancedName.equals("enchanted book") && tooltipContent != null) {
            // Check if it's an ultimate enchantment based on color
            boolean isUltimate = tooltipContent.contains("color=light_purple") || tooltipContent.contains("color=magenta");
            
            // Add the specific enchantment with magic find for alert
            addEnchantedBook(enhancedName, quantity, isUltimate, magicFind);
        } else {
            // Fallback to generic enchanted book
            addDrop("enchanted book", quantity);
        }
    }
    
    /**
     * Extract the specific enchantment name from enchanted book tooltip content
     */
    private static String extractEnchantedBookName(String tooltipContent) {
        if (tooltipContent == null || tooltipContent.trim().isEmpty()) {
            return null;
        }
        if (tooltipContent.contains("siblings=[literal{") && tooltipContent.contains("}[style=")) {
            return extractFromMinecraftTextFormat(tooltipContent);
        }
        return extractFromPlainTextFormat(tooltipContent);
    }
    
    /**
     * Extract enchantment name from Minecraft Text object format
     */
    private static String extractFromMinecraftTextFormat(String tooltipContent) {
        String pattern = "siblings=\\[literal\\{([^}]+)\\}\\[style=\\{([^}]*)\\}\\]";
        java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher matcher = regex.matcher(tooltipContent);
        
        if (matcher.find()) {
            String enchantmentName = matcher.group(1);
            String styleInfo = matcher.group(2);
            
            // Check if it's an ultimate enchantment (pink color)
            boolean isUltimate = styleInfo.contains("color=light_purple") || styleInfo.contains("color=magenta");
            return enchantmentName;
        }
        return null;
    }
    
    /**
     * Extract enchantment name from plain text format
     */
    private static String extractFromPlainTextFormat(String tooltipContent) {
        String[] lines = tooltipContent.split("\\n");
        
        for (String line : lines) {
            String cleanLine = line.trim()
                .replaceAll("§[0-9a-fk-or]", "") // Remove color codes
                .trim();
            
            if (shouldSkipLine(cleanLine)) {
                continue;
            }
            
            // Look for lines that contain enchantment information
            if (cleanLine.matches(".*\\b(I|II|III|IV|V|VI|VII|VIII|IX|X)\\b.*") ||
                (cleanLine.length() > 3 && !cleanLine.contains("§") && Character.isUpperCase(cleanLine.charAt(0)))) {
                return cleanLine;
            }
        }
        return null;
    }
    
    /**
     * Check if a line should be skipped during enchantment parsing
     */
    private static boolean shouldSkipLine(String cleanLine) {
        return cleanLine.isEmpty() || 
               cleanLine.startsWith("Right-click") ||
               cleanLine.startsWith("Click to") ||
               cleanLine.startsWith("Combine") ||
               cleanLine.contains("Enchanted Book") ||
               cleanLine.contains("Apply this book") ||
               cleanLine.contains("NBT:");
    }
    
    /**
     * Show book drop alert notification with optional magic find
     */
    private static void showBookDropAlert(String enchantmentName, String magicFind) {

        if (FishyConfig.getState(Key.BOOK_DROP_ALERT, true)) {
            // Create the base message
            net.minecraft.text.MutableText message = Text.literal(BOOK_DROP_MESSAGE).formatted(Formatting.GOLD, Formatting.BOLD);
            message = message.append(Text.literal(" " + enchantmentName).formatted(Formatting.WHITE));

            // Add magic find if available
            if (magicFind != null && !magicFind.trim().isEmpty()) {
                message = message.append(Text.literal(" (+" + magicFind + " ✯ Magic Find)").formatted(Formatting.AQUA));
            }

            message = message.append(Text.literal(" α").formatted(Formatting.AQUA, Formatting.ITALIC));
            me.valkeea.fishyaddons.util.FishyNotis.alert(message);
        }
    }
    
    /**
     * Add an enchanted book with metadata for proper price lookups
     */
    public static void addEnchantedBook(String enchantmentName, int quantity, boolean isUltimate) {
        addEnchantedBook(enchantmentName, quantity, isUltimate, null);
    }
    
    /**
     * Add an enchanted book with metadata for proper price lookups and magic find for alert
     */
    public static void addEnchantedBook(String enchantmentName, int quantity, boolean isUltimate, String magicFind) {
        if (enchantmentName == null || enchantmentName.trim().isEmpty()) return;
        
        // Show book drop alert with magic find if available
        showBookDropAlert(enchantmentName, magicFind);
        
        // Store the enchantment with metadata
        String normalizedName = normalizeItemName(enchantmentName);
        itemCounts.merge(normalizedName, quantity, Integer::sum);
        
        // Async price lookup with enchantment context
        if (priceClient != null && !itemValues.containsKey(normalizedName)) {
            new Thread(() -> {
                try {
                    // Pass enchantment info to price client for proper API ID conversion
                    double price = priceClient.getEnchantmentPrice(enchantmentName, isUltimate);
                    if (price > 0) {
                        itemValues.put(normalizedName, price);
                    }
                } catch (Exception e) {
                    System.err.println("Error fetching enchantment price for " + enchantmentName + ": " + e.getMessage());
                }
            }, "EnchantmentPriceLookup-" + normalizedName.replaceAll("[^a-zA-Z0-9]", "")).start();
        }
    }
    
    /**
     * Remove generic enchanted book entries
     */
    public static void removeGenericEnchantedBook(int quantity) {
        String genericName = normalizeItemName("enchanted book");
        Integer genericCount = itemCounts.get(genericName);
        
        if (genericCount != null && genericCount >= quantity) {
            if (genericCount == quantity) {
                itemCounts.remove(genericName);
            } else {
                itemCounts.put(genericName, genericCount - quantity);
            }
        }
    }
    
    /**
     * Clear the cached item values to force recalculation with new price type
     */
    public static void clearValueCache() {
        itemValues.clear();
    }
}