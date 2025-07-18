package me.valkeea.fishyaddons.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Provides caching for API ID conversions, message parsing results, and price lookups
 * for profit tracker operations.
 */
public class ApiCache {
    private static final ConcurrentHashMap<String, String> apiIdCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, Object> messageParseCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> normalizationCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, CachedPrice> priceCache = new ConcurrentHashMap<>();
    
    // Cache hit/miss statistics
    private static final AtomicLong apiIdCacheHits = new AtomicLong(0);
    private static final AtomicLong apiIdCacheMisses = new AtomicLong(0);
    private static final AtomicLong messageCacheHits = new AtomicLong(0);
    private static final AtomicLong messageCacheMisses = new AtomicLong(0);
    private static final AtomicLong normalizationCacheHits = new AtomicLong(0);
    private static final AtomicLong normalizationCacheMisses = new AtomicLong(0);
    private static final AtomicLong priceCacheHits = new AtomicLong(0);
    private static final AtomicLong priceCacheMisses = new AtomicLong(0);
    
    // Config
    private static final int MAX_CACHE_SIZE = 10000;
    private static final long PRICE_CACHE_TTL_MS = 5L * 60 * 1000;
    
    private ApiCache() {} 
    
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
        resetStatistics();
    }
    
    // Specific for ah
    public static void clearPriceCache() {
        priceCache.clear();
        priceCacheHits.set(0);
        priceCacheMisses.set(0);
    }
    
    public static void cleanupExpiredEntries() {
        priceCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        if (messageParseCache.size() > MAX_CACHE_SIZE * 0.8) {
            messageParseCache.clear();
        }

        /** No cleanup needed for apiIdCache and normalizationCache
        * as they are usually small and don't expire.
        */
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
    }
    
    public static CacheStatistics getStatistics() {
        return CacheStatistics.builder()
            .withApiIdCache(apiIdCache.size(), apiIdCacheHits.get(), apiIdCacheMisses.get())
            .withMessageCache(messageParseCache.size(), messageCacheHits.get(), messageCacheMisses.get())
            .withNormalizationCache(normalizationCache.size(), normalizationCacheHits.get(), normalizationCacheMisses.get())
            .withPriceCache(priceCache.size(), priceCacheHits.get(), priceCacheMisses.get())
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
        
        /**
         * Builder class for constructing CacheStatistics instances.
         * Provides methods to set cache sizes and hit/miss counts.
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
            
            public CacheStatistics build() {
                return new CacheStatistics(this);
            }
        }
    }
}
