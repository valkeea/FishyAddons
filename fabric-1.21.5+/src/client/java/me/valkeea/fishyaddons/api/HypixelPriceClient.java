package me.valkeea.fishyaddons.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import me.valkeea.fishyaddons.cache.ApiCache;
import net.minecraft.text.Text;

public class HypixelPriceClient {
    private static final String BZ_API_URL = "https://api.hypixel.net/v2/skyblock/bazaar";
    private static final String AUCTIONS_API_URL = "https://api.hypixel.net/v2/skyblock/auctions";
    private static final int BZ_CACHE_EXPIRY_MINUTES = 60;
    private static final String SELL_PRICE = "sellPrice";
    private static final String BUY_PRICE = "buyPrice";

    private final HttpClient httpClient;
    private final Gson gson;
    private final Map<String, PriceData> bazaarCache;
    private final Map<String, PriceData> auctionCache;
    private final ScheduledExecutorService scheduler;
    private volatile long lastBazaarUpdate = 0;
    private volatile long lastAuctionUpdate = 0;
    private volatile long sessionStartTime = System.currentTimeMillis();
    private static String type = SELL_PRICE;

    public HypixelPriceClient() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.gson = new Gson();
        this.bazaarCache = new ConcurrentHashMap<>();
        this.auctionCache = new ConcurrentHashMap<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "HypixelPriceClient-Scheduler");
            t.setDaemon(true);
            return t;
        });
        
        // Schedule periodic cache refresh - only for bazaar (1 hour), auctions are session-cached and on-demand
        this.scheduler.scheduleAtFixedRate(this::refreshBazaarAsync, 0, BZ_CACHE_EXPIRY_MINUTES, TimeUnit.MINUTES);
    }
    
    public static void setPriceType(String newType) {
        if (newType == null || newType.isEmpty()) {
            throw new IllegalArgumentException("Price type cannot be null or empty");
        }

        if (!newType.equals(SELL_PRICE) && !newType.equals(BUY_PRICE)) {
            throw new IllegalArgumentException("Invalid price type: " + newType);
        }

        // Clear cache when price type changes
        if (!newType.equals(type)) {
            ApiCache.clearAllCaches();
        }

        type = newType;
        me.valkeea.fishyaddons.config.FishyConfig.setString(me.valkeea.fishyaddons.config.Key.PRICE_TYPE, newType);
    }

    public static String getType() {
        return type;
    }

    // Check cache, bazaar, ah or fallback, in that order
    public double getBestPrice(String itemName) {
        String apiId = convertToApiId(itemName);
        Double cachedPrice = ApiCache.getCachedPrice(apiId, ApiCache.PriceType.BEST_PRICE);
        if (cachedPrice != null) {
            return cachedPrice;
        }

        if (isTieredDrop(itemName)) {
            double tieredPrice = getTieredPrice(itemName);
            if (tieredPrice > 0) {
                ApiCache.cachePrice(apiId, ApiCache.PriceType.BEST_PRICE, tieredPrice);
                return tieredPrice;
            }
        }
        
        PriceData bazaarPrice = bazaarCache.get(apiId);
        double bestPrice;
        if (bazaarPrice != null && !bazaarPrice.isExpired(BZ_CACHE_EXPIRY_MINUTES)) {
            bestPrice = bazaarPrice.buyPrice;
        } else {
            bestPrice = getLowestBinPrice(itemName);
        }
        ApiCache.cachePrice(apiId, ApiCache.PriceType.BEST_PRICE, bestPrice);
        return bestPrice;
    }
    
    // Get bazaar price based on current price type setting
    public double getBazaarBuyPrice(String itemName) {
        String apiId = convertToApiId(itemName);
        
        Double cachedPrice = ApiCache.getCachedPrice(apiId, ApiCache.PriceType.BZ_BUY);
        if (cachedPrice != null) {
            return cachedPrice;
        }
        
        PriceData priceData = bazaarCache.get(apiId);
        double price = 0.0;
        
        if (priceData != null && !priceData.isExpired(BZ_CACHE_EXPIRY_MINUTES)) {
            price = priceData.getPrice(type);
        }
        
        ApiCache.cachePrice(apiId, ApiCache.PriceType.BZ_BUY, price);
        return price;
    }
    
    // On-demand lookup for auction BIN prices
    public double getLowestBinPrice(String itemName) {
        String apiId = convertToApiId(itemName);
        Double cachedPrice = ApiCache.getCachedPrice(apiId, ApiCache.PriceType.AH_BIN);
        if (cachedPrice != null) {
            return cachedPrice;
        }
        
        String cleanedItemName = convertDisplayNameToApiId(itemName);
        PriceData priceData = auctionCache.get(cleanedItemName);
        double price;
        
        if (priceData != null) {
            price = priceData.buyPrice;
        } else {
            price = searchAuctionForItem(cleanedItemName);
            auctionCache.put(cleanedItemName, new PriceData(price, price, System.currentTimeMillis()));
        }
        
        ApiCache.cachePrice(apiId, ApiCache.PriceType.AH_BIN, price);
        return price;
    }
    
    /**
     * Check if an item name represents a tiered drop (rarity + item name format)
     */
    private boolean isTieredDrop(String itemName) {
        if (itemName == null) return false;
        String lower = itemName.toLowerCase().trim();
        
        return lower.startsWith("common ") || 
               lower.startsWith("uncommon ") || 
               lower.startsWith("rare ") || 
               lower.startsWith("epic ") || 
               lower.startsWith("legendary ") || 
               lower.startsWith("mythic ");
    }

    public double getTieredPrice(String tieredDropName) {
        String[] parts = getNameAndRarity(tieredDropName);
        if (parts.length != 2) return 0.0;

        String itemName = parts[0];
        String rarity = parts[1];
        String cacheKey = rarity.toLowerCase() + "_" + itemName.toLowerCase().replace(" ", "_");
        Double cachedPrice = ApiCache.getCachedPrice(cacheKey, ApiCache.PriceType.AH_BIN);
        if (cachedPrice != null) {
            return cachedPrice;
        }

        double price = searchForTiered(itemName, rarity);
        ApiCache.cachePrice(cacheKey, ApiCache.PriceType.AH_BIN, price);
        return price;
    }

    private String[] getNameAndRarity(String tieredDropName) {
        if (tieredDropName == null) return new String[0];

        String lower = tieredDropName.toLowerCase().trim();
        String[] rarities = {"mythic", "legendary", "epic", "rare", "uncommon", "common"};
        
        for (String rarity : rarities) {
            if (lower.startsWith(rarity + " ")) {
                String itemName = tieredDropName.substring(rarity.length() + 1).trim();
                return new String[]{itemName, rarity.toUpperCase()};
            }
        }
        
        return new String[0];
    }

    private double searchForTiered(String itemName, String rarity) {
        try {
            int maxPagesToSearch = 15;
            double lowestPrice = 0.0;
            int foundCount = 0;
            int emptyPageCount = 0;
            
            for (int page = 0; page < maxPagesToSearch; page++) {
                String url = AUCTIONS_API_URL + "?page=" + page;
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "FishyAddons/2.0.6")
                    .GET()
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() != 200) {
                    System.err.println("Failed to fetch auction page " + page + " for pet: HTTP " + response.statusCode());
                    continue;
                }
                
                double pageLowestPrice = parseForTiered(response.body(), itemName, rarity);
                if (pageLowestPrice > 0) {
                    foundCount++;
                    emptyPageCount = 0;
                    if (lowestPrice == 0 || pageLowestPrice < lowestPrice) {
                        lowestPrice = pageLowestPrice;
                    }
                    
                    if (foundCount >= 3 && page >= 3) {
                        break;
                    }
                } else {
                    emptyPageCount++;
                    if (foundCount > 0 && emptyPageCount >= 2) {
                        break;
                    }
                }              
                if (!handleSleep()) {
                    break;
                }
            }
            
            return lowestPrice;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Tiered auction search interrupted for " + itemName + ": " + e.getMessage());
            return 0.0;
        } catch (Exception e) {
            System.err.println("Error during tiered auction search for " + itemName + ": " + e.getMessage());
            return 0.0;
        }
    }

    private double parseForTiered(String responseBody, String targetItemName, String targetRarity) {
        try {
            JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
            if (!root.has(SUCCESS_KEY) || !root.get(SUCCESS_KEY).getAsBoolean()) {
                return 0.0;
            }
            
            JsonArray auctions = root.getAsJsonArray("auctions");
            double lowestPrice = 0.0;
            
            for (JsonElement auctionElement : auctions) {
                JsonObject auction = auctionElement.getAsJsonObject();

                if (!auction.has("bin") || !auction.get("bin").getAsBoolean()) {
                    continue;
                }

                if (isMatch(auction, targetItemName, targetRarity)) {
                    double price = auction.get("starting_bid").getAsDouble();
                    if (lowestPrice == 0 || price < lowestPrice) {
                        lowestPrice = price;
                    }
                }
            }
            
            return lowestPrice;
            
        } catch (Exception e) {
            System.err.println("Error parsing auction page for item " + targetItemName + ": " + e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * Check if an auction matches the target pet name and rarity
     */
    private boolean isMatch(JsonObject auction, String targetPetName, String targetRarity) {
        try {
            if (!auction.has("item_name")) return false;
            String itemName = auction.get("item_name").getAsString();
            String cleanedName = itemName.replaceAll("§[0-9a-fk-or]", "")
                                         .replaceAll("\\[Lvl \\d+\\]\\s*", "")
                                         .trim();
            
            if (!cleanedName.equalsIgnoreCase(targetPetName)) {
                return false;
            }
            
            if (!auction.has("tier")) return false;
            String auctionRarity = auction.get("tier").getAsString();
            
            return auctionRarity.equalsIgnoreCase(targetRarity);
            
        } catch (Exception e) {
            return false;
        }
    }
    
    public boolean hasBazaarData(String itemName) {
        String apiId = convertToApiId(itemName);
        PriceData priceData = bazaarCache.get(apiId);
        return priceData != null && !priceData.isExpired(BZ_CACHE_EXPIRY_MINUTES);
    }
    
    public boolean hasAuctionData(String itemName) {
        String cleanedItemName = convertDisplayNameToApiId(itemName);
        PriceData priceData = auctionCache.get(cleanedItemName);
        return priceData != null;
    }
    
    public boolean hasPriceData(String itemName) {
        return hasBazaarData(itemName) || hasAuctionData(itemName);
    }
    
    public PriceSource getPriceSource(String itemName) {
        if (hasBazaarData(itemName)) {
            return PriceSource.BAZAAR;
        } else if (hasAuctionData(itemName)) {
            return PriceSource.AUCTION;
        } else {
            return PriceSource.NONE;
        }
    }
    
    public long getLastBazaarUpdate() {
        return lastBazaarUpdate;
    }
    
    public long getLastAuctionUpdate() {
        return lastAuctionUpdate;
    }
    
    public void refreshBazaarAsync() {
        if (System.currentTimeMillis() - lastBazaarUpdate < TimeUnit.MINUTES.toMillis(1)) {
            me.valkeea.fishyaddons.util.FishyNotis.alert(
                Text.literal("§8§oBazaar cache was refreshed recently. Try again in a minute."));
            return;
        }
        CompletableFuture.runAsync(this::refreshBazaar);
    }
    
    public void refreshAuctionsAsync() {
        if (System.currentTimeMillis() - lastAuctionUpdate < TimeUnit.MINUTES.toMillis(1)) {
            me.valkeea.fishyaddons.util.FishyNotis.alert(
                Text.literal("§8§oAuction cache was refreshed recently, skipping..."));
            return;
        }
        if (auctionCache.isEmpty()) {
            return;
        }
        
        CompletableFuture.runAsync(() -> {
            String[] cachedItems = auctionCache.keySet().toArray(new String[0]);
            
            for (String itemName : cachedItems) {
                try {
                    double newPrice = searchAuctionForItem(itemName);
                    auctionCache.put(itemName, new PriceData(newPrice, newPrice, System.currentTimeMillis()));
                    
                } catch (Exception e) {
                    System.err.println("Error refreshing price for " + itemName + ": " + e.getMessage());
                }
            }
        });

        me.valkeea.fishyaddons.util.FishyNotis.alert(
            Text.literal("§7Auction cache refreshed successfully!"));
    }
    
    public void refreshAllAsync() {
        refreshBazaarAsync();
        refreshAuctionsAsync();
    }

    private void refreshBazaar() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BZ_API_URL))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", "FishyAddons")
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                parseBazaarData(response.body());
                lastBazaarUpdate = System.currentTimeMillis();
            } else {
                System.err.println("Failed to fetch bazaar data: HTTP " + response.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Bazaar data fetch interrupted: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error fetching bazaar data: " + e.getMessage());
        }
    }
    
    private void parseBazaarData(String responseBody) {
        try {
            JsonObject root = gson.fromJson(responseBody, JsonObject.class);
            
            if (!root.get(SUCCESS_KEY).getAsBoolean()) {
                System.err.println("Bazaar API returned success=false");
                return;
            }
            
            JsonObject products = root.getAsJsonObject("products");
            long now = System.currentTimeMillis();
            
            for (Map.Entry<String, JsonElement> entry : products.entrySet()) {
                String productId = entry.getKey();
                JsonObject product = entry.getValue().getAsJsonObject();
                
                JsonObject quickStatus = product.getAsJsonObject("quick_status");
                if (quickStatus != null) {
                    double buyPrice = quickStatus.has(BUY_PRICE) ? quickStatus.get(BUY_PRICE).getAsDouble() : 0.0;
                    double sellPrice = quickStatus.has(SELL_PRICE) ? quickStatus.get(SELL_PRICE).getAsDouble() : 0.0;

                    PriceData priceData = new PriceData(buyPrice, sellPrice, now);
                    bazaarCache.put(productId, priceData);
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing bazaar data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private String convertToApiId(String itemName) {
        String cachedApiId = ApiCache.getCachedApiId(itemName);
        if (cachedApiId != null) {
            return cachedApiId;
        }
        
        String normalized = itemName.trim();
        String apiId;
        
        // SHARD_<MOB> for all shards
        if (normalized.toLowerCase().contains("shard") && !normalized.toLowerCase().contains("shard of the shredded") &&
            !normalized.toLowerCase().contains("prismarine shard")) {
            apiId = convertShardToApiId(normalized);
        }
        else {
            // Exp bottles etc.
            String directMapping = getDirectMapping(normalized.toLowerCase());
            if (directMapping != null) {
                apiId = directMapping;
            }
            else if (isEnchantment(normalized)) {
                apiId = convertEnchantmentToApiId(normalized);
            }
            else {
                // Generic ids
                apiId = normalized.replaceAll("\\s+", "_").toUpperCase();
            }
        }
        ApiCache.cacheApiId(itemName, apiId);
        return apiId;
    }
    
    private String convertShardToApiId(String shardName) {
        String mobName = shardName.toLowerCase()
            .replace(" shard", "")
            .replace(" shards", "")
            .replaceAll("\\s+", "_")
            .toUpperCase();
        
        // Special cases
        String shardApiId = "SHARD_" + mobName;
        switch (shardApiId) {
            case "SHARD_LOCH_EMPEROR":
                return "SHARD_SEA_EMPEROR";
            default:
                return shardApiId;
        }
    }
    
    private boolean isEnchantment(String itemName) {
        String lower = itemName.toLowerCase();
        if (lower.matches(".*\\b([1-9]|10)\\b.*")) {
            return true;
        }
        
        // Support 1-10 and Roman numerals
        boolean hasValidLevel = lower.matches(".*\\b([1-9]|10)\\b.*");
        boolean hasEnchantWords = lower.contains("book") || lower.contains("ultimate");
        
        return hasValidLevel && hasEnchantWords;
    }
    
    /**
     * Convert enchantment name to API ID utilizing passed color information for type
     */
    private String convertEnchantmentToApiId(String enchantName) {
        return "ENCHANTMENT_" + enchantName.toUpperCase().replaceAll("\\s+", "_");
    }

    public double getEnchantmentPrice(String enchantmentName) {
        String apiId = convertEnchantmentToApiId(enchantmentName);
        Double cachedPrice = ApiCache.getCachedPrice(apiId, ApiCache.PriceType.BZ_BUY);
        if (cachedPrice != null) {
            return cachedPrice;
        }
        
        PriceData priceData = bazaarCache.get(apiId);
        double price = 0.0;
        
        if (priceData != null && !priceData.isExpired(BZ_CACHE_EXPIRY_MINUTES)) {
            price = priceData.buyPrice;
            if (price < 2) {
                price = bazaarCache.getOrDefault(
                    "ENCHANTMENT_ULTIMATE_" + enchantmentName.toUpperCase().replaceAll("\\s+", "_") + "_I",
                    new PriceData(0,0,0)).buyPrice;
            }
        }
        
        ApiCache.cachePrice(apiId, ApiCache.PriceType.BZ_BUY, price);
        return price;
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static final String SUCCESS_KEY = "success";
    
    /**
     * Price data container
     */
    private static class PriceData {
        final double buyPrice;
        final double sellPrice;
        final long timestamp;

        PriceData(double buyPrice, double sellPrice, long timestamp) {
            this.buyPrice = buyPrice;
            this.sellPrice = sellPrice;
            this.timestamp = timestamp;
        }

        boolean isExpired(int expiryMinutes) {
            return (System.currentTimeMillis() - timestamp) > (expiryMinutes * 60000L);
        }
        
        double getPrice(String priceType) {
            return SELL_PRICE.equals(priceType) ? sellPrice : buyPrice;
        }
    }

    /**
     * Search the auction API for a specific item on-demand.
     * This is called when an item is not found in the pre-cached auction data (on new drop).
     */
    private double searchAuctionForItem(String cleanedItemName) {
        try {
            int maxPagesToSearch = 25; 
            double lowestPrice = 0.0;
            int foundCount = 0;
            int emptyPageCount = 0;
            
            for (int page = 0; page < maxPagesToSearch; page++) {
                String url = AUCTIONS_API_URL + "?page=" + page;
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "FishyAddons/2.0.3")
                    .GET()
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() != 200) {
                    System.err.println("Failed to fetch auction page " + page + ": HTTP " + response.statusCode());
                    continue;
                }
                // Parse the response body for auction data
                double pageLowestPrice = parseAuctionPageForItem(response.body(), cleanedItemName);
                if (pageLowestPrice > 0) {
                    foundCount++;
                    emptyPageCount = 0;
                    if (lowestPrice == 0 || pageLowestPrice < lowestPrice) {
                        lowestPrice = pageLowestPrice;
                    }
                    
                    // If found several instances and a good sample, stop
                    if (foundCount >= 5 && page >= 5) {
                        break;
                    }
                } else {
                    emptyPageCount++;
                    if (foundCount > 0 && emptyPageCount >= 3) {
                        break;
                    }
                }

                if (!handleSleep()) {
                    break;
                }
            }
            
            return lowestPrice;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Auction search interrupted for " + cleanedItemName + ": " + e.getMessage());
            return 0.0;
        } catch (Exception e) {
            System.err.println("Error during on-demand auction search for " + cleanedItemName + ": " + e.getMessage());
            return 0.0;
        }
    }

    /**
     * Parse a single auction page response to find the lowest BIN price for a specific item.
     * Returns 0 if not found or if the API call was unsuccessful.
     */
    private double parseAuctionPageForItem(String responseBody, String targetItemName) {
        try {
            JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
            if (!root.has(SUCCESS_KEY) || !root.get(SUCCESS_KEY).getAsBoolean()) {
                return 0.0;
            }
            
            JsonArray auctions = root.getAsJsonArray("auctions");
            double lowestPrice = 0.0;
            for (JsonElement auctionElement : auctions) {
                JsonObject auction = auctionElement.getAsJsonObject();

                if (!auction.has("bin") || !auction.get("bin").getAsBoolean()) {
                    continue;
                }
                
                String itemName = auction.get("item_name").getAsString();
                String cleanedName = convertDisplayNameToApiId(itemName);
                if (cleanedName.equalsIgnoreCase(targetItemName)) {
                    double price = auction.get("starting_bid").getAsDouble();
                    if (lowestPrice == 0 || price < lowestPrice) {
                        lowestPrice = price;
                    }
                }
            }
            
            return lowestPrice;
            
        } catch (Exception e) {
            System.err.println("Error parsing auction page for " + targetItemName + ": " + e.getMessage());
            return 0.0;
        }
    }

    private boolean handleSleep() {
        try {
            Thread.sleep(20);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }    

    public enum PriceSource {
        BAZAAR, AUCTION, NONE
    }
    
    public void clearAuctionCache() {
        auctionCache.clear();
        sessionStartTime = System.currentTimeMillis();
    }
    
    public long getSessionStartTime() {
        return sessionStartTime;
    }
    
    public void setSessionStartTime(long timestamp) {
        this.sessionStartTime = timestamp;
    }
    
    public double getCachedAuctionPrice(String itemName) {
        String cleanedItemName = convertDisplayNameToApiId(itemName);
        PriceData priceData = auctionCache.get(cleanedItemName);
        return priceData != null ? priceData.buyPrice : 0;
    }
    
    // Auction names are just cleaned up display names
    private String convertDisplayNameToApiId(String displayName) {
        String cleaned = displayName.replaceAll("§[0-9a-fk-or]", "");
        if (cleaned.matches("\\[Lvl \\d+\\].*")) {
            cleaned = cleaned.replaceAll("\\[Lvl \\d+\\]\\s*", "");
        }
        
        // Support most reforges - not comprehensive but this isnt really needed for a drop tracker
        String[] reforgePrefixes = {
            "Sharp", "Heroic", "Spicy", "Legendary", "Fabled", "Withered", "Ancient",
            "Necrotic", "Pleasant", "Precise", "Spiritual", "Headstrong", "Clean",
            "Fierce", "Heavy", "Light", "Mythic", "Pure", "Smart", "Titanic",
            "Wise", "Perfect", "Refined", "Blessed", "Fruitful", "Magnetic",
            "Fleet", "Stellar", "Heated", "Ambered",
            "Keen", "Strong", "Festive", "Submerged", "Mossy"
        };
        
        for (String prefix : reforgePrefixes) {
            if (cleaned.startsWith(prefix + " ")) {
                cleaned = cleaned.substring(prefix.length() + 1);
                break;
            }
        }
        
        // Remove stars and other suffixes
        cleaned = cleaned.replaceAll("[✪➤◆⚚]+", "").trim();
        cleaned = cleaned.replaceAll("\\s*\\+\\d+\\s*$", "").trim();
        cleaned = cleaned.replaceAll("\\s*\\([^)]+\\)\\s*$", "").trim();
        cleaned = cleaned.replaceAll("\\s*\\([^)]*\\d+[^)]*\\)\\s*$", "").trim();
        
        return cleaned;
    }

    // Direct for special cases
    private String getDirectMapping(String itemName) {
        switch (itemName) {
            case "agathas coupon":
                return "AGATHA_COUPON";
            case "experience bottle":
                return "EXPERIENCE_BOTTLE";
            case "grand experience bottle":
                return "GRAND_EXP_BOTTLE";
            case "titanic experience bottle":
                return "TITANIC_EXP_BOTTLE";
            case "colossal experience bottle":
                return "COLOSSAL_EXP_BOTTLE";
            case "lily pad":
                return "WATER_LILY";
            case "enchanted lily pad":
                return "ENCHANTED_WATER_LILY";
            case "raw salmon":
                return "RAW_FISH:1";
            case "clownfish":
                return "RAW_FISH:2";
            case "pufferfish":
                return "RAW_FISH:3";
            case "ink sac":
                return "INK_SACK";
            case "emperors skull":
                return "DIVER_FRAGMENT";
            case "thunder fragment":
                return "THUNDER_SHARDS";
            case "hay bale":
                return "HAY_BLOCK";
            case "enchanted hay bale":
                return "ENCHANTED_HAY_BLOCK";
            case "lucky clover core":
                return "PET_ITEM_LUCKY_CLOVER_DROP";
            case "duplex i":
                return "ENCHANTMENT_ULTIMATE_REITERATE_1";
            case "magmafish":
                return "MAGMA_FISH";
            case "silver magmafish":
                return "MAGMA_FISH_SILVER";
            case "gold magmafish":
                return "MAGMA_FISH_GOLD";
            case "diamond magmafish":
                return "MAGMA_FISH_DIAMOND";
            case "coins":
                return null;
            default:
                return null;
        }
    }
}