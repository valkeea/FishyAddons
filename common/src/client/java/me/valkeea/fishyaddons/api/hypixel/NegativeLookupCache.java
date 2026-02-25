package me.valkeea.fishyaddons.api.hypixel;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class NegativeLookupCache {
    
    private final ConcurrentHashMap<String, Entry> cache = new ConcurrentHashMap<>();
    private final AtomicLong hits = new AtomicLong(0);
    
    private static final int MAX_CACHE_SIZE = 10000;
    private static final long DEFAULT_TTL_MS = 60L * 60 * 1000;
    
    /**
     * Check if an item price is known to be unavailable.
     * 
     * @param lookupKey The item lookup key
     * @return true if negatively cached and not expired
     */
    public boolean contains(String lookupKey) {
        Entry entry = cache.get(lookupKey);
        if (entry != null && !entry.isExpired(DEFAULT_TTL_MS)) {
            hits.incrementAndGet();
            return true;
        }
        if (entry != null && entry.isExpired(DEFAULT_TTL_MS)) {
            cache.remove(lookupKey);
        }
        return false;
    }
    
    /**
     * Mark an item as negatively cached.
     * 
     * @param lookupKey The item lookup key
     */
    public void put(String lookupKey) {
        if (cache.size() < MAX_CACHE_SIZE) {
            cache.put(lookupKey, new Entry(System.currentTimeMillis()));
        }
    }
    
    public void clear() {
        cache.clear();
        hits.set(0);
    }
    
    public void cleanupExpired() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired(DEFAULT_TTL_MS));
    }
    
    public int size() {
        return cache.size();
    }
    
    public long getHits() {
        return hits.get();
    }
    
    @Override
    public String toString() {
        return String.format("NegativeLookupCache{size=%d, hits=%d}", size(), hits.get());
    }
    
    /**
     * Negative cache entry with timestamp for expiry checking.
     */
    private record Entry(long timestamp) {
        boolean isExpired(long ttlMs) {
            return System.currentTimeMillis() - timestamp > ttlMs;
        }
    }
}
