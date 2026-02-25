package me.valkeea.fishyaddons.api.hypixel;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;

import net.minecraft.text.Text;

/**
 * Service responsible for fetching and caching item prices.
 */
public class PriceService {
    private static final String BZ_API_URL = "https://api.hypixel.net/v2/skyblock/bazaar";
    private static final String AUCTIONS_API_URL = "https://api.hypixel.net/v2/skyblock/auctions";
    private static final String USER_AGENT = "FA-PriceClient";
    
    private static final long BAZAAR_REFRESH_MINUTES = 120;
    private static final long AUCTION_REFRESH_MINUTES = 120;
    
    private static final String SELL_PRICE = "sellPrice";
    private static final String BUY_PRICE = "buyPrice";
    private static String priceType = SELL_PRICE;

    private final HttpClient httpClient;
    private final Gson gson;
    
    private final BzResponse bazaarParser;
    private final AhResponse auctionParser;
    private final ApiIdCache apiIdCache;
    private final NegativeLookupCache negativeLookupCache;
    
    private final Map<String, PriceData> bazaarCache; // API_ID -> PriceData
    private final Map<String, TieredPriceData> auctionCache; // cleaned_name -> TieredPriceData
    
    private final ScheduledExecutorService scheduler;
    private volatile long lastBazaarUpdate = 0;
    private volatile long lastAuctionUpdate = 0;

    public PriceService() {

        initPriceType();

        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.gson = new Gson();
        
        // Init helper components & caches
        this.bazaarParser = new BzResponse(gson);
        this.auctionParser = new AhResponse(gson);
        this.apiIdCache = new ApiIdCache();
        this.negativeLookupCache = new NegativeLookupCache();
        this.bazaarCache = new ConcurrentHashMap<>();
        this.auctionCache = new ConcurrentHashMap<>();
        
        // Schedule periodic refreshes
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PriceService-Scheduler");
            t.setDaemon(true);
            return t;
        });
        
        long initialDelaySeconds = ThreadLocalRandom.current().nextLong(10, 60);
        
        scheduler.scheduleAtFixedRate(
            this::refreshBazaar, 
            initialDelaySeconds, 
            BAZAAR_REFRESH_MINUTES, 
            TimeUnit.MINUTES
        );
        
        scheduler.scheduleAtFixedRate(
            this::refreshAuctions, 
            initialDelaySeconds + 30,
            AUCTION_REFRESH_MINUTES, 
            TimeUnit.MINUTES
        );
        
        new Thread(this::refreshBazaar, "Initial-Bazaar-Load").start();
        new Thread(this::refreshAuctions, "Initial-Auction-Load").start();
    }
    
    // --- Configuration ---

    private void initPriceType() {
        String configuredType = me.valkeea.fishyaddons.config.FishyConfig.getString(
            me.valkeea.fishyaddons.config.Key.PRICE_TYPE, SELL_PRICE
        );
        setPriceType(configuredType);
    }
    
    public static void setPriceType(String newType) {
        if (newType == null || newType.isEmpty()) {
            throw new IllegalArgumentException("Price type cannot be null or empty");
        }

        if (!newType.equals(SELL_PRICE) && !newType.equals(BUY_PRICE)) {
            throw new IllegalArgumentException("Invalid price type: " + newType);
        }

        priceType = newType;
        me.valkeea.fishyaddons.config.FishyConfig.setString(
            me.valkeea.fishyaddons.config.Key.PRICE_TYPE, newType
        );
    }

    public static String getType() {
        return priceType;
    }

    // -- Price Lookup API ---

    /**
     * Get the price for an item.
     * 
     * @param itemName The normalized item name
     * @return The price for the mapped name, or 0.0 if not found
     */
    public double getPrice(String itemName) {
        if (itemName == null || itemName.trim().isEmpty()) return 0.0;

        var apiId = getApiId(itemName);
        var bazaarData = bazaarCache.get(apiId);
        if (bazaarData != null) return bazaarData.getPrice(priceType);

        if (ItemNameMapper.isTieredDrop(itemName)) return getTieredPrice(itemName);
        
        var cleanedName = ItemNameMapper.cleanDisplayName(itemName);
        var auctionData = auctionCache.get(cleanedName.toLowerCase());
        if (auctionData != null) return auctionData.getLowestPrice();
        
        negativeLookupCache.put(apiId);
        return 0.0;
    }

    /**
     * Batch price lookup for multiple items.
     * 
     * @param itemNames List of item names
     * @return Map of item name to price
     */
    public Map<String, Double> getPrices(java.util.List<String> itemNames) {
        Map<String, Double> results = new HashMap<>();
        
        if (itemNames != null) {
            for (String itemName : itemNames) {
                results.put(itemName, getPrice(itemName));
            }
        }
        
        return results;
    }

    /**
     * Get price for a tiered item
     * @param tieredDropName Full name
     * @return The price for that specific rarity, or 0.0 if not found
     */
    private double getTieredPrice(String tieredDropName) {

        String cacheKey = tieredDropName.toLowerCase().replace(" ", "_");
        if (negativeLookupCache.contains(cacheKey)) return 0.0;
        
        String[] parts = ItemNameMapper.parseNameAndRarity(tieredDropName);
        if (parts.length != 2) {
            negativeLookupCache.put(cacheKey);
            return 0.0;
        }
        
        String baseName = parts[0];
        String rarity = parts[1];
        
        var tieredData = auctionCache.get(baseName.toLowerCase());
        if (tieredData == null) {
            negativeLookupCache.put(cacheKey);
            return 0.0;
        }
        
        double price = tieredData.getPrice(rarity);
        if (price == 0.0) negativeLookupCache.put(cacheKey);
        return price;
    }
    
    // --- Cache state ---
    
    public boolean hasBazaarData(String itemName) {
        String apiId = getApiId(itemName);
        return bazaarCache.containsKey(apiId);
    }
    
    public boolean hasAuctionData(String itemName) {
        String cleanedItemName = ItemNameMapper.cleanDisplayName(itemName);
        return auctionCache.containsKey(cleanedItemName.toLowerCase());
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
    
    public int getBazaarCacheSize() {
        return bazaarCache.size();
    }
    
    public int getAuctionCacheSize() {
        return auctionCache.size();
    }
    
    // --- Data refresh methods ---
    
    private static final int CD_MINUTES = 2;

    /**
     * Manually trigger a bazaar refresh
     */
    public void refreshBazaarAsync() {
        if (System.currentTimeMillis() - lastBazaarUpdate < TimeUnit.MINUTES.toMillis(CD_MINUTES)) {
            long secondsLeft = TimeUnit.MINUTES.toSeconds(CD_MINUTES) - TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - lastBazaarUpdate);
            me.valkeea.fishyaddons.util.FishyNotis.alert(
                Text.literal("§8§oBazaar cache was refreshed recently. Try again in " + secondsLeft + " second(s)."));
            return;
        }
        
        new Thread(this::refreshBazaar, "Manual-Bazaar-Refresh").start();
    }
    
    /**
     * Manually trigger an auction refresh
     */
    public void refreshAuctionsAsync() {
        if (System.currentTimeMillis() - lastAuctionUpdate < TimeUnit.MINUTES.toMillis(CD_MINUTES)) {
            long secondsLeft = TimeUnit.MINUTES.toSeconds(CD_MINUTES) - TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - lastAuctionUpdate);
            me.valkeea.fishyaddons.util.FishyNotis.alert(
                Text.literal("§8§oAuction cache was refreshed recently. Try again in " + secondsLeft + " second(s)."));
            return;
        }
        
        new Thread(this::refreshAuctions, "Manual-Auction-Refresh").start();
    }
    
    public void refreshAllAsync() {
        refreshBazaarAsync();
        refreshAuctionsAsync();
    }

    /**
     * Load entire bazaar data into local cache.
     */
    private void refreshBazaar() {
        try {
            
            var request = HttpRequest.newBuilder()
                .uri(URI.create(BZ_API_URL))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                var parsedData = bazaarParser.parse(response.body());
                bazaarCache.clear();
                bazaarCache.putAll(parsedData);
                lastBazaarUpdate = System.currentTimeMillis();
            } else {
                System.err.println("Failed to fetch bazaar data: HTTP " + response.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Bazaar data fetch interrupted: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error fetching bazaar data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Load ALL BIN auctions into local cache.
     */
    private void refreshAuctions() {
        try {
            
            auctionCache.clear();
            
            var aggregateResult = new AhResponse.ParseResult();
            
            int page = 0;
            int totalPages = 0;
            
            do {
                var url = AUCTIONS_API_URL + "?page=" + page;
                var request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", USER_AGENT)
                    .GET()
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() != 200) {
                    System.err.println("Failed to fetch auction page " + page + ": HTTP " + response.statusCode());
                    break;
                }
                
                AhResponse.ParseResult pageResult = auctionParser.parsePage(response.body());
                aggregateResult.merge(pageResult);
                totalPages = pageResult.getTotalPages();
                
                page++;
                
                Thread.sleep(100);
                
            } while (page < totalPages);
            
            var builtCache = auctionParser.buildTieredCache(aggregateResult.itemPrices);
            auctionCache.putAll(builtCache);
            
            lastAuctionUpdate = System.currentTimeMillis();
                
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Auction refresh interrupted: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error refreshing auctions: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // --- Utils ---
    
    /**
     * Get API ID for an item name, using cache when available.
     */
    private String getApiId(String itemName) {
        String cached = apiIdCache.get(itemName);
        if (cached != null) {
            return cached;
        }
        
        String apiId = ItemNameMapper.toApiId(itemName);
        apiIdCache.put(itemName, apiId);
        return apiId;
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

    public ApiIdCache getApiIdCache() {
        return apiIdCache;
    }

    public NegativeLookupCache getNegativeLookupCache() {
        return negativeLookupCache;
    }
}
