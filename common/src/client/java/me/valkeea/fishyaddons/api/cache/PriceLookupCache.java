package me.valkeea.fishyaddons.api.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Provides caching for API ID conversions, message parsing results, and price lookups.
 */
public class PriceLookupCache {
    private static final ConcurrentHashMap<String, String> apiIdCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, Object> messageParseCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> normalizationCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, CachedPrice> priceCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, CachedEntry> auctionCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, NegativeCacheEntry> negativeLookupCache = new ConcurrentHashMap<>();
    
    // Cache hit/miss statistics
    private static final AtomicLong apiIdCacheHits = new AtomicLong(0);
    private static final AtomicLong apiIdCacheMisses = new AtomicLong(0);
    private static final AtomicLong messageCacheHits = new AtomicLong(0);
    private static final AtomicLong messageCacheMisses = new AtomicLong(0);
    private static final AtomicLong normalizationCacheHits = new AtomicLong(0);
    private static final AtomicLong normalizationCacheMisses = new AtomicLong(0);
    private static final AtomicLong priceCacheHits = new AtomicLong(0);
    private static final AtomicLong priceCacheMisses = new AtomicLong(0);
    private static final AtomicLong auctionCacheHits = new AtomicLong(0);
    private static final AtomicLong auctionCacheMisses = new AtomicLong(0);
    private static final AtomicLong negativeCacheHits = new AtomicLong(0);
    
    // Config
    private static final int MAX_CACHE_SIZE = 10000;
    private static final long PRICE_CACHE_TTL_MS = 60L * 60 * 1000;  // 60 minutes instead of 5
    private static final long AUCTION_CACHE_TTL_MS = 120L * 60 * 1000;  // 2 hours
    private static final long NEGATIVE_CACHE_TTL_MS = 60L * 60 * 1000;  // 1 hour for failed lookups
    
    private PriceLookupCache() {} 
    
    public static String getCachedApiId(String itemName) {
        String cached = apiIdCache.get(itemName);
        if (cached != null) {
            apiIdCacheHits.incrementAndGet();
            return cached;
        }
        apiIdCacheMisses.incrementAndGet();
        return null;
    }
    
    public static void cacheApiId(String itemName, String apiId) {
        if (apiIdCache.size() < MAX_CACHE_SIZE) {
            apiIdCache.put(itemName, apiId);
        }
    }
    
    public static Object getCachedMessageParse(String message) {
        int messageHash = message.hashCode();
        Object cached = messageParseCache.get(messageHash);
        if (cached != null) {
            messageCacheHits.incrementAndGet();
            return cached;
        }
        messageCacheMisses.incrementAndGet();
        return null;
    }
    
    public static void cacheMessageParse(String message, Object parseResult) {
        if (messageParseCache.size() < MAX_CACHE_SIZE) {
            int messageHash = message.hashCode();
            // Use a special marker for null results to avoid repeated parsing
            Object cacheValue = parseResult != null ? parseResult : NULL_PARSE_RESULT;
            messageParseCache.put(messageHash, cacheValue);
        }
    }
    
    private static final Object NULL_PARSE_RESULT = new Object();
    
    public static boolean isNullParseResult(Object cachedResult) {
        return cachedResult == NULL_PARSE_RESULT;
    }
    
    public static String getCachedNormalization(String rawName) {
        String cached = normalizationCache.get(rawName);
        if (cached != null) {
            normalizationCacheHits.incrementAndGet();
            return cached;
        }
        normalizationCacheMisses.incrementAndGet();
        return null;
    }
    
    public static void cacheNormalization(String rawName, String normalizedName) {
        if (normalizationCache.size() < MAX_CACHE_SIZE) {
            normalizationCache.put(rawName, normalizedName);
        }
    }

    public static Double getCachedPrice(String apiId, PriceType priceType) {
        String cacheKey = apiId + ":" + priceType.name();
        CachedPrice cached = priceCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            priceCacheHits.incrementAndGet();
            return cached.price;
        }
        if (cached != null && cached.isExpired()) {
            priceCache.remove(cacheKey);
        }
        priceCacheMisses.incrementAndGet();
        return null;
    }
    
    public static void cachePrice(String apiId, PriceType priceType, double price) {
        if (priceCache.size() < MAX_CACHE_SIZE) {
            String cacheKey = apiId + ":" + priceType.name();
            priceCache.put(cacheKey, new CachedPrice(price, System.currentTimeMillis()));
        }
    }
    
    public static void clearAllCaches() {
        apiIdCache.clear();
        messageParseCache.clear();
        normalizationCache.clear();
        priceCache.clear();
        auctionCache.clear();
        negativeLookupCache.clear();
        resetStatistics();
    }
    
    // Specific cache clearing methods
    public static void clearPriceCache() {
        priceCache.clear();
        priceCacheHits.set(0);
        priceCacheMisses.set(0);
    }
    
    public static void clearAuctionCache() {
        auctionCache.clear();
        auctionCacheHits.set(0);
        auctionCacheMisses.set(0);
    }
    
    public static void clearNegativeCache() {
        negativeLookupCache.clear();
        negativeCacheHits.set(0);
    }
    
    // Auction cache methods with TTL support
    public static Double getCachedAuctionPrice(String apiId) {
        CachedEntry cached = auctionCache.get(apiId);
        if (cached != null && !cached.isExpired(AUCTION_CACHE_TTL_MS)) {
            auctionCacheHits.incrementAndGet();
            return cached.price;
        }
        if (cached != null && cached.isExpired(AUCTION_CACHE_TTL_MS)) {
            auctionCache.remove(apiId);
        }
        auctionCacheMisses.incrementAndGet();
        return null;
    }
    
    public static void cacheAuctionPrice(String apiId, double price) {
        if (auctionCache.size() < MAX_CACHE_SIZE) {
            auctionCache.put(apiId, new CachedEntry(price, System.currentTimeMillis()));
        }
    }
    
    // Negative cache methods - remember failed lookups to avoid repeated searches
    public static boolean isNegativelyCached(String apiId) {
        NegativeCacheEntry entry = negativeLookupCache.get(apiId);
        if (entry != null && !entry.isExpired(NEGATIVE_CACHE_TTL_MS)) {
            negativeCacheHits.incrementAndGet();
            return true;
        }
        if (entry != null && entry.isExpired(NEGATIVE_CACHE_TTL_MS)) {
            negativeLookupCache.remove(apiId);
        }
        return false;
    }
    
    public static void cacheNegativeLookup(String apiId) {
        if (negativeLookupCache.size() < MAX_CACHE_SIZE) {
            negativeLookupCache.put(apiId, new NegativeCacheEntry(System.currentTimeMillis()));
        }
    }
    
    public static void cleanupExpiredEntries() {

        priceCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        auctionCache.entrySet().removeIf(entry -> entry.getValue().isExpired(AUCTION_CACHE_TTL_MS));
        negativeLookupCache.entrySet().removeIf(entry -> entry.getValue().isExpired(NEGATIVE_CACHE_TTL_MS));
        
        if (messageParseCache.size() > MAX_CACHE_SIZE * 0.7) {
            messageParseCache.clear();
        }
    }
    
    public static void resetStatistics() {
        apiIdCacheHits.set(0);
        apiIdCacheMisses.set(0);
        messageCacheHits.set(0);
        messageCacheMisses.set(0);
        normalizationCacheHits.set(0);
        normalizationCacheMisses.set(0);
        priceCacheHits.set(0);
        priceCacheMisses.set(0);
        auctionCacheHits.set(0);
        auctionCacheMisses.set(0);
        negativeCacheHits.set(0);
    }
    
    public static CacheStatistics getStatistics() {
        return CacheStatistics.builder()
            .withApiIdCache(apiIdCache.size(), apiIdCacheHits.get(), apiIdCacheMisses.get())
            .withMessageCache(messageParseCache.size(), messageCacheHits.get(), messageCacheMisses.get())
            .withNormalizationCache(normalizationCache.size(), normalizationCacheHits.get(), normalizationCacheMisses.get())
            .withPriceCache(priceCache.size(), priceCacheHits.get(), priceCacheMisses.get())
            .withAuctionCache(auctionCache.size(), auctionCacheHits.get(), auctionCacheMisses.get())
            .withNegativeCache(negativeLookupCache.size(), negativeCacheHits.get())
            .build();
    }
    
    public enum PriceType {
        BEST_PRICE,
        BZ_BUY,
        BZ_SELL,
        AH_BIN
    }
    
    private static class CachedPrice {
        final double price;
        final long timestamp;
        
        CachedPrice(double price, long timestamp) {
            this.price = price;
            this.timestamp = timestamp;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > PRICE_CACHE_TTL_MS;
        }
    }
    
    // Statistics class to hold cache metrics
    public static class CacheStatistics {
        public final int apiIdCacheSize;
        public final long apiIdCacheHits;
        public final long apiIdCacheMisses;
        
        public final int messageCacheSize;
        public final long messageCacheHits;
        public final long messageCacheMisses;
        
        public final int normalizationCacheSize;
        public final long normalizationCacheHits;
        public final long normalizationCacheMisses;
        
        public final int priceCacheSize;
        public final long priceCacheHits;
        public final long priceCacheMisses;
        
        public final int auctionCacheSize;
        public final long auctionCacheHits;
        public final long auctionCacheMisses;
        
        public final int negativeCacheSize;
        public final long negativeCacheHits;
        
        private CacheStatistics(Builder builder) {
            this.apiIdCacheSize = builder.apiIdCacheSize;
            this.apiIdCacheHits = builder.apiIdCacheHits;
            this.apiIdCacheMisses = builder.apiIdCacheMisses;
            this.messageCacheSize = builder.messageCacheSize;
            this.messageCacheHits = builder.messageCacheHits;
            this.messageCacheMisses = builder.messageCacheMisses;
            this.normalizationCacheSize = builder.normalizationCacheSize;
            this.normalizationCacheHits = builder.normalizationCacheHits;
            this.normalizationCacheMisses = builder.normalizationCacheMisses;
            this.priceCacheSize = builder.priceCacheSize;
            this.priceCacheHits = builder.priceCacheHits;
            this.priceCacheMisses = builder.priceCacheMisses;
            this.auctionCacheSize = builder.auctionCacheSize;
            this.auctionCacheHits = builder.auctionCacheHits;
            this.auctionCacheMisses = builder.auctionCacheMisses;
            this.negativeCacheSize = builder.negativeCacheSize;
            this.negativeCacheHits = builder.negativeCacheHits;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public double getApiIdHitRate() {
            long total = apiIdCacheHits + apiIdCacheMisses;
            return total > 0 ? (double) apiIdCacheHits / total : 0.0;
        }
        
        public double getMessageHitRate() {
            long total = messageCacheHits + messageCacheMisses;
            return total > 0 ? (double) messageCacheHits / total : 0.0;
        }
        
        public double getNormalizationHitRate() {
            long total = normalizationCacheHits + normalizationCacheMisses;
            return total > 0 ? (double) normalizationCacheHits / total : 0.0;
        }
        
        public double getPriceHitRate() {
            long total = priceCacheHits + priceCacheMisses;
            return total > 0 ? (double) priceCacheHits / total : 0.0;
        }
        
        public double getAuctionHitRate() {
            long total = auctionCacheHits + auctionCacheMisses;
            return total > 0 ? (double) auctionCacheHits / total : 0.0;
        }
        
        /**
         * Builder for CacheStatistics.
         */
        public static class Builder {
            private int apiIdCacheSize;
            private long apiIdCacheHits;
            private long apiIdCacheMisses;
            private int messageCacheSize;
            private long messageCacheHits;
            private long messageCacheMisses;
            private int normalizationCacheSize;
            private long normalizationCacheHits;
            private long normalizationCacheMisses;
            private int priceCacheSize;
            private long priceCacheHits;
            private long priceCacheMisses;
            private int auctionCacheSize;
            private long auctionCacheHits;
            private long auctionCacheMisses;
            private int negativeCacheSize;
            private long negativeCacheHits;
            
            public Builder withApiIdCache(int size, long hits, long misses) {
                this.apiIdCacheSize = size;
                this.apiIdCacheHits = hits;
                this.apiIdCacheMisses = misses;
                return this;
            }
            
            public Builder withMessageCache(int size, long hits, long misses) {
                this.messageCacheSize = size;
                this.messageCacheHits = hits;
                this.messageCacheMisses = misses;
                return this;
            }
            
            public Builder withNormalizationCache(int size, long hits, long misses) {
                this.normalizationCacheSize = size;
                this.normalizationCacheHits = hits;
                this.normalizationCacheMisses = misses;
                return this;
            }
            
            public Builder withPriceCache(int size, long hits, long misses) {
                this.priceCacheSize = size;
                this.priceCacheHits = hits;
                this.priceCacheMisses = misses;
                return this;
            }
            
            public Builder withAuctionCache(int size, long hits, long misses) {
                this.auctionCacheSize = size;
                this.auctionCacheHits = hits;
                this.auctionCacheMisses = misses;
                return this;
            }
            
            public Builder withNegativeCache(int size, long hits) {
                this.negativeCacheSize = size;
                this.negativeCacheHits = hits;
                return this;
            }
            
            public CacheStatistics build() {
                return new CacheStatistics(this);
            }
        }
    }
    
    /**
     * Cache entry for auction data
     */
    private static class CachedEntry {
        final double price;
        final long timestamp;
        
        CachedEntry(double price, long timestamp) {
            this.price = price;
            this.timestamp = timestamp;
        }
        
        boolean isExpired(long ttlMs) {
            return System.currentTimeMillis() - timestamp > ttlMs;
        }
    }
    
    /**
     * Negative cache entry for tracking failed lookups
     */
    private static class NegativeCacheEntry {
        final long timestamp;
        
        NegativeCacheEntry(long timestamp) {
            this.timestamp = timestamp;
        }
        
        boolean isExpired(long ttlMs) {
            return System.currentTimeMillis() - timestamp > ttlMs;
        }
    }
}
