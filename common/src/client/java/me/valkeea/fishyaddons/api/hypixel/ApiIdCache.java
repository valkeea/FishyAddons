package me.valkeea.fishyaddons.api.hypixel;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class ApiIdCache {
    
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();
    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);
    
    private static final int MAX_CACHE_SIZE = 10000;
    
    /**
     * Get cached API ID for an item name.
     * 
     * @param itemName The display name or item name
     * @return The cached API ID, or null if not cached
     */
    public String get(String itemName) {
        String cached = cache.get(itemName);
        if (cached != null) {
            hits.incrementAndGet();
            return cached;
        }
        misses.incrementAndGet();
        return null;
    }
    
    /**
     * Cache an API ID conversion.
     * 
     * @param itemName The display name or item name
     * @param apiId The API ID
     */
    public void put(String itemName, String apiId) {
        if (cache.size() < MAX_CACHE_SIZE) {
            cache.put(itemName, apiId);
        }
    }
    
    public void clear() {
        cache.clear();
        hits.set(0);
        misses.set(0);
    }
    
    public int size() {
        return cache.size();
    }
    
    public long getHits() {
        return hits.get();
    }
    
    public long getMisses() {
        return misses.get();
    }
    
    /**
     * Get cache hit rate (0.0 to 1.0).
     */
    public double getHitRate() {
        long totalRequests = hits.get() + misses.get();
        return totalRequests > 0 ? (double) hits.get() / totalRequests : 0.0;
    }
    
    @Override
    public String toString() {
        return String.format("ApiIdCache{size=%d, hits=%d, misses=%d, hitRate=%.1f%%}", 
            size(), hits.get(), misses.get(), getHitRate() * 100);
    }
}
